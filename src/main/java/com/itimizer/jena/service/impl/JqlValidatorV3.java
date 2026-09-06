package com.itimizer.jena.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

import java.util.List;

/**
 * {@link JqlValidator} for Jira Cloud: {@code POST /rest/api/3/jql/parse?validation=strict}. The
 * endpoint answers {@code 200} even for invalid JQL, so the verdict is read from the body — a
 * query is rejected only when Jira reports errors for it. A {@code 400} is also treated as a
 * verdict; any other failure (auth, a missing endpoint, an outage) propagates untranslated rather
 * than blaming the JQL.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "jena.jira.deployment-type", havingValue = "cloud")
public class JqlValidatorV3 extends AbstractJiraClient implements JqlValidator {

    public JqlValidatorV3(@Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
    }

    @Override
    public void validate(@NonNull String jql) {
        ParseResponse response;

        try {
            response = fetchRequired(jiraWebClient()
                            .post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/rest/api/3/jql/parse")
                                    .queryParam("validation", "strict")
                                    .build())
                            .bodyValue(new ParseRequest(List.of(jql))),
                    ParseResponse.class,
                    "validating jql");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ConfigurationException(
                        "Jira has no /rest/api/3/jql/parse endpoint at the configured URL, so JQL "
                                + "cannot be validated. Check jena.jira.url, and set "
                                + "jena.jira.deployment-type=server if this is a Jira "
                                + "Server/Data Center instance.");
            }
            if (e.getStatusCode() != HttpStatus.BAD_REQUEST) {
                throw e;
            }
            throw new ValidationException("Invalid JQL '" + jql + "': " + e.getMessage());
        }

        if (response == null || response.queries() == null || response.queries().isEmpty()) {
            throw new ValidationException("Could not validate JQL '" + jql
                    + "': Jira returned no parse result");
        }
        var errors = response.queries().getFirst().errors();

        if (errors != null && !errors.isEmpty()) {
            throw new ValidationException("Invalid JQL '" + jql + "': "
                    + String.join("; ", errors));
        }
    }

    /** Parse request body; the endpoint takes a batch, of which only one query is ever sent. */
    record ParseRequest(List<String> queries) {
    }

    /** Parse verdicts, one per submitted query; {@code errors} is absent for a valid query. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ParseResponse(List<ParsedQuery> queries) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ParsedQuery(String query, List<String> errors) {
        }
    }
}