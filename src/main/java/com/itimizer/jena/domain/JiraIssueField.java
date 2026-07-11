package com.itimizer.jena.domain;

import tools.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Schema + value of one issue field as seen during deserialization: its id/name, type metadata
 * ({@code type}, {@code items}, {@code system}, {@code custom}) and the raw JSON {@code value},
 * which the field mappers turn into a {@link TemplateField}.
 */
@Data
public class JiraIssueField {

    private final String key;
    private final String name;
    private final String type;
    private final String items;
    private final String system;
    private final String custom;
    private final JsonNode value;
}
