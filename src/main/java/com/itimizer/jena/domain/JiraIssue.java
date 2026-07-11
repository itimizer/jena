package com.itimizer.jena.domain;

import tools.jackson.databind.annotation.JsonDeserialize;
import com.itimizer.jena.mapper.JiraIssueDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory model of a single Jira issue as returned by the issue endpoint: current field values
 * (system and custom), the raw {@code names} map used to resolve changelog labels, and the
 * changelog history. Deserialized by {@code JiraIssueDeserializer}.
 */
@Getter
@ToString
@AllArgsConstructor
@JsonDeserialize(using = JiraIssueDeserializer.class)
public class JiraIssue {

    private final long id;
    private final String self;
    private final String key;
    private final JiraIssueBasicObject status;
    private final JiraIssueBasicObject issueType;
    private final JiraProject project;
    private final List<JiraIssueBasicObject> components;
    private final String summary;
    private final String description;
    private final JiraUser creator;
    private final JiraUser reporter;
    private final JiraUser assignee;
    private final JiraIssueBasicObject resolution;
    private final ZonedDateTime created;
    private final ZonedDateTime updated;
    private final ZonedDateTime resolutiondate;
    private final LocalDateTime duedate;
    private final JiraIssueBasicObject priority;
    private final List<JiraIssueBasicObject> fixVersions;
    private final List<JiraIssueBasicObject> versions;
    private final long timeoriginalestimate;
    private final long timeestimate;
    private final long timespent;
    private final Set<String> labels;
    private final List<JiraIssueField> issueFields;
    private final List<JiraIssueChangelogGroup> changelog;
    private final Map<String, String> names;
}
