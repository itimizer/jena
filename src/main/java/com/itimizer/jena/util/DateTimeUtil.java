package com.itimizer.jena.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date-time conversions between local, zoned and UTC representations — used for watermark handling
 * (results are relative to the Jira user's timezone) and for the {@code #jena} template helpers.
 */
@Component
public class DateTimeUtil {

    public ZonedDateTime convertLocalToZonedDateTime(LocalDateTime localDateTime, String zone) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.of(zone));
    }

    public ZonedDateTime getZonedDateTime(LocalDateTime utcDateTime, String zone) {
        if (utcDateTime == null) {
            return null;
        }
        return utcDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(zone));
    }

    public ZonedDateTime changeTimeZone(ZonedDateTime zonedDateTime, String zone) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(ZoneId.of(zone));
    }

    public LocalDateTime getUtcLocalDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

    public LocalDateTime parseLocalDateTime(String localDateTime, String format) {
        if (localDateTime == null || localDateTime.trim().isEmpty()) {
            return null;
        }
        var formatter = DateTimeFormatter.ofPattern(format);

        return LocalDateTime.parse(localDateTime, formatter);
    }

    public ZonedDateTime parseZonedDateTime(String zonedDateTime, String format) {
        if (zonedDateTime == null || zonedDateTime.trim().isEmpty()) {
            return null;
        }
        var formatter = DateTimeFormatter.ofPattern(format);

        return ZonedDateTime.parse(zonedDateTime, formatter);
    }

    public String format(LocalDateTime localDateTime, String format) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    public String format(ZonedDateTime zonedDateTime, String format) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.format(DateTimeFormatter.ofPattern(format));
    }
}
