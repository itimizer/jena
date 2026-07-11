package com.itimizer.jena.service;

/**
 * The retry job: re-dispatches notifications whose send failed, up to the configured attempt limit.
 * Snapshot (audit-only) notifications are excluded — their next digest run is the retry.
 */
public interface RetryerService {

    /** Re-attempts every eligible failed notification once. */
    void retry();
}
