package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * One Jira changelog history block: a timestamped, authored set of {@link JiraIssueChangelogItem}s
 * that changed together. Its {@code id} doubles as the history point identifier used for context
 * lookups.
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueChangelogGroup {

    private long id;
    private final JiraUser author;
    private ZonedDateTime created;
    private final List<JiraIssueChangelogItem> items;
}
