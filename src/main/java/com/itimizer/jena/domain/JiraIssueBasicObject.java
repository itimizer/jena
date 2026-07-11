package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Generic {@code id} + {@code name} pair used for the many Jira reference fields (status, priority,
 * resolution, components, versions, …).
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueBasicObject {

    private final long id;
    private final String name;
}
