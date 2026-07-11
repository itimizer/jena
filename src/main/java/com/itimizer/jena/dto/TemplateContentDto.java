package com.itimizer.jena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A template's per-channel message bodies (the typed view of the {@code content} JSON column). */
public record TemplateContentDto(
        TelegramTemplateDto telegram,
        @JsonProperty("jira-email") JiraEmailTemplateDto jiraEmail,
        ExpressTemplateDto express) {
}
