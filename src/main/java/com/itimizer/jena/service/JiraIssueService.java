package com.itimizer.jena.service;

import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Fetches single Jira issues (current field values plus changelog) and slices their history.
 */
public interface JiraIssueService {

    /** Fetches one issue with its full field set and changelog, or {@code null} if not found. */
    JiraIssue fetchJiraIssue(@NonNull String issueKey);

    /**
     * The issue's changelog history groups created after {@code updated} — i.e. only the changes
     * that fall inside the current incremental window.
     */
    List<JiraIssueChangelogGroup> getJiraIssueChangelogGroups(JiraIssue jiraIssue,
                                                              ZonedDateTime updated);
}
