package com.itimizer.jena.entity;

/**
 * How a {@link JiraFilter} is polled and what it sends.
 */
public enum FilterMode {
    /** Change-driven: polls an {@code updated >=} window on the global schedule and fires rules. */
    INCREMENTAL,
    /**
     * Scheduled digest: runs the JQL verbatim on a per-filter cron and sends one message per issue.
     */
    SNAPSHOT
}
