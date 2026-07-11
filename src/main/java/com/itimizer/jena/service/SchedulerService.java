package com.itimizer.jena.service;

import com.itimizer.jena.entity.Status;

/**
 * Entry points for JENA's scheduled background work. Each method is triggered by a scheduler — the
 * global {@code Scheduler} for the two INCREMENTAL jobs, a per-filter {@code CronTrigger} for
 * digests — and runs to completion on the calling thread.
 */
public interface SchedulerService {

    /**
     * Runs one INCREMENTAL poll cycle: for every enabled INCREMENTAL filter, queries Jira for
     * issues changed since the filter's watermark and records each changelog item that matches an
     * enabled rule. Filters are polled in parallel and isolated from one another, so a single
     * failing filter neither aborts the cycle nor affects the others. Timed under the
     * {@code jena.jira.poll} meter.
     */
    void findJiraNotificationItems();

    /**
     * Dispatches every queued changelog: sends its notification and marks the changelog
     * {@link Status#SUCCESS} or
     * {@link Status#ERROR}. Failed sends stay queued for the
     * retry job.
     */
    void sendJiraNotification();

    /**
     * Runs a single SNAPSHOT filter's digest: executes the filter's JQL verbatim and sends one
     * notification per returned issue, rendered from the filter's own template. Invoked by the
     * filter's own {@code CronTrigger}. Exceptions are logged and swallowed — the next scheduled
     * run is the retry. Timed under the {@code jena.jira.snapshot} meter.
     *
     * @param filterId id of the SNAPSHOT filter to run; the call is a no-op if the filter is
     *                 missing, disabled, or no longer in SNAPSHOT mode
     */
    void digestFilter(long filterId);
}
