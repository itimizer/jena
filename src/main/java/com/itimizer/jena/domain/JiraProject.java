package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A Jira project reference — an {@link JiraIssueBasicObject} extended with the project {@code key}.
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraProject extends JiraIssueBasicObject {

    private final String key;
}
