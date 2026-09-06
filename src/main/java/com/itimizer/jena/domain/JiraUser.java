package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A Jira user as returned by the API. The {@code timeZone} matters for incremental polling: search
 * results are relative to the authenticated user's zone, so it drives watermark conversion.
 *
 * <p>Which identity fields are populated depends on the deployment. Server/Data Center returns
 * {@code key} and {@code name}; Jira Cloud removed both from every user object and identifies
 * accounts by {@code accountId} instead, so on Cloud those two are always {@code null}.
 * {@code emailAddress} is present on Server/DC but frequently absent on Cloud, where it depends on
 * the account's profile-visibility setting.
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraUser {

    private final String key;
    private final String name;
    private final String accountId;
    private final String emailAddress;
    private final String displayName;
    private final boolean active;
    private final String timeZone;

    /**
     * The identifier this user is referred to by in user field values —
     * {@code key} (Server/DC) falling back to {@code name}, then {@code accountId} (Cloud).
     *
     * @return the changelog-comparable identifier, or {@code null} if the user carries none
     */
    public String getIdentifier() {
        if (key != null) {
            return key;
        }
        return name != null ? name : accountId;
    }
}