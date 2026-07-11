package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.JiraSearchRun;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.exception.RequestFailedException;
import com.itimizer.jena.repository.JiraSearchRunRepository;
import com.itimizer.jena.service.JiraSearchResultService;
import com.itimizer.jena.service.JiraUserService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import com.itimizer.jena.util.DateTimeUtil;
import com.itimizer.jena.util.JiraUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * {@link JiraSearchResultService} over the Jira search endpoint. Fetches keys only, walking all
 * result pages, and persists the per-filter {@link JiraSearchRun} cursor. The cursor's
 * last-successful-run time is the watermark and only advances on a successful run, so a failed poll
 * never skips a window.
 */
@Slf4j
@Service
public class JiraSearchResultServiceImpl extends AbstractJiraClient
        implements JiraSearchResultService {

    private final TransactionRunner transactionRunner;
    private final ApplicationProperties applicationProperties;
    private final JiraSearchRunRepository jiraSearchRunRepository;
    private final JiraUserService jiraUserService;
    private final JiraUtil jiraUtil;
    private final DateTimeUtil dateTimeUtil;

    public JiraSearchResultServiceImpl(TransactionRunner transactionRunner,
                                       ApplicationProperties applicationProperties,
                                       JiraSearchRunRepository jiraSearchRunRepository,
                                       JiraUserService jiraUserService,
                                       JiraUtil jiraUtil,
                                       DateTimeUtil dateTimeUtil,
                                       @Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
        this.transactionRunner = transactionRunner;
        this.applicationProperties = applicationProperties;
        this.jiraSearchRunRepository = jiraSearchRunRepository;
        this.jiraUserService = jiraUserService;
        this.jiraUtil = jiraUtil;
        this.dateTimeUtil = dateTimeUtil;
    }

    @Override
    public JiraSearchResult fetchAllJiraSearchResult(@NonNull JiraFilter filter,
                                                     @NonNull LocalDateTime updated) {
        return fetchAllPaged(jiraUtil.getJiraSearchJql(filter.getJql(), updated), filter.getName());
    }

    @Override
    public JiraSearchResult fetchSnapshotSearchResult(@NonNull JiraFilter filter) {
        return fetchAllPaged(filter.getJql(), filter.getName());
    }

    /**
     * Runs the search and accumulates every page into a single result. Progress is tracked by the
     * number of issues actually received rather than the server-echoed cursor, so a page that
     * arrives null or empty while issues remain fails the run — advancing the watermark past
     * unseen issues would lose them silently.
     */
    private JiraSearchResult fetchAllPaged(@NonNull String jql, @NonNull String filterName) {
        log.debug("Starting Fetching Jira Search Result for filter {} with JQL: {}",
                filterName, jql);
        var res = fetchJiraSearchResult(jql, 0);

        if (res != null) {
            var received = (long) res.getIssues().size();

            while (received < res.getTotal()) {
                var page = fetchJiraSearchResult(jql, received);

                if (page == null || page.getIssues().isEmpty()) {
                    throw new RequestFailedException(
                            "Search page at " + received + " for filter " + filterName
                                    + " returned no issues while "
                                    + (res.getTotal() - received) + " remain");
                }
                res.getIssues().addAll(page.getIssues());
                received += page.getIssues().size();
            }
            res.setStartAt(received);
        }

        log.debug("Completed fetch Jira Search Result for filter {}", filterName);
        log.trace("Fetched Jira Search Result: {}", res);
        return res;
    }

    private JiraSearchResult fetchJiraSearchResult(@NonNull String jql, long startAt) {
        return fetch(
                jiraWebClient()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/rest/api/2/search")
                                .queryParam("fields", "key")
                                .queryParam("startAt", startAt)
                                .queryParam("jql", jql)
                                .build()),
                JiraSearchResult.class,
                "fetching jira search result",
                true);
    }

    /**
     * Resolves the window start in the Jira user's timezone (falling back to the system zone): the
     * stored last-successful-run time, or — on first run — the configured initial timestamp, or
     * now.
     */
    @Override
    public ZonedDateTime getLastUpdated(@NonNull JiraFilter filter) {
        var jiraUser = jiraUserService.getJiraUser();
        var timeZone = (jiraUser != null && jiraUser.getTimeZone() != null)
                ? jiraUser.getTimeZone() : ZoneId.systemDefault().getId();
        ZonedDateTime updated;
        var jiraSearchRun = jiraSearchRunRepository.findByFilterId(filter.getId());

        if (jiraSearchRun != null && jiraSearchRun.getLastSuccessRunTime() != null) {
            updated = dateTimeUtil.getZonedDateTime(
                    jiraSearchRun.getLastSuccessRunTime(), timeZone);
        } else {
            if (applicationProperties.getJira().getInitialUpdatedAfter() != null) {
                updated = dateTimeUtil.convertLocalToZonedDateTime(
                        applicationProperties.getJira().getInitialUpdatedAfter(), timeZone);
            } else {
                updated = dateTimeUtil.changeTimeZone(ZonedDateTime.now(), timeZone);
            }
        }
        return updated;
    }

    /**
     * Upserts the filter's cursor. {@code lastRunTime} always moves to {@code runTime}, but the
     * watermark ({@code lastSuccessRunTime}) and {@code SUCCESS} status are set only when
     * {@code jiraSearchResult} is non-null — a {@code null} result records a failed run and
     * increments the consecutive-failure count (reset to zero on success).
     */
    public JiraSearchRun saveJiraSearchResult(@NonNull JiraFilter filter,
                                              JiraSearchResult jiraSearchResult,
                                              @NonNull ZonedDateTime runTime) {
        var jiraSearchRun = jiraSearchRunRepository.findByFilterId(filter.getId());

        if (jiraSearchRun == null) {
            jiraSearchRun = new JiraSearchRun();
            jiraSearchRun.setFilter(filter);
            jiraSearchRun.setBaseUrl(applicationProperties.getJira().getUrl());
            jiraSearchRun.setLastSuccessRunTime(jiraSearchResult != null
                    ? dateTimeUtil.getUtcLocalDateTime(runTime) : null);
        } else {
            if (jiraSearchResult != null) {
                jiraSearchRun.setLastSuccessRunTime(dateTimeUtil.getUtcLocalDateTime(runTime));
            }
        }
        jiraSearchRun.setLastRunTime(dateTimeUtil.getUtcLocalDateTime(runTime));
        jiraSearchRun.setStatus(jiraSearchResult != null ? Status.SUCCESS : Status.ERROR);
        jiraSearchRun.setFailureCount(jiraSearchResult != null
                ? 0 : jiraSearchRun.getFailureCount() + 1);

        var finalJiraSearchRun = jiraSearchRun;
        return transactionRunner
                .doInTransaction(() -> jiraSearchRunRepository.save(finalJiraSearchRun));
    }
}
