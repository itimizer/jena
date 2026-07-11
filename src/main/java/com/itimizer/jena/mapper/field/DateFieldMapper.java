package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;
import com.itimizer.jena.config.ApplicationProperties;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Maps date fields, formatting the date with the configured date pattern. */
public class DateFieldMapper implements BasicFieldMapper {

    @Setter
    private ApplicationProperties applicationProperties;

    private static final DateTimeFormatter JIRA_DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    @Override
    public String toValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        return LocalDate.parse(value.asString(), JIRA_DATE_FORMATTER).format(JIRA_DATE_FORMATTER);
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        return LocalDate.parse(value.asString(), JIRA_DATE_FORMATTER)
                .format(DateTimeFormatter
                        .ofPattern(applicationProperties
                                .getJira()
                                .getFormatter()
                                .getDatePattern()));
    }

    @Override
    public boolean isExernalProperties() {
        return true;
    }
}
