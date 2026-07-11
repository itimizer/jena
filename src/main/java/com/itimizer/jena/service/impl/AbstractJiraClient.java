package com.itimizer.jena.service.impl;

import com.itimizer.jena.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Base class for the blocking Jira REST clients. Wraps the shared {@code jiraWebClient} and gives
 * subclasses a single {@link #fetch} helper with uniform error handling and logging, so each client
 * only declares its request.
 */
@Slf4j
public abstract class AbstractJiraClient {

    private final WebClient webClient;

    /**
     * Creates a client over the shared Jira {@code WebClient}.
     *
     * @param webClient the preconfigured Jira {@code WebClient} shared by all subclasses
     */
    protected AbstractJiraClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Exposes the shared {@code WebClient} to subclasses for building requests.
     *
     * @return the shared Jira {@code WebClient}
     */
    protected WebClient jiraWebClient() {
        return webClient;
    }

    /**
     * Executes a request and blocks for the deserialized body. {@code 4xx}/{@code 5xx} are logged
     * and surfaced as {@code HttpClientErrorException}/{@code HttpServerErrorException}; a
     * {@code 404} always yields {@code null}. Other errors are re-thrown when
     * {@code propagateErrors} is set, otherwise swallowed to {@code null}.
     *
     * @param <T> the deserialized response body type
     * @param request the request to execute
     * @param type the class to deserialize the response body into
     * @param description short phrase naming the call, used in log messages
     * @param propagateErrors whether non-404 failures should propagate or be downgraded to
     *                        {@code null}
     * @return the deserialized response body, or {@code null} on a 404 or a swallowed error
     */
    protected <T> T fetch(WebClient.RequestHeadersSpec<?> request,
                          Class<T> type,
                          String description,
                          boolean propagateErrors) {
        return request
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> toError(response, description, true))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> toError(response, description, false))
                .bodyToMono(type)
                .onErrorResume(e -> handleError(e, description, propagateErrors))
                .block();
    }

    private Mono<? extends Throwable> toError(ClientResponse response, String description,
                                              boolean clientError) {
        log.warn("{} error occurred during {}: Status {}",
                clientError ? "Client" : "Server", description, response.statusCode());
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    log.debug("Upstream error body during {}: {}", description, body);
                    var message = String.format("Error calling url: %s, Cause: %s",
                            response.request().getURI(),
                            StringUtil.truncate(body, StringUtil.MAX_ERROR_BODY_LENGTH));
                    return Mono.error(clientError
                            ? new HttpClientErrorException(response.statusCode(), message)
                            : new HttpServerErrorException(response.statusCode(), message));
                });
    }

    private <T> Mono<T> handleError(Throwable e, String description, boolean propagateErrors) {
        if (e instanceof HttpClientErrorException clientError
                && clientError.getStatusCode() == HttpStatus.NOT_FOUND) {
            log.warn("{} not found: {}", description, e.getMessage());
            return Mono.empty();
        }
        log.error("Unexpected error occurred during {}: {}", description, e.getMessage(), e);
        if (propagateErrors) {
            return Mono.error(e);
        }
        return Mono.empty();
    }
}
