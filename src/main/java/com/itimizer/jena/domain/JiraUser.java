package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A Jira user as returned by the API. The {@code timeZone} matters for incremental polling: search
 * results are relative to the authenticated user's zone, so it drives watermark conversion.
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraUser {

    private final String key;
    private final String name;
    private final String emailAddress;
    private final String displayName;
    private final boolean active;
    private final String timeZone;
}
