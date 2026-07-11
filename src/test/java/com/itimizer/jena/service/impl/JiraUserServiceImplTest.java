package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraUser;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraUserService Integration Tests")
class JiraUserServiceImplTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Jira jiraProperties;

    private MockWebServer mockWebServer;
    private WebClient webClient;
    private JiraUserServiceImpl jiraUserService;
    private String userJsonResponse;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                .responseTimeout(Duration.ofMillis(100));

        webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        jiraUserService = new JiraUserServiceImpl(applicationProperties, webClient);
        userJsonResponse = new String(new ClassPathResource("json/user/valid-user.json")
                .getInputStream().readAllBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("fetchJiraUser() method tests")
    class FetchJiraUserTests {

        @Test
        @DisplayName("should fetch Jira user successfully")
        void should_fetch_jira_user_successfully() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(userJsonResponse));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("admin");
            assertThat(result.getKey()).isEqualTo("admin");
            assertThat(result.getEmailAddress()).isEqualTo("admin@example.com");
            assertThat(result.getDisplayName()).isEqualTo("Administrator");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getTimeZone()).isEqualTo("Antarctica/Troll");

            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            Assertions.assertNotNull(request);
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/rest/api/2/myself");
        }

        @Test
        @DisplayName("should return null when user is unauthorized")
        void should_return_null_when_user_is_unauthorized() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("{\"message\":[\"Client must be authenticated to access.\"]}"));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when internal server error")
        void should_return_null_when_internal_server_error() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("{\"errorMessages\":[\"Internal Server Error\"]}"));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle network timeout")
        void should_handle_network_timeout() {
            mockWebServer.enqueue(new MockResponse()
                    .setBodyDelay(1, TimeUnit.SECONDS)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(userJsonResponse)
                    .setResponseCode(200));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle network connection error")
        void should_handle_network_connection_error() {
            mockWebServer.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when json is invalid")
        void should_return_null_when_json_is_invalid() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{invalid json}"));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when empty response body")
        void should_return_null_when_empty_response() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(""));

            JiraUser result = jiraUserService.fetchJiraUser();
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("initialize() method tests")
    class InitializeTests {

        @BeforeEach
        void setUp() {
            when(applicationProperties.getJira()).thenReturn(jiraProperties);
        }

        @Test
        @DisplayName("should initialize user when PAT is configured")
        void should_initialize_user_when_pat_configured() {
            when(jiraProperties.getPat()).thenReturn("4699w08et90edsfkjfskdgasf44aasf");
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(userJsonResponse));

            jiraUserService = new JiraUserServiceImpl(applicationProperties, webClient);
            jiraUserService.initialize();
            assertThat(jiraUserService.getJiraUser()).isNotNull();
            assertThat(jiraUserService.getJiraUser().getName()).isEqualTo("admin");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should initialize user when username and password are configured")
        void should_initialize_user_when_username_password_configured() {
            when(jiraProperties.getPat()).thenReturn(null);
            when(jiraProperties.getUsername()).thenReturn("admin");
            when(jiraProperties.getPassword()).thenReturn("password");
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(userJsonResponse));

            jiraUserService = new JiraUserServiceImpl(applicationProperties, webClient);
            jiraUserService.initialize();
            assertThat(jiraUserService.getJiraUser()).isNotNull();
            assertThat(jiraUserService.getJiraUser().getName()).isEqualTo("admin");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should skip initialization when no credentials configured")
        void should_skip_initialization_when_no_credentials() {
            jiraUserService = new JiraUserServiceImpl(applicationProperties, webClient);

            jiraUserService.initialize();
            assertThat(jiraUserService.getJiraUser()).isNull();
        }

        @Test
        @DisplayName("should resolve user lazily when Jira was unavailable at startup")
        void should_resolve_user_lazily_after_failed_initialization() {
            when(jiraProperties.getPat()).thenReturn("4699w08et90edsfkjfskdgasf44aasf");
            mockWebServer.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(userJsonResponse));

            jiraUserService = new JiraUserServiceImpl(applicationProperties, webClient);
            jiraUserService.initialize();

            JiraUser resolved = jiraUserService.getJiraUser();
            assertThat(resolved).isNotNull();
            assertThat(resolved.getName()).isEqualTo("admin");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);

            assertThat(jiraUserService.getJiraUser()).isSameAs(resolved);
            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        }
    }
}