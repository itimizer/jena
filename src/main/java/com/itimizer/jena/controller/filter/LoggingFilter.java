package com.itimizer.jena.controller.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.list;

/**
 * Servlet filter that assigns a request trace id (MDC) and logs each API request/response with
 * timing. Ordered just after Spring Security so the authenticated principal is available when
 * logging.
 */
@Slf4j
@Component
@Order(LoggingFilter.SECURITY_FILTER_ORDER + 1)
public class LoggingFilter implements Filter {

    static final int SECURITY_FILTER_ORDER = -100;

    private static final int CONTENT_CACHE_LIMIT = 64 * 1024;

    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"[^\"]*(?i:password|secret|token)[^\"]*\"\\s*:\\s*)\"(?:\\\\.|[^\"\\\\])*\"");

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        var requestWrapper = requestWrapper(servletRequest);
        var responseWrapper = responseWrapper(servletResponse);

        var tid = UUID.randomUUID().toString();
        var http = (HttpServletRequest) servletRequest;
        var principal = http.getUserPrincipal();
        var user = principal != null ? principal.getName() : "anonymous";

        MDC.put("tid", tid);
        MDC.put("user", user);
        MDC.put("method", http.getMethod());
        MDC.put("path", http.getRequestURI());
        log.info("Request START");

        var start = Instant.now();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            var finish = Instant.now();
            var time = Duration.between(start, finish).toMillis();

            log.trace("Execution Time: {} ms", time);
            logRequest(requestWrapper);
            logResponse(responseWrapper);
            log.info("Request COMPLETE");
            MDC.clear();
        }

    }

    private void logRequest(ContentCachingRequestWrapper request) {
        var builder = new StringBuilder();
        builder.append(headersToString(list(request.getHeaderNames()), request::getHeader));
        if (isTextLoggable(request.getContentType())) {
            builder.append(maskSensitiveFields(
                    new String(request.getContentAsByteArray(), StandardCharsets.UTF_8)));
        }
        log.trace("Request: {}", builder);
    }

    private void logResponse(ContentCachingResponseWrapper response) throws IOException {
        var builder = new StringBuilder();
        builder.append(headersToString(response.getHeaderNames(), response::getHeader));
        if (isTextLoggable(response.getContentType())) {
            builder.append(maskSensitiveFields(
                    new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)));
        }
        log.trace("Response: {}", builder);
        response.copyBodyToResponse();
    }

    /** Masks values of JSON fields whose name contains password/secret/token. */
    static String maskSensitiveFields(String body) {
        return SENSITIVE_JSON_FIELD.matcher(body).replaceAll("$1\"****\"");
    }

    private boolean isTextLoggable(String contentType) {
        if (contentType == null) {
            return false;
        }
        var type = contentType.toLowerCase(Locale.ROOT);
        return type.contains("json")
                || type.contains("xml")
                || type.startsWith("text")
                || type.contains("x-www-form-urlencoded");
    }

    private String headersToString(Collection<String> headerNames,
                                   Function<String, String> headerValueResolver) {
        var builder = new StringBuilder();

        for (var headerName : headerNames) {
            String header;

            if (headerName.equalsIgnoreCase("AUTHORIZATION")) {
                header = "****";
            } else {
                header = headerValueResolver.apply(headerName);
            }

            builder.append("%s=%s".formatted(headerName, header)).append(", ");
        }

        return builder.toString();
    }

    private ContentCachingRequestWrapper requestWrapper(ServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper requestWrapper) {
            return requestWrapper;
        }

        return new ContentCachingRequestWrapper((HttpServletRequest) request, CONTENT_CACHE_LIMIT);
    }

    private ContentCachingResponseWrapper responseWrapper(ServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper responseWrapper) {
            return responseWrapper;
        }

        return new ContentCachingResponseWrapper((HttpServletResponse) response);
    }

}
