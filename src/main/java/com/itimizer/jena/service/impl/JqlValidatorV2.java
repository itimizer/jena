package com.itimizer.jena.service.impl;

import com.itimizer.jena.exception.ConfigurationException;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.JqlValidator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link JqlValidator} for Jira Server/Data Center, which has no dedicated parse endpoint: it
 * issues a zero-result search with {@code validateQuery=true} and reads the verdict from the
 * status code — Jira rejects invalid JQL with a {@code 400}. The response body is irrelevant and
 * is not parsed. Only that {@code 400} is a verdict on the query: any other failure (auth, a
 * removed endpoint, an outage) propagates untranslated rather than blaming the JQL.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "jena.jira.deployment-type", havingValue = "server",
        matchIfMissing = true)
public class JqlValidatorV2 extends AbstractJiraClient implements JqlValidator {

    public JqlValidatorV2(@Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
    }

    @Override
    public void validate(@NonNull String jql) {
        try {
            fetchRequired(jiraWebClient()
                            .get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/rest/api/2/search")
                                    .queryParam("jql", jql)
                                    .queryParam("maxResults", 0)
                                    .queryParam("validateQuery", true)
                                    .build()),
                    Void.class,
                    "validating jql");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ConfigurationException(
                        "Jira has no /rest/api/2/search endpoint at the configured URL, so JQL "
                                + "cannot be validated. Check jena.jira.url, and set "
                                + "jena.jira.deployment-type=cloud if this is a Jira Cloud site.");
            }
            if (e.getStatusCode() != HttpStatus.BAD_REQUEST) {
                throw e;
            }
            throw new ValidationException("Invalid JQL '" + jql + "': " + e.getMessage());
        }
    }
}