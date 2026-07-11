package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraUser;
import com.itimizer.jena.service.JiraUserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link JiraUserService} that resolves the authenticated Jira account and caches it. Resolution
 * is attempted at startup, but a failure (e.g. Jira down at boot) is not fatal — the user is
 * re-resolved lazily on the next {@link #getJiraUser()} call, so timezone-aware watermark handling
 * recovers as soon as Jira does. When neither a PAT nor username/password is configured, access is
 * anonymous and the user stays {@code null}.
 */
@Slf4j
@Service
public class JiraUserServiceImpl extends AbstractJiraClient implements JiraUserService {

    private final ApplicationProperties applicationProperties;

    private volatile JiraUser jiraUser;

    public JiraUserServiceImpl(ApplicationProperties applicationProperties,
                               @Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void initialize() {
        if (!isConfigured()) {
            log.info("Jira user is not configured, anonymous access will be used");
            return;
        }
        resolveJiraUser();
    }

    @Override
    public JiraUser getJiraUser() {
        var user = jiraUser;

        if (user == null && isConfigured()) {
            return resolveJiraUser();
        }
        return user;
    }

    private synchronized JiraUser resolveJiraUser() {
        if (jiraUser == null) {
            jiraUser = fetchJiraUser();

            if (jiraUser == null) {
                log.warn("Jira user could not be resolved; will retry on next use");
            } else {
                log.info("Jira user: {}", jiraUser);
            }
        }
        return jiraUser;
    }

    private boolean isConfigured() {
        var jira = applicationProperties.getJira();
        return jira.getPat() != null
                || (jira.getUsername() != null && jira.getPassword() != null);
    }

    public JiraUser fetchJiraUser() {
        log.debug("Starting fetch Jira User");
        var user = fetch(
                jiraWebClient().get().uri("/rest/api/2/myself"),
                JiraUser.class,
                "fetching jira user",
                false);
        log.debug("Completed fetch Jira User");
        log.trace("Fetched Jira User: {}", user);
        return user;
    }
}
