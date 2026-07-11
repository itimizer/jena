package com.itimizer.jena.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The view of a Jira field exposed to templates: {@code stringValue} (human-readable text, matching
 * changelog {@code *String} values) and {@code value} (the underlying id/key, often {@code null}
 * for text fields). Templates reference these as {@code field.stringValue} / {@code field.value}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateField {

    private String value;
    private String stringValue;
}
