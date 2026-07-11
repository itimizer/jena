package com.itimizer.jena.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebClientLoggingFilter masking tests")
class WebClientLoggingFilterTest {

    private static final String JIRA_PAT = "super-secret-pat";
    private static final String TELEGRAM_TOKEN = "123456789:AAEhBOweik6ad-secret_TOKEN";

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(WebClientLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    @DisplayName("should mask Telegram bot token embedded in the request URL")
    void should_mask_telegram_token_in_url() {
        var url = "https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/sendMessage";

        runFilter(ClientRequest.create(HttpMethod.POST, URI.create(url)).build());

        assertThat(debugMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("https://api.telegram.org/bot****/sendMessage")
                        .doesNotContain(TELEGRAM_TOKEN));
    }

    @Test
    @DisplayName("should keep the Bearer scheme but hide the credential")
    void should_mask_bearer_authorization() {
        runFilter(request(headers -> headers.setBearerAuth(JIRA_PAT)));

        assertThat(authorizationHeaderMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Bearer ****")
                        .doesNotContain(JIRA_PAT));
    }

    @Test
    @DisplayName("should keep the Basic scheme but hide the credential")
    void should_mask_basic_authorization() {
        runFilter(request(headers -> headers.setBasicAuth("admin", "password")));

        var credential = Base64.getEncoder()
                .encodeToString("admin:password".getBytes());
        assertThat(authorizationHeaderMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Basic ****")
                        .doesNotContain(credential));
    }

    @Test
    @DisplayName("should not mask non-Authorization headers")
    void should_not_mask_other_headers() {
        runFilter(request(headers -> headers.add("X-Custom-Header", "plain-value")));

        assertThat(traceMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("X-Custom-Header")
                        .contains("plain-value"));
    }

    @Test
    @DisplayName("should not log headers when level is above TRACE")
    void should_not_log_headers_at_debug() {
        logger.setLevel(Level.DEBUG);

        runFilter(request(headers -> headers.setBearerAuth(JIRA_PAT)));

        assertThat(debugMessages()).isNotEmpty();
        assertThat(traceEvents()).isEmpty();
    }

    private ClientRequest request(Consumer<HttpHeaders> headersConsumer) {
        return ClientRequest.create(HttpMethod.GET, URI.create("https://jira.example.com/myself"))
                .headers(headersConsumer)
                .build();
    }

    private void runFilter(ClientRequest request) {
        ExchangeFilterFunction filter = WebClientLoggingFilter.create();
        filter.filter(request, req -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
                .block();
    }

    private List<ILoggingEvent> traceEvents() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.TRACE)
                .toList();
    }

    private List<String> traceMessages() {
        return traceEvents().stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private List<String> debugMessages() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.DEBUG)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private List<String> authorizationHeaderMessages() {
        return traceMessages().stream()
                .filter(message -> message.contains(HttpHeaders.AUTHORIZATION))
                .toList();
    }
}