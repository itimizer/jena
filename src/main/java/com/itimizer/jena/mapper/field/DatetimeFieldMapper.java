package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;
import com.itimizer.jena.config.ApplicationProperties;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Maps datetime fields, formatting the timestamp with the configured datetime pattern. */
public class DatetimeFieldMapper implements BasicFieldMapper {

    @Setter
    private ApplicationProperties applicationProperties;

    private static final DateTimeFormatter JIRA_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Override
    public String toValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        return ZonedDateTime.parse(value.asString(), JIRA_DATE_TIME_FORMATTER)
                .format(DATETIME_FORMATTER);
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        return ZonedDateTime.parse(value.asString(), JIRA_DATE_TIME_FORMATTER)
                .format(DateTimeFormatter
                        .ofPattern(applicationProperties
                                .getJira()
                                .getFormatter()
                                .getDatetimePattern()));
    }

    @Override
    public boolean isExernalProperties() {
        return true;
    }
}
