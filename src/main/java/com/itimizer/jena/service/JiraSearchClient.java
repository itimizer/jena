package com.itimizer.jena.service;

import com.itimizer.jena.domain.JiraSearchResult;
import lombok.NonNull;

/**
 * Runs a JQL search against Jira and returns every matching issue key. Implementations differ only
 * in the search endpoint and paging scheme of the targeted deployment; exactly one is wired in,
 * selected by {@code jena.jira.deployment-type}.
 */
public interface JiraSearchClient {

    /**
     * Accumulates every page of the search into a single result.
     *
     * @param jql the query to run
     * @param filterName the filter the query belongs to, used in log and error messages
     * @return all matched issue keys, or {@code null} if Jira answered the first page with a 404
     * @throws com.itimizer.jena.exception.RequestFailedException if a page fails or the paging
     *         sequence cannot be trusted to have returned every issue
     */
    JiraSearchResult searchAllKeys(@NonNull String jql, @NonNull String filterName);
}