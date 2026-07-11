package com.itimizer.jena.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DateTimeUtil Tests")
class DateTimeUtilTest {

    private final DateTimeUtil dateTimeUtil = new DateTimeUtil();

    @Nested
    @DisplayName("convertLocalToZonedDateTime() method tests")
    class ConvertLocalToZonedDateTimeTests {

        @Test
        @DisplayName("should convert LocalDateTime to ZonedDateTime with UTC zone")
        void should_convert_local_to_zoned_utc() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            ZonedDateTime result = dateTimeUtil.convertLocalToZonedDateTime(localDateTime, "UTC");
            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isEqualTo(0);
            assertThat(result.getMinute()).isEqualTo(0);
            assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("should convert LocalDateTime to ZonedDateTime with custom zone")
        void should_convert_local_to_zoned_custom_zone() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            ZonedDateTime result = dateTimeUtil.convertLocalToZonedDateTime(localDateTime,
                    "America/New_York");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("America/New_York"));
        }

        @Test
        @DisplayName("should throw exception for invalid zone")
        void should_throw_exception_for_invalid_zone() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            assertThatThrownBy(() -> dateTimeUtil.convertLocalToZonedDateTime(localDateTime,
                    "Invalid/Zone"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should return null for null LocalDateTime")
        void should_return_null_for_null_local_datetime() {
            ZonedDateTime result = dateTimeUtil.convertLocalToZonedDateTime(null, "UTC");
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getZonedDateTime() method tests")
    class GetZonedDateTimeTests {

        @Test
        @DisplayName("should convert UTC LocalDateTime to target zone")
        void should_convert_utc_to_target_zone() {
            LocalDateTime utcDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            ZonedDateTime result = dateTimeUtil.getZonedDateTime(utcDateTime, "America/New_York");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("America/New_York"));
            assertThat(result.getHour()).isEqualTo(19);
        }

        @Test
        @DisplayName("should handle UTC to UTC conversion")
        void should_handle_utc_to_utc() {
            LocalDateTime utcDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            ZonedDateTime result = dateTimeUtil.getZonedDateTime(utcDateTime, "UTC");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC"));
            assertThat(result.getHour()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle UTC to Europe/London conversion")
        void should_handle_utc_to_london() {
            LocalDateTime utcDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            ZonedDateTime result = dateTimeUtil.getZonedDateTime(utcDateTime, "Europe/London");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("Europe/London"));
            assertThat(result.getHour()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw exception for invalid zone")
        void should_throw_exception_for_invalid_zone() {
            LocalDateTime utcDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            assertThatThrownBy(() -> dateTimeUtil.getZonedDateTime(utcDateTime, "Invalid/Zone"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("changeTimeZone() method tests")
    class ChangeTimeZoneTests {

        @Test
        @DisplayName("should change from UTC to America/New_York")
        void should_change_from_utc_to_new_york() {
            ZonedDateTime utcDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

            ZonedDateTime result = dateTimeUtil.changeTimeZone(utcDateTime, "America/New_York");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("America/New_York"));
            assertThat(result.getHour()).isEqualTo(19);
        }

        @Test
        @DisplayName("should change from America/New_York to UTC")
        void should_change_from_new_york_to_utc() {
            ZonedDateTime newYorkDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("America/New_York"));

            ZonedDateTime result = dateTimeUtil.changeTimeZone(newYorkDateTime, "UTC");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC"));
            assertThat(result.getHour()).isEqualTo(5);
        }

        @Test
        @DisplayName("should handle same zone conversion")
        void should_handle_same_zone_conversion() {
            ZonedDateTime utcDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

            ZonedDateTime result = dateTimeUtil.changeTimeZone(utcDateTime, "UTC");
            assertThat(result).isNotNull();
            assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC"));
            assertThat(result.getHour()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw exception for invalid zone")
        void should_throw_exception_for_invalid_zone() {
            ZonedDateTime utcDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

            assertThatThrownBy(() -> dateTimeUtil.changeTimeZone(utcDateTime, "Invalid/Zone"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("getUtcLocalDateTime() method tests")
    class GetUtcLocalDateTimeTests {

        @Test
        @DisplayName("should convert ZonedDateTime to UTC LocalDateTime from America/New_York")
        void should_convert_to_utc_from_new_york() {
            ZonedDateTime newYorkDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("America/New_York"));

            LocalDateTime result = dateTimeUtil.getUtcLocalDateTime(newYorkDateTime);
            assertThat(result).isNotNull();
            assertThat(result.getHour()).isEqualTo(5);
            assertThat(result.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("should convert ZonedDateTime to UTC LocalDateTime from UTC")
        void should_convert_from_utc_to_utc() {
            ZonedDateTime utcDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("UTC"));

            LocalDateTime result = dateTimeUtil.getUtcLocalDateTime(utcDateTime);
            assertThat(result).isNotNull();
            assertThat(result.getHour()).isEqualTo(0);
            assertThat(result.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("should convert ZonedDateTime to UTC LocalDateTime from Europe/London")
        void should_convert_from_london_to_utc() {
            ZonedDateTime londonDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("Europe/London"));

            LocalDateTime result = dateTimeUtil.getUtcLocalDateTime(londonDateTime);
            assertThat(result).isNotNull();
            assertThat(result.getHour()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return null for null ZonedDateTime")
        void should_return_null_for_null_zoned_date_time() {
            LocalDateTime result = dateTimeUtil.getUtcLocalDateTime(null);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("parseLocalDateTime() method tests")
    class ParseLocalDateTimeTests {

        @Test
        @DisplayName("should parse valid LocalDateTime string")
        void should_parse_valid_local_datetime() {
            String dateTimeString = "2026-01-01 00:00:00";

            LocalDateTime result = dateTimeUtil.parseLocalDateTime(dateTimeString,
                    "yyyy-MM-dd HH:mm:ss");
            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isEqualTo(0);
            assertThat(result.getMinute()).isEqualTo(0);
            assertThat(result.getSecond()).isEqualTo(0);
        }

        @Test
        @DisplayName("should parse LocalDateTime with different format")
        void should_parse_local_datetime_different_format() {
            String dateTimeString = "01/01/2026 00:00";

            LocalDateTime result = dateTimeUtil.parseLocalDateTime(dateTimeString,
                    "dd/MM/yyyy HH:mm");
            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return null for null input")
        void should_return_null_for_null_input() {
            LocalDateTime result = dateTimeUtil.parseLocalDateTime(null,
                    "yyyy-MM-dd HH:mm:ss");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for empty string")
        void should_return_null_for_empty_string() {
            LocalDateTime result = dateTimeUtil.parseLocalDateTime("",
                    "yyyy-MM-dd HH:mm:ss");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for whitespace string")
        void should_return_null_for_whitespace_string() {
            LocalDateTime result = dateTimeUtil.parseLocalDateTime("   ",
                    "yyyy-MM-dd HH:mm:ss");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception for invalid format")
        void should_throw_exception_for_invalid_format() {
            String dateTimeString = "2026-1-1 00:00:00";

            assertThatThrownBy(() -> dateTimeUtil.parseLocalDateTime(dateTimeString,
                    "yyyy-MM-dd HH:mm"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw exception for invalid date string")
        void should_throw_exception_for_invalid_date_string() {
            String dateTimeString = "invalid-date-time";

            assertThatThrownBy(() -> dateTimeUtil.parseLocalDateTime(dateTimeString,
                    "yyyy-MM-dd HH:mm:ss"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("parseZonedDateTime() method tests")
    class ParseZonedDateTimeTests {

        @Test
        @DisplayName("should parse valid ZonedDateTime string")
        void should_parse_valid_zoned_datetime() {
            String dateTimeString = "2026-01-01T00:00:00+00:00";

            ZonedDateTime result = dateTimeUtil.parseZonedDateTime(dateTimeString,
                    "yyyy-MM-dd'T'HH:mm:ssXXX");
            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isEqualTo(0);
            assertThat(result.getMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("should parse ZonedDateTime with timezone offset")
        void should_parse_zoned_datetime_with_offset() {
            String dateTimeString = "2026-01-01T00:00:00+0500";

            ZonedDateTime result = dateTimeUtil.parseZonedDateTime(dateTimeString,
                    "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("should return null for null input")
        void should_return_null_for_null_input() {
            ZonedDateTime result = dateTimeUtil.parseZonedDateTime(null,
                    "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for empty string")
        void should_return_null_for_empty_string() {
            ZonedDateTime result = dateTimeUtil.parseZonedDateTime("",
                    "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for whitespace string")
        void should_return_null_for_whitespace_string() {
            ZonedDateTime result = dateTimeUtil.parseZonedDateTime("   ",
                    "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception for invalid format")
        void should_throw_exception_for_invalid_format() {
            String dateTimeString = "2026-01-01T00:00:00+05:00";

            assertThatThrownBy(() -> dateTimeUtil.parseZonedDateTime(dateTimeString,
                    "yyyy-MM-dd HH:mm:ss"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should throw exception for invalid date string")
        void should_throw_exception_for_invalid_date_string() {
            String dateTimeString = "invalid-zoned-datetime";

            assertThatThrownBy(() -> dateTimeUtil.parseZonedDateTime(dateTimeString,
                    "yyyy-MM-dd'T'HH:mm:ssZ"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("format() method tests for LocalDateTime")
    class FormatLocalDateTimeTests {

        @Test
        @DisplayName("should format LocalDateTime to string")
        void should_format_local_datetime() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            String result = dateTimeUtil.format(localDateTime, "yyyy-MM-dd HH:mm:ss");
            assertThat(result).isEqualTo("2026-01-01 00:00:00");
        }

        @Test
        @DisplayName("should format LocalDateTime with different format")
        void should_format_local_datetime_different_format() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            String result = dateTimeUtil.format(localDateTime, "dd/MM/yyyy HH:mm");
            assertThat(result).isEqualTo("01/01/2026 00:00");
        }

        @Test
        @DisplayName("should format LocalDateTime with ISO format")
        void should_format_local_datetime_iso_format() {
            LocalDateTime localDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

            String result = dateTimeUtil.format(localDateTime, "yyyy-MM-dd'T'HH:mm:ss");
            assertThat(result).isEqualTo("2026-01-01T00:00:00");
        }

        @Test
        @DisplayName("should return null for null LocalDateTime")
        void should_return_null_for_null_local_datetime() {
            String result = dateTimeUtil.format((LocalDateTime) null, "yyyy-MM-dd'T'HH:mm:ss");
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("format() method tests for ZonedDateTime")
    class FormatZonedDateTimeTests {

        @Test
        @DisplayName("should format ZonedDateTime to string")
        void should_format_zoned_datetime() {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("UTC"));

            String result = dateTimeUtil.format(zonedDateTime, "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isEqualTo("2026-01-01T00:00:00+0000");
        }

        @Test
        @DisplayName("should format ZonedDateTime with timezone offset format")
        void should_format_zoned_datetime_with_offset() {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("America/New_York"));

            String result = dateTimeUtil.format(zonedDateTime, "yyyy-MM-dd'T'HH:mm:ssXXX");
            assertThat(result).isEqualTo("2026-01-01T00:00:00-05:00");
        }

        @Test
        @DisplayName("should format ZonedDateTime with custom format")
        void should_format_zoned_datetime_custom_format() {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0,
                    ZoneId.of("UTC"));

            String result = dateTimeUtil.format(zonedDateTime, "dd/MM/yyyy HH:mm");
            assertThat(result).isEqualTo("01/01/2026 00:00");
        }

        @Test
        @DisplayName("should return null for null ZonedDateTime")
        void should_return_null_for_null_zoned_datetime() {

            String result = dateTimeUtil.format((ZonedDateTime) null, "yyyy-MM-dd'T'HH:mm:ssZ");
            assertThat(result).isNull();
        }
    }
}

