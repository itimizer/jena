package com.itimizer.jena.service.impl;

import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.JiraIssueService;
import com.itimizer.jena.util.JiraUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * {@link JiraIssueService} backed by the Jira issue endpoint, fetching each issue with its
 * changelog, field names, schema and delegating history slicing to {@link JiraUtil}.
 */
@Slf4j
@Service
public class JiraIssueServiceImpl extends AbstractJiraClient implements JiraIssueService {

    private final JiraUtil jiraUtil;

    public JiraIssueServiceImpl(@Qualifier("jiraWebClient") WebClient webClient,
                                JiraUtil jiraUtil) {
        super(webClient);
        this.jiraUtil = jiraUtil;
    }

    @Override
    public JiraIssue fetchJiraIssue(@NonNull String issueKey) {
        if (issueKey.isEmpty()) {
            throw new ValidationException("Issue key cannot be empty");
        }
        log.debug("Starting Fetching Jira Issue for issue: {}", issueKey);

        var jiraIssue = fetch(
                jiraWebClient()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/rest/api/2/issue/{issueKey}")
                                .queryParam("fields", "-comment,-votes,-worklog")
                                .queryParam("expand", "changelog,names,schema")
                                .build(issueKey)),
                JiraIssue.class,
                "fetching jira issue " + issueKey,
                true);

        log.debug("Completed fetch Jira Issue for issue: {}", issueKey);
        log.trace("Fetched Jira Issue: {}", jiraIssue);
        return jiraIssue;
    }

    @Override
    public List<JiraIssueChangelogGroup> getJiraIssueChangelogGroups(JiraIssue jiraIssue,
                                                                     ZonedDateTime updated) {
        return jiraUtil.getJiraIssueChangelogGroups(jiraIssue, updated);
    }
}
