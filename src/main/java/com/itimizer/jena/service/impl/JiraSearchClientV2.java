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
 * {@link JiraSearchClient} for Jira Server/Data Center: {@code GET /rest/api/2/search} walked with
 * {@code startAt} offsets until the server-reported {@code total} is reached.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "jena.jira.deployment-type", havingValue = "server",
        matchIfMissing = true)
public class JiraSearchClientV2 extends AbstractJiraClient implements JiraSearchClient {

    public JiraSearchClientV2(@Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
    }

    /**
     * Progress is tracked by the number of issues actually received rather than the server-echoed
     * cursor, so a page that arrives null or empty while issues remain fails the run — advancing
     * the watermark past unseen issues would lose them silently.
     */
    @Override
    public JiraSearchResult searchAllKeys(@NonNull String jql, @NonNull String filterName) {
        log.debug("Starting Fetching Jira Search Result for filter {} with JQL: {}",
                filterName, jql);
        var first = fetchPage(jql, 0);

        if (first == null) {
            log.debug("Completed fetch Jira Search Result for filter {}", filterName);
            return null;
        }
        List<JiraIssueKey> issues = new ArrayList<>(first.issues());
        var received = (long) issues.size();

        while (received < first.total()) {
            var page = fetchPage(jql, received);

            if (page == null || page.issues().isEmpty()) {
                throw new RequestFailedException(
                        "Search page at " + received + " for filter " + filterName
                                + " returned no issues while "
                                + (first.total() - received) + " remain");
            }
            issues.addAll(page.issues());
            received += page.issues().size();
        }
        var result = new JiraSearchResult(List.copyOf(issues));
        log.debug("Completed fetch Jira Search Result for filter {}", filterName);
        log.trace("Fetched Jira Search Result: {}", result);
        return result;
    }

    private SearchPage fetchPage(@NonNull String jql, long startAt) {
        return fetch(
                jiraWebClient()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/rest/api/2/search")
                                .queryParam("fields", "key")
                                .queryParam("startAt", startAt)
                                .queryParam("jql", jql)
                                .build()),
                SearchPage.class,
                "fetching jira search result",
                true);
    }

    /**
     * One v2 search page. Only the fields the paging loop consumes are bound; the echoed
     * {@code startAt}/{@code maxResults} are deliberately ignored.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchPage(long total, List<JiraIssueKey> issues) {
    }
}