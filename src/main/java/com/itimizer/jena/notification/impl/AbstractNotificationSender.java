package com.itimizer.jena.notification.impl;

import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.notification.NotificationSender;
import com.itimizer.jena.util.StringUtil;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Base {@link NotificationSender} that owns the WebClient send pipeline — logging, 4xx/5xx and
 * timeout handling, and normalising the result into a {@link ResponseEntity} — leaving subclasses
 * to declare only their channel name and how to build the request. A {@code null} WebClient means
 * the channel is unconfigured and {@link #send} returns {@code null} (skipped).
 */
@Slf4j
public abstract class AbstractNotificationSender implements NotificationSender {

    private final WebClient webClient;

    /**
     * Creates a sender over the channel's configured {@code WebClient}, which may be {@code null}
     * when the channel is not configured.
     *
     * @param webClient the channel's {@code WebClient}, or {@code null} if unconfigured
     */
    protected AbstractNotificationSender(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Names the channel for log messages.
     *
     * @return the human-readable channel name
     */
    protected abstract String channel();

    /**
     * Provides the warning logged when the channel has no {@code WebClient} configured.
     *
     * @return the message logged when the channel is not configured
     */
    protected abstract String unconfiguredMessage();

    /**
     * Builds the channel-specific HTTP request for the message.
     *
     * @param webClient the channel's configured {@code WebClient}
     * @param message the rendered message to send
     * @return the request ready to be retrieved
     */
    protected abstract WebClient.RequestHeadersSpec<?> buildRequest(WebClient webClient,
                                                                    NotificationMessageDto message);

    /**
     * Whether a {@code 401} should be retried once with a freshly built request, for channels
     * whose credentials are minted per request and can expire in flight.
     *
     * @return {@code true} to retry once on {@code 401}
     */
    protected boolean retryOnUnauthorized() {
        return false;
    }

    /**
     * Formats the error message recorded when the channel API returns a non-2xx response.
     *
     * @param body the (possibly truncated) response body
     * @return the formatted error message
     */
    protected String apiErrorMessage(String body) {
        return String.format("Error calling %s API, Cause: %s", channel(),
                StringUtil.truncate(body, StringUtil.MAX_ERROR_BODY_LENGTH));
    }

    @Override
    public ResponseEntity<String> send(@NonNull NotificationMessageDto message) {
        if (webClient == null) {
            log.warn(unconfiguredMessage());
            return null;
        }

        var issueKey = message.issueKey();
        log.debug("Starting {} notification for issue: {}", channel(), issueKey);
        log.trace("{} notification payload for {}: {}", channel(), issueKey, message.content());

        var exchange = Mono.defer(() -> buildRequest(webClient, message)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.warn("Client error occurred during sending {} notification for {}: "
                                    + "Status {}", channel(), issueKey, response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.debug("Upstream {} error body for {}: {}",
                                        channel(), issueKey, body);
                                return Mono.error(new HttpClientErrorException(
                                        response.statusCode(), apiErrorMessage(body)));
                            });
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.warn("Server error occurred during sending {} notification for {}: "
                                    + "Status {}", channel(), issueKey, response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.debug("Upstream {} error body for {}: {}",
                                        channel(), issueKey, body);
                                return Mono.error(new HttpServerErrorException(
                                        response.statusCode(), apiErrorMessage(body)));
                            });
                })
                .toEntity(String.class));

        var res = (retryOnUnauthorized() ? exchange.retryWhen(unauthorizedRetry(issueKey))
                : exchange)
                .map(responseEntity -> {
                    if (responseEntity.getStatusCode().is2xxSuccessful()) {
                        log.debug("Successful {} notification for {}: Status {}",
                                channel(), issueKey, responseEntity.getStatusCode());
                        return ResponseEntity.status(responseEntity.getStatusCode()).body("");
                    }
                    return responseEntity;
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error occurred while sending {} notification for {}: {}",
                            channel(), issueKey, e.getMessage(), e);
                    if (e instanceof HttpServerErrorException serverError) {
                        return Mono.just(ResponseEntity
                                .status(serverError.getStatusCode())
                                .body(e.getMessage()));
                    } else if (e instanceof HttpClientErrorException clientError) {
                        return Mono.just(ResponseEntity
                                .status(clientError.getStatusCode())
                                .body(e.getMessage()));
                    } else if (e.getCause() instanceof ReadTimeoutException
                            || e.getCause() instanceof ConnectTimeoutException) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.GATEWAY_TIMEOUT)
                                .body("Request timeout while sending "
                                        + channel() + " notification"));
                    }
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to send " + channel() + " notification"));
                })
                .block();

        log.debug("Completed {} notification for issue: {}", channel(), issueKey);
        log.trace("{} notification response for {}: {}", channel(), issueKey,
                res != null ? res.getBody() : null);
        return res;
    }

    private Retry unauthorizedRetry(String issueKey) {
        return Retry.max(1)
                .filter(e -> e instanceof HttpClientErrorException clientError
                        && clientError.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED))
                .doBeforeRetry(signal -> log.warn(
                        "Retrying {} notification for {} with fresh credentials after 401",
                        channel(), issueKey))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }
}
