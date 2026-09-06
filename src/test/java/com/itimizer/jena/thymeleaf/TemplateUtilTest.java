package com.itimizer.jena.thymeleaf;

import com.itimizer.jena.util.DateTimeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateUtil Tests")
class TemplateUtilTest {

    @Mock
    private DateTimeUtil dateTimeUtil;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TemplateUtil templateUtil;

    @Nested
    @DisplayName("parseLocalDateTime() method tests")
    class ParseLocalDateTimeTests {

        @Test
        @DisplayName("should parse LocalDateTime successfully")
        void should_parse_local_datetime_successfully() {
            LocalDateTime expected = LocalDateTime.of(2026, 1, 1, 0, 0);
            when(dateTimeUtil.parseLocalDateTime("2026-1-1 0:0", "yyyy-MM-dd HH:mm"))
                    .thenReturn(expected);

            LocalDateTime result =
                    templateUtil.parseLocalDateTime("2026-1-1 0:0", "yyyy-MM-dd HH:mm");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw exception when parsing invalid LocalDateTime")
        void should_throw_exception_when_parsing_invalid_local_datetime() {
            when(dateTimeUtil.parseLocalDateTime("invalid", "yyyy-MM-dd HH:mm"))
                    .thenThrow(new IllegalArgumentException("Invalid format"));

            assertThatThrownBy(() -> templateUtil.parseLocalDateTime("invalid", "yyyy-MM-dd HH:mm"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("formatLocalDateTime() method tests")
    class FormatLocalDateTimeTests {

        @Test
        @DisplayName("should format LocalDateTime successfully")
        void should_format_local_datetime_successfully() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0);
            when(dateTimeUtil.format(localDateTime, "yyyy-MM-dd HH:mm"))
                    .thenReturn("2026-1-1 0:0");

            String result = templateUtil.formatLocalDateTime(localDateTime, "yyyy-MM-dd HH:mm");
            assertThat(result).isEqualTo("2026-1-1 0:0");
        }

        @Test
        @DisplayName("should throw exception when formatting LocalDateTime fails")
        void should_throw_exception_when_formatting_local_datetime_fails() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0);
            when(dateTimeUtil.format(localDateTime, "invalid-format"))
                    .thenThrow(new IllegalArgumentException("Invalid format"));

            assertThatThrownBy(() ->
                        templateUtil.formatLocalDateTime(localDateTime, "invalid-format"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("parseZonedDateTime() method tests")
    class ParseZonedDateTimeTests {

        @Test
        @DisplayName("should parse ZonedDateTime successfully")
        void should_parse_zoned_datetime_successfully() {
            ZonedDateTime expected = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            when(dateTimeUtil.parseZonedDateTime("2026-1-1T0:0:0+00", "yyyy-MM-dd'T'HH:mm:ssZ"))
                    .thenReturn(expected);

            ZonedDateTime result =
                    templateUtil.parseZonedDateTime("2026-1-1T0:0:0+00", "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw exception when parsing invalid ZonedDateTime")
        void should_throw_exception_when_parsing_invalid_zoned_datetime() {
            when(dateTimeUtil.parseZonedDateTime("invalid", "yyyy-MM-dd'T'HH:mm:ssZ"))
                    .thenThrow(new IllegalArgumentException("Invalid format"));

            assertThatThrownBy(() ->
                    templateUtil.parseZonedDateTime("invalid", "yyyy-MM-dd'T'HH:mm:ssZ"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("formatZonedDateTime() method tests")
    class FormatZonedDateTimeTests {

        @Test
        @DisplayName("should format ZonedDateTime successfully")
        void should_format_zoned_datetime_successfully() {
            ZonedDateTime zonedDateTime =
                    ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            when(dateTimeUtil.format(zonedDateTime, "yyyy-MM-dd'T'HH:mm:ssZ"))
                    .thenReturn("2026-1-1T0:0:0+00");

            String result =
                    templateUtil.formatZonedDateTime(zonedDateTime, "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isEqualTo("2026-1-1T0:0:0+00");
        }

        @Test
        @DisplayName("should throw exception when formatting ZonedDateTime fails")
        void should_throw_exception_when_formatting_zoned_datetime_fails() {
            ZonedDateTime zonedDateTime =
                    ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            when(dateTimeUtil.format(zonedDateTime, "invalid-format"))
                    .thenThrow(new IllegalArgumentException("Invalid format"));

            assertThatThrownBy(() ->
                        templateUtil.formatZonedDateTime(zonedDateTime, "invalid-format"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("json() method tests")
    class JsonMethodTests {

        @Test
        @DisplayName("should parse valid JSON object into a navigable node")
        void should_parse_valid_json_object_into_navigable_node() {
            JsonNode result = templateUtil.json("{\"value\":\"High\",\"id\":\"3\"}");

            assertThat(result.isObject()).isTrue();
            assertThat(result.path("value").asString()).isEqualTo("High");
            assertThat(result.path("id").asString()).isEqualTo("3");
        }

        @Test
        @DisplayName("should navigate nested JSON objects")
        void should_navigate_nested_json_objects() {
            JsonNode result = templateUtil.json("{\"nested\":{\"id\":\"7\"}}");

            assertThat(result.path("nested").path("id").asString()).isEqualTo("7");
        }

        @Test
        @DisplayName("should fall back to default when a key is absent")
        void should_fall_back_to_default_when_key_absent() {
            JsonNode result = templateUtil.json("{\"id\":\"3\"}");

            assertThat(result.path("value").isMissingNode()).isTrue();
            assertThat(result.path("value").asString("raw")).isEqualTo("raw");
        }

        @Test
        @DisplayName("should return missing node for null input")
        void should_return_missing_node_for_null_input() {
            assertThat(templateUtil.json(null).isMissingNode()).isTrue();
        }

        @Test
        @DisplayName("should return missing node for blank input")
        void should_return_missing_node_for_blank_input() {
            assertThat(templateUtil.json("   ").isMissingNode()).isTrue();
        }

        @Test
        @DisplayName("should return missing node for non-JSON input")
        void should_return_missing_node_for_non_json_input() {
            assertThat(templateUtil.json("foo").isMissingNode()).isTrue();
        }

        @Test
        @DisplayName("should return missing node for malformed JSON")
        void should_return_missing_node_for_malformed_json() {
            assertThat(templateUtil.json("{\"value\":").isMissingNode()).isTrue();
        }
    }
}
