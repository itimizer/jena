package com.itimizer.jena.event;

/**
 * Published after a filter is created, updated or deleted so the snapshot scheduler can re-sync
 * that filter's cron trigger. Consumed {@code AFTER_COMMIT}.
 *
 * @param filterId id of the affected filter (the row may no longer exist, on delete)
 */
public record JiraFilterChangedEvent(Long filterId) {
}
