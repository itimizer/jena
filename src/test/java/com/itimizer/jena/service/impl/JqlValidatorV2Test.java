package com.itimizer.jena.service.impl;

import com.itimizer.jena.exception.ConfigurationException;
import com.itimizer.jena.exception.ValidationException;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JqlValidatorV2 Tests")
class JqlValidatorV2Test {

    private static final String JQL = "project = TST";

    private MockWebServer mockWebServer;
    private JqlValidatorV2 jqlValidator;

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

        jqlValidator = new JqlValidatorV2(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("should pass when Jira accepts the query")
    void should_pass_when_jira_accepts_query() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"startAt\":0,\"maxResults\":0,\"total\":3,\"issues\":[]}"));

        assertThatCode(() -> jqlValidator.validate(JQL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should validate without executing the query")
    void should_validate_without_executing_query() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"startAt\":0,\"maxResults\":0,\"total\":3,\"issues\":[]}"));

        jqlValidator.validate(JQL);

        var request = mockWebServer.takeRequest();
        var url = requireNonNull(request.getRequestUrl());
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).startsWith("/rest/api/2/search");
        assertThat(url.queryParameter("jql")).isEqualTo(JQL);
        assertThat(url.queryParameter("maxResults")).isEqualTo("0");
        assertThat(url.queryParameter("validateQuery")).isEqualTo("true");
    }

    @Test
    @DisplayName("should throw ValidationException when Jira rejects the query")
    void should_throw_validation_exception_when_jira_rejects_query() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"errorMessages\":[\"Field 'nope' does not exist.\"]}"));

        assertThatThrownBy(() -> jqlValidator.validate("nope = 1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid JQL 'nope = 1'")
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("should fail loudly when the search endpoint is absent instead of passing")
    void should_fail_when_endpoint_absent() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("jena.jira.url");
    }

    @Test
    @DisplayName("should propagate a 410 rather than blame the query")
    void should_propagate_gone() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(410)
                .setBody("{\"errorMessages\":[\"The requested API has been removed.\"]}"));

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(HttpClientErrorException.class)
                .isNotInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should propagate an authorization failure rather than blame the query")
    void should_propagate_unauthorized() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(HttpClientErrorException.class)
                .isNotInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should propagate a server error rather than blame the query")
    void should_propagate_server_error() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("should throw exception when jql is null")
    @SuppressWarnings("ConstantConditions")
    void should_throw_exception_when_jql_is_null() {
        assertThatThrownBy(() -> jqlValidator.validate(null))
                .isInstanceOf(NullPointerException.class);
    }
}