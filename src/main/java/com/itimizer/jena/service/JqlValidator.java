package com.itimizer.jena.service;

import com.itimizer.jena.exception.ValidationException;
import lombok.NonNull;

/**
 * Checks a filter's JQL with Jira before it is persisted. Implementations differ only in the
 * endpoint the targeted deployment offers for this; exactly one is wired in, selected by
 * {@code jena.jira.deployment-type}.
 */
public interface JqlValidator {

    /**
     * Asks Jira to validate the JQL without running it.
     *
     * @param jql the query to check
     * @throws ValidationException if Jira rejects the JQL, or answers in a way that leaves its
     *         validity unknown
     */
    void validate(@NonNull String jql);
}