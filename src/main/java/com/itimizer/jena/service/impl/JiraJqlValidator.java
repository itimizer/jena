package com.itimizer.jena.service.impl;

import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.exception.ValidationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Validates filter JQL against Jira before it is persisted, by issuing a zero-result search with
 * {@code validateQuery=true}. Turns a Jira rejection into a {@link ValidationException}.
 */
@Slf4j
@Component
public class JiraJqlValidator extends AbstractJiraClient {

    public JiraJqlValidator(@Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
    }

    /**
     * Asks Jira to validate the JQL without executing it.
     *
     * @throws ValidationException if Jira rejects the JQL as invalid
     */
    public void validate(@NonNull String jql) {
        try {
            fetch(jiraWebClient()
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/rest/api/2/search")
                                    .queryParam("jql", jql)
                                    .queryParam("maxResults", 0)
                                    .queryParam("validateQuery", true)
                                    .build()),
                    JiraSearchResult.class,
                    "validating jql",
                    true);
        } catch (HttpClientErrorException e) {
            throw new ValidationException("Invalid JQL '" + jql + "': " + e.getMessage());
        }
    }
}