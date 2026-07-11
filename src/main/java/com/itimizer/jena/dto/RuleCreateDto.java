package com.itimizer.jena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.validation.JiraFieldRequiredForIssueUpdated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body to create a rule; {@code field} is required for {@code ISSUE_UPDATED}. */
@JiraFieldRequiredForIssueUpdated
public record RuleCreateDto(
        @NotNull Long template,
        boolean enabled,
        @NotNull Event event,
        @Size(max = ColumnLengths.DEFAULT) String field,
        @Size(max = ColumnLengths.DEFAULT) String from,
        @JsonProperty("fromString") @Size(max = ColumnLengths.DEFAULT) String fromStr,
        @Size(max = ColumnLengths.DEFAULT) String to,
        @JsonProperty("toString") @Size(max = ColumnLengths.DEFAULT) String toStr,
        boolean hasChanged) {
}
