package com.itimizer.jena.dto;

import jakarta.validation.constraints.NotNull;

/** Jira Email message body of a template. */
public record JiraEmailTemplateDto(@NotNull String message) {
}
