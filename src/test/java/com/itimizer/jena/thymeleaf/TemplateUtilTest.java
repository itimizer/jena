package com.itimizer.jena.thymeleaf;

import com.itimizer.jena.util.DateTimeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
