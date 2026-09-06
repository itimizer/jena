package com.itimizer.jena.domain;

import java.util.List;

/**
 * Every {@link JiraIssueKey} matched by a search, accumulated across all result pages. Paging
 * metadata is transport detail of a particular Jira API version and stays inside the search client
 * that produced it.
 */
public record JiraSearchResult(List<JiraIssueKey> issues) {
}