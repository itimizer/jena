package com.itimizer.jena.service.impl;

import com.itimizer.jena.domain.JiraIssueKey;
import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.exception.RequestFailedException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JiraSearchClientV3 Tests")
class JiraSearchClientV3Test {

    private static final String JQL = "project in ('TST') and updated >= '2026-01-01'";
    private static final String FILTER_NAME = "Test Filter";

    private MockWebServer mockWebServer;
    private JiraSearchClientV3 jiraSearchClient;
    private String issuesSearchResponse;
    private String issuesSearchResponseEmpty;
    private String issuesSearchResponsePage1;
    private String issuesSearchResponsePage2;
    private String issuesSearchResponsePage3;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        issuesSearchResponse = readFixture("json/search/v3/issues.json");
        issuesSearchResponseEmpty = readFixture("json/search/v3/issues-empty.json");
        issuesSearchResponsePage1 = readFixture("json/search/v3/issues-page1.json");
        issuesSearchResponsePage2 = readFixture("json/search/v3/issues-page2.json");
        issuesSearchResponsePage3 = readFixture("json/search/v3/issues-page3.json");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                .responseTimeout(Duration.ofMillis(100));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        jiraSearchClient = new JiraSearchClientV3(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private String readFixture(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes());
    }

    private void enqueueJson(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));
    }

    @Test
    @DisplayName("should handle single page result carrying no token")
    void should_handle_single_page() {
        enqueueJson(issuesSearchResponse);

        JiraSearchResult result = jiraSearchClient.searchAllKeys(JQL, FILTER_NAME);
        assertThat(result).isNotNull();
        assertThat(result.issues()).extracting(JiraIssueKey::getKey).containsExactly(
                "TST-1", "TST-2");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should accumulate every page until one arrives without a token")
    void should_accumulate_all_pages() {
        enqueueJson(issuesSearchResponsePage1);
        enqueueJson(issuesSearchResponsePage2);
        enqueueJson(issuesSearchResponsePage3);

        JiraSearchResult result = jiraSearchClient.searchAllKeys(JQL, FILTER_NAME);
        assertThat(result).isNotNull();
        assertThat(result.issues()).extracting(JiraIssueKey::getKey).containsExactly(
                "TST-1", "TST-2", "TST-3");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("should query the v3 search endpoint with key fields and no token on first call")
    void should_query_v3_search_endpoint() throws InterruptedException {
        enqueueJson(issuesSearchResponse);

        jiraSearchClient.searchAllKeys(JQL, FILTER_NAME);

        var request = mockWebServer.takeRequest();
        var url = requireNonNull(request.getRequestUrl());
        assertThat(request.getPath()).startsWith("/rest/api/3/search/jql");
        assertThat(url.queryParameter("fields")).isEqualTo("key");
        assertThat(url.queryParameter("maxResults")).isEqualTo("5000");
        assertThat(url.queryParameter("jql")).isEqualTo(JQL);
        assertThat(url.queryParameter("nextPageToken")).isNull();
    }

    @Test
    @DisplayName("should send the token returned by the previous page")
    void should_send_token_from_previous_page() throws InterruptedException {
        enqueueJson(issuesSearchResponsePage1);
        enqueueJson(issuesSearchResponsePage2);
        enqueueJson(issuesSearchResponsePage3);

        jiraSearchClient.searchAllKeys(JQL, FILTER_NAME);

        mockWebServer.takeRequest();
        var secondUrl = requireNonNull(mockWebServer.takeRequest().getRequestUrl());
        var thirdUrl = requireNonNull(mockWebServer.takeRequest().getRequestUrl());
        assertThat(secondUrl.queryParameter("nextPageToken")).isEqualTo("token-1");
        assertThat(thirdUrl.queryParameter("nextPageToken")).isEqualTo("token-2");
    }

    @Test
    @DisplayName("should handle empty result")
    void should_handle_empty_result() {
        enqueueJson(issuesSearchResponseEmpty);

        JiraSearchResult result = jiraSearchClient.searchAllKeys(JQL, FILTER_NAME);
        assertThat(result).isNotNull();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("should fail when a page returns nothing while a token is outstanding")
    void should_fail_when_page_returns_nothing_while_token_outstanding() {
        enqueueJson(issuesSearchResponsePage1);
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(RequestFailedException.class)
                .hasMessageContaining(FILTER_NAME)
                .hasMessageContaining("outstanding");
    }

    @Test
    @DisplayName("should fail when a page echoes the token it was requested with")
    void should_fail_when_page_echoes_requested_token() {
        enqueueJson(issuesSearchResponsePage1);
        enqueueJson(issuesSearchResponsePage1);

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(RequestFailedException.class)
                .hasMessageContaining(FILTER_NAME)
                .hasMessageContaining("same next page token");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should return null when Jira answers the first page with 404")
    void should_return_null_when_not_found() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        assertThat(jiraSearchClient.searchAllKeys(JQL, FILTER_NAME)).isNull();
    }

    @Test
    @DisplayName("should throw exception when first page fails")
    void should_throw_exception_when_first_page_fails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("should propagate a failure on a later page")
    void should_propagate_failure_on_later_page() {
        enqueueJson(issuesSearchResponsePage1);
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("should throw exception when Bad Request error")
    void should_throw_exception_when_bad_request_error() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"errorMessages\":[\"The value 'TST' does not exist.\"]}"));

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("should throw exception when network timeout")
    void should_throw_exception_when_network_timeout() {
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(1, TimeUnit.SECONDS)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(issuesSearchResponse)
                .setResponseCode(200));

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(Exception.class)
                .hasRootCauseInstanceOf(ReadTimeoutException.class);
    }

    @Test
    @DisplayName("should throw exception when network connection error")
    void should_throw_exception_when_network_connection_error() {
        mockWebServer.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(JQL, FILTER_NAME))
                .isInstanceOf(WebClientRequestException.class);
    }

    @Test
    @DisplayName("should throw exception when jql is null")
    @SuppressWarnings("ConstantConditions")
    void should_throw_exception_when_jql_is_null() {
        assertThatThrownBy(() -> jiraSearchClient.searchAllKeys(null, FILTER_NAME))
                .isInstanceOf(NullPointerException.class);
    }
}