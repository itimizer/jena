package com.itimizer.jena.domain;

import lombok.Getter;

/**
 * Canonical ids of the system issue fields JENA understands, mapping each enum constant to its Jira
 * field id (e.g. {@code FIX_VERSIONS_FIELD} → {@code "fixVersions"}). Custom fields are not
 * enumerated — they flow through as {@code customfield_*}.
 */
@Getter
public enum JiraIssueFieldId {

    AFFECTS_VERSIONS_FIELD("versions"),
    ASSIGNEE_FIELD("assignee"),
    COMPONENTS_FIELD("components"),
    CREATED_FIELD("created"),
    CREATOR_FIELD("creator"),
    DESCRIPTION_FIELD("description"),
    DUE_DATE_FIELD("duedate"),
    FIX_VERSIONS_FIELD("fixVersions"),
    ISSUE_TYPE_FIELD("issuetype"),
    LABELS_FIELD("labels"),
    PRIORITY_FIELD("priority"),
    PROJECT_FIELD("project"),
    REPORTER_FIELD("reporter"),
    RESOLUTION_FIELD("resolution"),
    RESOLUTIONDATE_FIELD("resolutiondate"),
    STATUS_FIELD("status"),
    SUMMARY_FIELD("summary"),
    ORIGINAL_ESTIMATE_FIELD("timeoriginalestimate"),
    REMAINING_ESTIMATE_FIELD("timeestimate"),
    TIME_SPENT_FIELD("timespent"),
    UPDATED_FIELD("updated");

    public final String id;

    JiraIssueFieldId(String id) {
        this.id = id;
    }
}
