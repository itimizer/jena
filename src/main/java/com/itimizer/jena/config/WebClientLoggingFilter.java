package com.itimizer.jena.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link ExchangeFilterFunction} factory that logs outbound WebClient requests with secrets masked
 * — Authorization headers and the Telegram bot token embedded in the URL path.
 */
@Slf4j
public final class WebClientLoggingFilter {

    private static final String MASK = "****";

    private static final Pattern TELEGRAM_TOKEN = Pattern.compile("/bot[^/]+");

    private WebClientLoggingFilter() {
    }

    public static ExchangeFilterFunction create() {
        return ExchangeFilterFunction.ofRequestProcessor(WebClientLoggingFilter::logRequest);
    }

    private static Mono<ClientRequest> logRequest(ClientRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("WebClient request: {} {}", request.method(),
                    maskUrl(request.url().toString()));
        }
        if (log.isTraceEnabled()) {
            request.headers().forEach((name, values) ->
                    log.trace("WebClient request header: {}: {}", name, maskHeader(name, values)));
        }
        return Mono.just(request);
    }

    private static String maskUrl(String url) {
        return TELEGRAM_TOKEN.matcher(url).replaceAll("/bot" + MASK);
    }

    private static List<String> maskHeader(String name, List<String> values) {
        if (!HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
            return values;
        }
        return values.stream()
                .map(WebClientLoggingFilter::maskAuthorization)
                .toList();
    }

    private static String maskAuthorization(String value) {
        if (value == null || value.isBlank()) {
            return MASK;
        }
        int space = value.indexOf(' ');
        return space > 0 ? value.substring(0, space + 1) + MASK : MASK;
    }
}
