package com.itimizer.jena.service;

import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.JiraSearchRun;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Runs a filter's JQL search against Jira and maintains its polling cursor ({@link JiraSearchRun}).
 * INCREMENTAL searches add an {@code updated >=} window and advance the watermark; SNAPSHOT
 * searches run the JQL verbatim and keep no cursor.
 */
public interface JiraSearchResultService {

    /**
     * Searches the filter's JQL constrained to {@code updated >= 'updated'} (INCREMENTAL window).
     */
    JiraSearchResult fetchAllJiraSearchResult(@NonNull JiraFilter filter,
                                              @NonNull LocalDateTime updated);

    /** Searches the filter's JQL verbatim, with no time window (SNAPSHOT). */
    JiraSearchResult fetchSnapshotSearchResult(@NonNull JiraFilter filter);

    /**
     * Start of the next incremental window for the filter — its stored watermark, or the configured
     * initial timestamp on first run. {@code null} means the filter has no usable start point yet.
     */
    ZonedDateTime getLastUpdated(@NonNull JiraFilter filter);

    /** Advances the filter's cursor after a poll, recording the query and last-successful times. */
    JiraSearchRun saveJiraSearchResult(@NonNull JiraFilter filter,
                                       JiraSearchResult jiraSearchResult,
                                       @NonNull ZonedDateTime updated);
}
