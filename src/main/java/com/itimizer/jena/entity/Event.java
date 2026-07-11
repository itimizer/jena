package com.itimizer.jena.entity;

/**
 * The trigger condition a {@link Rule} watches for (INCREMENTAL mode).
 */
public enum Event {
    /** A newly created issue matched the filter. */
    ISSUE_CREATED,
    /** A field changed on a matched issue. */
    ISSUE_UPDATED
}
