package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itimizer.jena.entity.Rule;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A single field change within a changelog group, mirroring Jira's structure: {@code field}/
 * {@code fieldtype} plus the old/new value in both id form ({@code from}/{@code to}) and human form
 * ({@code fromString}/{@code toString}). This is what
 * {@link Rule} matches against.
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueChangelogItem {

    private final String field;
    private final String fieldtype;
    private final String from;
    private final String fromString;
    private final String to;
    private final String toString;
}
