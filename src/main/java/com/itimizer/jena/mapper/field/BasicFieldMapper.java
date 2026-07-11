package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;
import com.itimizer.jena.config.ApplicationProperties;

/**
 * Strategy for converting one Jira custom-field type's raw JSON into a template-ready
 * {@code value}/{@code stringValue} pair. Implementations that need config (e.g. date patterns)
 * return {@code true} from {@link #isExernalProperties()} and receive it via
 * {@link #setApplicationProperties}.
 */
public interface BasicFieldMapper {

    String toValue(JsonNode value);

    String toStringValue(JsonNode value);

    boolean isExernalProperties();

    default void setApplicationProperties(ApplicationProperties applicationProperties) {
    }
}
