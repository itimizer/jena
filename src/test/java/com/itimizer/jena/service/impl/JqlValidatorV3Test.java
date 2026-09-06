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
import org.springframework.core.io.ClassPathResource;
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

@DisplayName("JqlValidatorV3 Tests")
class JqlValidatorV3Test {

    private static final String JQL = "project = TST";
    private static final String INVALID_JQL = "project = !!!";

    private MockWebServer mockWebServer;
    private JqlValidatorV3 jqlValidator;
    private String parseValidResponse;
    private String parseInvalidResponse;
    private String parseEmptyResponse;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        parseValidResponse = readFixture("json/jql/v3/parse-valid.json");
        parseInvalidResponse = readFixture("json/jql/v3/parse-invalid.json");
        parseEmptyResponse = readFixture("json/jql/v3/parse-empty.json");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                .responseTimeout(Duration.ofMillis(100));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        jqlValidator = new JqlValidatorV3(webClient);
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
    @DisplayName("should pass when Jira reports no errors")
    void should_pass_when_jira_reports_no_errors() {
        enqueueJson(parseValidResponse);

        assertThatCode(() -> jqlValidator.validate(JQL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should post the query to the strict parse endpoint")
    void should_post_query_to_strict_parse_endpoint() throws InterruptedException {
        enqueueJson(parseValidResponse);

        jqlValidator.validate(JQL);

        var request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).startsWith("/rest/api/3/jql/parse");
        assertThat(requireNonNull(request.getRequestUrl()).queryParameter("validation"))
                .isEqualTo("strict");
        assertThat(request.getBody().readUtf8())
                .isEqualTo("{\"queries\":[\"" + JQL + "\"]}");
    }

    @Test
    @DisplayName("should throw ValidationException carrying Jira's messages on a 200 with errors")
    void should_throw_validation_exception_with_jira_messages() {
        enqueueJson(parseInvalidResponse);

        assertThatThrownBy(() -> jqlValidator.validate(INVALID_JQL))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid JQL '" + INVALID_JQL + "'")
                .hasMessageContaining("'!' is a reserved JQL character.")
                .hasMessageContaining("The operator '=' is not supported by the 'project' field.");
    }

    @Test
    @DisplayName("should throw ValidationException when Jira answers 400")
    void should_throw_validation_exception_on_bad_request() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"errorMessages\":[\"Bad request.\"]}"));

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid JQL '" + JQL + "'");
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
    @DisplayName("should throw ValidationException when Jira returns no parse result")
    void should_throw_validation_exception_when_no_parse_result() {
        enqueueJson(parseEmptyResponse);

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Could not validate JQL");
    }

    @Test
    @DisplayName("should report a configuration error when the parse endpoint is absent")
    void should_throw_configuration_exception_when_endpoint_absent() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        assertThatThrownBy(() -> jqlValidator.validate(JQL))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("jena.jira.url");
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