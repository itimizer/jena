package com.itimizer.jena.thymeleaf;

import com.itimizer.jena.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.MissingNode;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * The {@code #jena} template helper: parse/format methods for local and zoned date-times (used to
 * reformat Jira date fields), plus {@link #json(String)} for navigating the raw JSON of
 * structurally-unknown fields inside Thymeleaf templates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateUtil {

    private final DateTimeUtil dateTimeUtil;
    private final ObjectMapper objectMapper;

    public LocalDateTime parseLocalDateTime(String localDateTime, String format) {
        try {
            return dateTimeUtil.parseLocalDateTime(localDateTime, format);
        } catch (Exception e) {
            log.error("Error occurred while parsing LocalDateTime "
                            + "attribute in Thymeleaf template: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String formatLocalDateTime(LocalDateTime localDateTime, String format) {
        try {
            return dateTimeUtil.format(localDateTime, format);
        } catch (Exception e) {
            log.error("Error occurred while formatting LocalDateTime"
                    + " attribute in Thymeleaf template: {}", e.getMessage(), e);
            throw e;
        }
    }

    public ZonedDateTime parseZonedDateTime(String zonedDateTime, String format) {
        try {
            return dateTimeUtil.parseZonedDateTime(zonedDateTime, format);
        } catch (Exception e) {
            log.error("Error occurred while parsing ZonedDateTime"
                    + " attribute in Thymeleaf template: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String formatZonedDateTime(ZonedDateTime zonedDateTime, String format) {
        try {
            return dateTimeUtil.format(zonedDateTime, format);
        } catch (Exception e) {
            log.error("Error occurred while formatting ZonedDateTime"
                    + " attribute in Thymeleaf template: {}", e.getMessage(), e);
            throw e;
        }
    }

    public JsonNode json(String raw) {
        if (raw == null || raw.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException e) {
            log.debug("Unparseable JSON"
                    + " attribute in Thymeleaf template: {}", e.getMessage());
            return MissingNode.getInstance();
        }
    }
}
