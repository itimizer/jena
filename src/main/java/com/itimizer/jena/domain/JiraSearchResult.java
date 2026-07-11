package com.itimizer.jena.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * One page of a Jira search response: paging metadata ({@code startAt}, {@code maxResults},
 * {@code total}) and the matched {@link JiraIssueKey}s. The service accumulates pages into a single
 * instance.
 */
@Data
@AllArgsConstructor
public class JiraSearchResult {

    private long startAt;
    private final long maxResults;
    private final long total;
    private final List<JiraIssueKey> issues;
}
