package com.itimizer.jena.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebClientConfig Authorization header tests")
class WebClientConfigAuthorizationTest {

    private MockWebServer mockWebServer;
    private ClientHttpConnector clientHttpConnector;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        clientHttpConnector = new ReactorClientHttpConnector(HttpClient.create());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("should send Bearer auth when PAT is configured")
    void should_send_bearer_auth_when_pat_configured() throws InterruptedException {
        var jira = new ApplicationProperties.Jira();
        jira.setUrl(mockWebServer.url("/").toString());
        jira.setPat("my-secret-token");

        var request = exchange(jira);

        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer my-secret-token");
    }

    @Test
    @DisplayName("should send Basic auth when username and password are configured")
    void should_send_basic_auth_when_username_password_configured() throws InterruptedException {
        var jira = new ApplicationProperties.Jira();
        jira.setUrl(mockWebServer.url("/").toString());
        jira.setUsername("admin");
        jira.setPassword("password");

        var request = exchange(jira);

        var expected = "Basic " + Base64.getEncoder()
                .encodeToString("admin:password".getBytes(StandardCharsets.UTF_8));
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo(expected);
    }

    @Test
    @DisplayName("should send PAT Bearer auth when both PAT and credentials are configured")
    void should_prefer_pat_over_credentials() throws InterruptedException {
        var jira = new ApplicationProperties.Jira();
        jira.setUrl(mockWebServer.url("/").toString());
        jira.setPat("my-secret-token");
        jira.setUsername("admin");
        jira.setPassword("password");

        var request = exchange(jira);

        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer my-secret-token");
    }

    @Test
    @DisplayName("should not send Authorization header when no credentials are configured")
    void should_not_send_auth_when_no_credentials() throws InterruptedException {
        var jira = new ApplicationProperties.Jira();
        jira.setUrl(mockWebServer.url("/").toString());

        var request = exchange(jira);

        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    private RecordedRequest exchange(ApplicationProperties.Jira jira) throws InterruptedException {
        var properties = new ApplicationProperties();
        properties.setJira(jira);

        WebClient webClient = new WebClientConfig(properties, null)
                .jiraWebClient(clientHttpConnector);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        webClient.get()
                .uri("/rest/api/2/myself")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    }
}