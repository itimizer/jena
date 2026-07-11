package com.itimizer.jena.service.impl;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.mapper.JiraIssueDeserializer;
import com.itimizer.jena.util.JiraUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import okhttp3.HttpUrl;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIssueService Integration Tests")
class JiraIssueServiceImplTest {

    @Mock
    private DeserializationContext deserializationContext;

    @InjectMocks
    private JiraUtil jiraUtil;

    private MockWebServer mockWebServer;
    private JiraIssueServiceImpl jiraIssueService;
    private String issueJsonResponse;
    private JiraIssue issue;
    private final String issueKey = "TST-1";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                .responseTimeout(Duration.ofMillis(100));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        jiraIssueService = new JiraIssueServiceImpl(webClient, jiraUtil);
        issueJsonResponse = new String(new ClassPathResource("json/issue/valid-issue.json")
                .getInputStream().readAllBytes());

        JiraIssueDeserializer deserializer = new JiraIssueDeserializer();
        ObjectMapper objectMapper = new ObjectMapper();
        issue = deserializer.deserialize(
                objectMapper
                        .createParser(new ClassPathResource("json/issue/valid-issue.json")
                                .getFile()),
                deserializationContext);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("fetchJiraIssue() method tests")
    class FetchJiraIssueTests {

        @Test
        @DisplayName("should fetch jira issue successfully")
        void should_fetch_jira_issue_successfully() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issueJsonResponse));

            JiraIssue result = jiraIssueService.fetchJiraIssue(issueKey);
            assertThat(result).isNotNull();
            assertThat(result.getKey()).isEqualTo(issueKey);
            assertThat(result.getSummary()).isEqualTo("Test Issue Summary");
            assertThat(result.getDescription()).isEqualTo("Test Issue Description");
            assertThat(result.getStatus().getName()).isEqualTo("In Progress");
            assertThat(result.getIssueType().getName()).isEqualTo("Bug");

            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            Assertions.assertNotNull(request);
            HttpUrl url = HttpUrl.parse("http://localhost" + request.getPath());
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).contains("/rest/api/2/issue/" + issueKey);
            Assertions.assertNotNull(url);
            assertThat(url.queryParameter("expand")).isEqualTo("changelog,names,schema");
            assertThat(url.queryParameter("fields")).isEqualTo("-comment,-votes,-worklog");
        }

        @Test
        @DisplayName("should return null when issue not found")
        void should_return_null_when_issue_not_found() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("{\"errorMessages\":[\"Issue Does Not Exist\"]}"));

            JiraIssue result = jiraIssueService.fetchJiraIssue(issueKey);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when json is invalid")
        void should_throw_exception_when_json_is_invalid() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{invalid json}"));

            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(issueKey))
                    .isInstanceOf(DecodingException.class);
        }

        @Test
        @DisplayName("should return null when empty response body")
        void should_return_null_when_empty_response() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(""));

            JiraIssue result = jiraIssueService.fetchJiraIssue(issueKey);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when issueKey is null or empty")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_issueKey_is_null_or_empty() {
            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(""))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should throw exception when internal server error")
        void should_throw_exception_when_internal_server_error() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("{\"errorMessages\":[\"Internal Server Error\"]}"));

            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(issueKey))
                    .isInstanceOf(HttpServerErrorException.class);
        }

        @Test
        @DisplayName("should throw exception when client error other than not found")
        void should_throw_exception_when_client_error_other_than_not_found() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(401)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("{\"errorMessages\":[\"Unauthorized\"]}"));

            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(issueKey))
                    .isInstanceOf(HttpClientErrorException.class);
        }

        @Test
        @DisplayName("should throw exception when network timeout")
        void should_throw_exception_when_network_timeout() {
            mockWebServer.enqueue(new MockResponse()
                    .setBodyDelay(1, TimeUnit.SECONDS)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issueJsonResponse)
                    .setResponseCode(200));

            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(issueKey))
                    .isInstanceOf(Exception.class)
                    .hasRootCauseInstanceOf(ReadTimeoutException.class);
        }

        @Test
        @DisplayName("should throw exception when network connection error")
        void should_throw_exception_when_network_connection_error() {
            mockWebServer.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

            assertThatThrownBy(() -> jiraIssueService.fetchJiraIssue(issueKey))
                    .isInstanceOf(WebClientRequestException.class);
        }
    }

    @Nested
    @DisplayName("getJiraIssueChangelogGroups() method tests")
    class GetChangelogGroupsTests {

        @Test
        @DisplayName("should get all changelog groups successfully")
        void should_get_all_changelog_groups_successfully() {
            ZonedDateTime updated = issue.getUpdated().minusYears(1);

            List<JiraIssueChangelogGroup> result = jiraIssueService
                    .getJiraIssueChangelogGroups(issue, updated);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            JiraIssueChangelogGroup firstGroup = result.getFirst();
            assertThat(firstGroup.getCreated()).isNotNull();
            assertThat(firstGroup.getItems()).hasSize(4);
            assertThat(firstGroup.getAuthor()).isNotNull();
            assertThat(firstGroup.getAuthor().getDisplayName()).isEqualTo("Administrator");

            List<JiraIssueChangelogItem> items = firstGroup.getItems();
            assertThat(items).anyMatch(item -> item.getField().equals("Fix Version"));
            assertThat(items).anyMatch(item -> item.getField().equals("Version"));
            assertThat(items).anyMatch(item -> item.getField().equals("Component"));

            JiraIssueChangelogGroup secondGroup = result.get(1);
            assertThat(secondGroup.getItems()).hasSize(1);
            assertThat(secondGroup.getItems().getFirst().getField()).isEqualTo("My Number Field");
            assertThat(secondGroup.getItems().getFirst().getToString()).isEqualTo("1.457");
        }

        @Test
        @DisplayName("should return changelog groups created after specified date")
        void should_return_changelog_groups_after_specified_date() {
            ZonedDateTime updated = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));

            List<JiraIssueChangelogGroup> result = jiraIssueService
                    .getJiraIssueChangelogGroups(issue, updated);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            JiraIssueChangelogGroup firstGroup = result.getFirst();
            assertThat(firstGroup.getCreated()).isNotNull();
            assertThat(firstGroup.getItems()).hasSize(1);
            assertThat(firstGroup.getAuthor()).isNotNull();
            assertThat(firstGroup.getAuthor().getDisplayName()).isEqualTo("Administrator");

            List<JiraIssueChangelogItem> items = firstGroup.getItems();
            assertThat(items).anyMatch(item -> item.getField().equals("My Number Field"));
        }

        @Test
        @DisplayName("should return empty list when issue has no changelog")
        void should_return_empty_list_when_no_changelog() {
            ZonedDateTime updated = issue.getUpdated().plusYears(1);

            List<JiraIssueChangelogGroup> result = jiraIssueService
                    .getJiraIssueChangelogGroups(issue, updated);
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle null issue")
        void should_handle_null_issue() {
            ZonedDateTime updated = ZonedDateTime.now();

            List<JiraIssueChangelogGroup> result = jiraIssueService
                    .getJiraIssueChangelogGroups(null, updated);
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle null updated date")
        void should_handle_null_updated_date() {
            List<JiraIssueChangelogGroup> result = jiraIssueService
                    .getJiraIssueChangelogGroups(issue, null);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should maintain order of changelog groups")
        void should_maintain_order_of_changelog_groups() {
            ZonedDateTime updated = issue.getUpdated().minusYears(1);

            List<JiraIssueChangelogGroup> result = jiraIssueService
                    .getJiraIssueChangelogGroups(issue, updated);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            ZonedDateTime firstDate = result.get(0).getCreated();
            ZonedDateTime secondDate = result.get(1).getCreated();
            assertThat(firstDate).isBeforeOrEqualTo(secondDate);
        }
    }
}