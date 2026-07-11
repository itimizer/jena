package com.itimizer.jena.controller.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoggingFilter Tests")
class LoggingFilterTest {

    @Test
    @DisplayName("should mask password field values")
    void should_mask_password_values() {
        var body = "{\"username\":\"alice\",\"password\":\"s3cr3t\",\"roles\":[\"USER\"]}";

        assertThat(LoggingFilter.maskSensitiveFields(body))
                .isEqualTo("{\"username\":\"alice\",\"password\":\"****\",\"roles\":[\"USER\"]}");
    }

    @Test
    @DisplayName("should mask fields whose name contains secret or token")
    void should_mask_secret_and_token_fields() {
        var body = "{\"secretKey\":\"abc\",\"apiToken\":\"xyz\",\"name\":\"jena\"}";

        assertThat(LoggingFilter.maskSensitiveFields(body))
                .isEqualTo("{\"secretKey\":\"****\",\"apiToken\":\"****\",\"name\":\"jena\"}");
    }

    @Test
    @DisplayName("should mask values containing escaped quotes")
    void should_mask_values_with_escaped_quotes() {
        var body = "{\"password\":\"a\\\"b\"}";

        assertThat(LoggingFilter.maskSensitiveFields(body))
                .isEqualTo("{\"password\":\"****\"}");
    }

    @Test
    @DisplayName("should mask field names case-insensitively and with spacing")
    void should_mask_case_insensitively() {
        var body = "{\"Password\" : \"p\"}";

        assertThat(LoggingFilter.maskSensitiveFields(body))
                .isEqualTo("{\"Password\" : \"****\"}");
    }

    @Test
    @DisplayName("should leave non-sensitive fields untouched")
    void should_leave_other_fields_untouched() {
        var body = "{\"jql\":\"project = TST\",\"name\":\"filter\"}";

        assertThat(LoggingFilter.maskSensitiveFields(body)).isEqualTo(body);
    }
}