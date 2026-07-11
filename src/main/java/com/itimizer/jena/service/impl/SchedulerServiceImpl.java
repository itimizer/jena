package com.itimizer.jena.service.impl;

import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueKey;
import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.service.ChangelogService;
import com.itimizer.jena.service.JiraFilterService;
import com.itimizer.jena.service.JiraIssueService;
import com.itimizer.jena.service.JiraSearchResultService;
import com.itimizer.jena.service.NotificationService;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.service.SchedulerService;
import com.itimizer.jena.util.JiraUtil;
import com.itimizer.jena.util.RuleUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Default {@link SchedulerService}. INCREMENTAL polling fans out across {@code jiraPollExecutor}
 * (sized by {@code jena.jira.poll.concurrency}) and propagates the caller's MDC into each worker so
 * log correlation survives the thread hop. Digests instead run on the snapshot scheduler pool and
 * start their own trace id.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    private final JiraSearchResultService jiraSearchResultService;
    private final NotificationService notificationService;
    private final JiraIssueService jiraIssueService;
    private final ChangelogService changelogService;
    private final RuleService ruleService;
    private final JiraFilterService jiraFilterService;
    private final RuleUtil ruleUtil;
    private final JiraUtil jiraUtil;
    private final MeterRegistry meterRegistry;
    private final ExecutorService jiraPollExecutor;

    @Override
    public void findJiraNotificationItems() {
        var sample = Timer.start(meterRegistry);
        try {
            var filters = jiraFilterService.findEnabled(FilterMode.INCREMENTAL);
            var mdc = MDC.getCopyOfContextMap();

            var futures = filters.stream()
                    .map(filter -> CompletableFuture.runAsync(
                            () -> runWithMdc(mdc, () -> pollFilterSafely(filter)),
                            jiraPollExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } finally {
            sample.stop(meterRegistry.timer("jena.jira.poll"));
        }
    }

    /**
     * Isolates a single filter's polling failure: logs it and still persists a (failed) search run
     * so watermark bookkeeping stays consistent and the next cycle resumes cleanly.
     */
    private void pollFilterSafely(JiraFilter filter) {
        try {
            pollFilter(filter);
        } catch (Exception e) {
            log.error("Polling failed for filter {}: {}", filter.getName(), e.getMessage(), e);
            try {
                jiraSearchResultService.saveJiraSearchResult(filter, null, ZonedDateTime.now());
            } catch (Exception saveException) {
                log.error("Error occurred while saving failed jira search run for filter {}: {}",
                        filter.getName(), saveException.getMessage(), saveException);
            }
        }
    }

    /**
     * Polls one filter for its incremental window. A {@code null} watermark means the filter has no
     * usable starting point yet, so the run is skipped without advancing it; otherwise the matched
     * issues are processed and the run is saved with the pre-fetch timestamp as the next watermark.
     */
    private void pollFilter(JiraFilter filter) {
        var updated = jiraSearchResultService.getLastUpdated(filter);
        log.trace("Got updated for filter {}: {}", filter.getName(), updated);

        if (updated == null) {
            return;
        }
        var now = ZonedDateTime.now();
        var jiraSearchResult =
                jiraSearchResultService.fetchAllJiraSearchResult(filter, updated.toLocalDateTime());
        log.trace("Got jira search result for filter {}: {}", filter.getName(), jiraSearchResult);

        if (jiraSearchResult != null) {
            processJiraSearchResult(filter, jiraSearchResult, updated);
        }
        jiraSearchResultService.saveJiraSearchResult(filter, jiraSearchResult, now);
    }

    /**
     * Walks the issues returned by the filter, short-circuiting when the filter has no enabled
     * rules. Each issue is fetched in full; an {@code ISSUE_CREATED} match fires only when the
     * issue was created after the window start, then the issue's changelog groups are evaluated for
     * {@code ISSUE_UPDATED} rules.
     */
    private void processJiraSearchResult(JiraFilter filter, JiraSearchResult jiraSearchResult,
                                         ZonedDateTime updated) {
        log.debug("Starting process Jira Search Result for filter {}", filter.getName());

        if (!ruleService.hasEnabledRules(filter.getId())) {
            log.debug("Filter {} has no enabled linked rules; skipping notification processing",
                    filter.getName());
            return;
        }
        for (var issueKey : jiraSearchResult.getIssues()) {
            log.trace("Fetching JiraIssue: {}", issueKey.getKey());
            var jiraIssue = jiraIssueService.fetchJiraIssue(issueKey.getKey());

            if (jiraIssue == null) {
                log.warn("Jira issue {} not found, skipping", issueKey.getKey());
                continue;
            }
            if (jiraUtil.isIssueCreatedAfter(jiraIssue, updated)) {
                processCreatedIssue(filter, jiraIssue, issueKey);
            }
            processChangelogGroups(filter, jiraIssue, issueKey, updated);
        }
        log.debug("Complete process Jira Search Result for filter {}", filter.getName());
    }

    /**
     * Records an {@code ISSUE_CREATED} changelog per matching rule. A creation has no real
     * changelog group, so synthetic group id {@code 0} and item index {@code 0} are used.
     */
    private void processCreatedIssue(JiraFilter filter, JiraIssue jiraIssue,
                                     JiraIssueKey issueKey) {
        var rulesByEvent =
                ruleService.findRulesByEvent(filter.getId(), true, Event.ISSUE_CREATED);

        if (rulesByEvent.isEmpty()) {
            return;
        }
        var fields = jiraUtil.getJiraIssueFieldsCtx(jiraIssue, false);

        for (var rule : rulesByEvent) {
            log.debug("Found jira issue {} matched rule {} of filter {} [ISSUE_CREATED]",
                    issueKey, rule.getId(), filter.getName());
            log.trace("Fields for jira issue {} matched rule {} [ISSUE_CREATED]: {}",
                    issueKey, rule.getId(), fields);
            changelogService.create(new ChangelogCreateDto(Event.ISSUE_CREATED,
                    0L, 0, issueKey.getKey(), rule.getId(), filter.getId(), fields));
        }
    }

    /**
     * Evaluates each changelog item against the filter's {@code ISSUE_UPDATED} rules. The raw Jira
     * field label is first resolved to its field id; a match records the item keyed by its group id
     * and position within the group.
     */
    private void processChangelogGroups(JiraFilter filter, JiraIssue jiraIssue,
                                        JiraIssueKey issueKey, ZonedDateTime updated) {
        var jiraIssueChangelogGroups =
                jiraIssueService.getJiraIssueChangelogGroups(jiraIssue, updated);
        log.trace("Got JiraIssueChangelogGroups {}", jiraIssueChangelogGroups);

        for (var group : jiraIssueChangelogGroups) {
            var itemCount = 0;

            for (var item : group.getItems()) {
                var convertedItem =
                        jiraUtil.convertFieldNameToKey(item, jiraIssue.getNames());
                var rulesByField =
                        ruleService.findRulesByField(filter.getId(), convertedItem.getField(),
                                true, Event.ISSUE_UPDATED);

                for (var rule : rulesByField) {
                    if (ruleUtil.isItemMatchesRule(convertedItem, rule)) {
                        log.debug("Found item {} of jira issue {} "
                                        + "matched rule {} of filter {} [ISSUE_UPDATED]",
                                convertedItem, issueKey, rule.getId(), filter.getName());
                        var fields =
                                jiraUtil.getJiraIssueFieldsCtx(jiraIssue, group.getId());

                        log.trace("Fields for item {} of jira issue {}"
                                        + " matched rule {} [ISSUE_UPDATED]: {}",
                                convertedItem, issueKey, rule.getId(), fields);
                        changelogService.create(new ChangelogCreateDto(
                                Event.ISSUE_UPDATED, group.getId(), itemCount,
                                issueKey.getKey(), rule.getId(), filter.getId(), fields));
                    }
                }
                itemCount++;
            }
        }
    }

    @Override
    public void sendJiraNotification() {
        var changelog = changelogService.findNewChangelog();
        for (var cl : changelog) {
            log.debug("Starting send notifications for changelog {}", cl.getId());
            try {
                notificationService.sendNotification(cl);
                cl.setStatus(Status.SUCCESS);
            } catch (Exception e) {
                log.error("Error occurred while sending notifications for changelog {}: {}",
                        cl.getId(), e.getMessage(), e);
                cl.setStatus(Status.ERROR);
            }
            changelogService.update(cl);
        }
    }

    @Override
    public void digestFilter(long filterId) {
        MDC.put("tid", UUID.randomUUID().toString());
        var sample = Timer.start(meterRegistry);
        try {
            var filter = jiraFilterService.findEnabledSnapshot(filterId);

            if (filter == null) {
                return;
            }
            var result = jiraSearchResultService.fetchSnapshotSearchResult(filter);

            if (result == null || result.getIssues().isEmpty()) {
                log.debug("Snapshot filter {} returned no issues", filter.getName());
                return;
            }
            for (var issueKey : result.getIssues()) {
                digestIssueSafely(filter, issueKey.getKey());
            }
        } catch (Exception e) {
            log.error("Snapshot digest failed for filter {}: {}", filterId, e.getMessage(), e);
        } finally {
            sample.stop(meterRegistry.timer("jena.jira.snapshot"));
            MDC.clear();
        }
    }

    /**
     * Isolates a single issue's digest notification: a failure is logged and does not abort the
     * remaining issues of the run.
     */
    private void digestIssueSafely(JiraFilter filter, String issueKey) {
        try {
            var jiraIssue = jiraIssueService.fetchJiraIssue(issueKey);

            if (jiraIssue == null) {
                log.warn("Jira issue {} not found, skipping", issueKey);
                return;
            }
            notificationService.sendSnapshotNotification(filter, jiraIssue);
        } catch (Exception e) {
            log.error("Snapshot notification failed for issue {} of filter {}: {}",
                    issueKey, filter.getName(), e.getMessage(), e);
        }
    }

    /**
     * Runs a task with the parent thread's MDC restored, clearing it afterwards so pooled worker
     * threads do not leak log context between filters.
     */
    private void runWithMdc(Map<String, String> mdc, Runnable task) {
        if (mdc != null) {
            MDC.setContextMap(mdc);
        }
        try {
            task.run();
        } finally {
            MDC.clear();
        }
    }
}
