package com.itimizer.jena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Lightweight issue reference holding only the key — the shape returned by a {@code fields=key}
 * search before the full issue is fetched.
 */
@Getter
@ToString
@Jacksonized
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueKey {

    private final String key;
}
