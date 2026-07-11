package com.itimizer.jena.dto;

import com.itimizer.jena.entity.FilterMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body to create a filter (SNAPSHOT also sets {@code templateId}/{@code scheduleCron}). */
public record JiraFilterCreateDto(
        @NotBlank @Size(max = ColumnLengths.DEFAULT) String name,
        @NotBlank String jql,
        @NotNull FilterMode mode,
        Long templateId,
        @Size(max = ColumnLengths.DEFAULT) String scheduleCron,
        @Size(max = ColumnLengths.ZONE) String scheduleZone,
        boolean enabled) {
}