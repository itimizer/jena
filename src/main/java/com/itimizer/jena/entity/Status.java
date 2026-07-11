package com.itimizer.jena.entity;

/**
 * Processing/delivery outcome shared by notifications, changelog rows and search runs.
 */
public enum Status {
    /** Delivered (or processed) successfully. */
    SUCCESS,
    /** Delivery failed; eligible for retry. */
    ERROR,
    /** Nothing was sent (e.g. no sender configured for the channel). */
    SKIPPED
}
