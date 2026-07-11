package com.itimizer.jena.service;

import com.itimizer.jena.domain.JiraUser;

/**
 * Resolves the Jira account JENA authenticates as (the PAT/basic-auth identity).
 * It is used to obtain the user's timezone and construct the JQL filters (INCREMENTAL mode).
 */
public interface JiraUserService {

    /** The current authenticated Jira user, e.g. for timezone-aware date handling. */
    JiraUser getJiraUser();
}
