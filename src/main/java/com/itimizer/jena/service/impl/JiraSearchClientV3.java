package com.itimizer.jena.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itimizer.jena.domain.JiraIssueKey;
import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.exception.RequestFailedException;
import com.itimizer.jena.service.JiraSearchClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link JiraSearchClient} for Jira Cloud, which removed the v2 search endpoint:
 * {@code GET /rest/api/3/search/jql} walked by opaque {@code nextPageToken} cursors. The response
 * carries no total, so the last page is the one that comes back without a token. Requesting keys
 * only allows {@value #MAX_RESULTS} per page, so most polls are a single call.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "jena.jira.deployment-type", havingValue = "cloud")
public class JiraSearchClientV3 extends AbstractJiraClient implements JiraSearchClient {

    private static final int MAX_RESULTS = 5000;

    public JiraSearchClientV3(@Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
    }

    /**
     * Without a total there is nothing to reconcile the accumulated issues against, so the loop
     * trusts the cursor and instead refuses to continue when it stops making sense: a page that
     * fails to arrive while a token is outstanding, or one that hands back the very token it was
     * requested with, fails the run rather than truncating or spinning forever.
     */
    @Override
    public JiraSearchResult searchAllKeys(@NonNull String jql, @NonNull String filterName) {
        log.debug("Starting Fetching Jira Search Result for filter {} with JQL: {}",
                filterName, jql);
        var first = fetchPage(jql, null);

        if (first == null) {
            log.debug("Completed fetch Jira Search Result for filter {}", filterName);
            return null;
        }
        List<JiraIssueKey> issues = new ArrayList<>(first.issues());
        var token = first.nextPageToken();

        while (token != null) {
            var page = fetchPage(jql, token);

            if (page == null) {
                throw new RequestFailedException(
                        "Search page for filter " + filterName
                                + " returned nothing while a next page was outstanding");
            }
            if (token.equals(page.nextPageToken())) {
                throw new RequestFailedException(
                        "Search page for filter " + filterName
                                + " returned the same next page token it was requested with");
            }
            issues.addAll(page.issues());
            token = page.nextPageToken();
        }
        var result = new JiraSearchResult(List.copyOf(issues));
        log.debug("Completed fetch Jira Search Result for filter {}", filterName);
        log.trace("Fetched Jira Search Result: {}", result);
        return result;
    }

    private SearchPage fetchPage(@NonNull String jql, String nextPageToken) {
        return fetch(
                jiraWebClient()
                        .get()
                        .uri(uriBuilder -> {
                            uriBuilder
                                    .path("/rest/api/3/search/jql")
                                    .queryParam("fields", "key")
                                    .queryParam("maxResults", MAX_RESULTS)
                                    .queryParam("jql", jql);

                            if (nextPageToken != null) {
                                uriBuilder.queryParam("nextPageToken", nextPageToken);
                            }
                            return uriBuilder.build();
                        }),
                SearchPage.class,
                "fetching jira search result",
                true);
    }

    /** One v3 search page; {@code nextPageToken} is absent on the last one. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchPage(List<JiraIssueKey> issues, String nextPageToken) {
    }
}