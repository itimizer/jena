package com.itimizer.jena.dto;

import com.itimizer.jena.entity.FilterMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** API representation of a filter, including its attached target and rule ids. */
public record JiraFilterDto(
        @NotNull Long id,
        @NotBlank @Size(max = ColumnLengths.DEFAULT) String name,
        @NotBlank String jql,
        @NotNull FilterMode mode,
        Long templateId,
        @Size(max = ColumnLengths.DEFAULT) String scheduleCron,
        @Size(max = ColumnLengths.ZONE) String scheduleZone,
        boolean enabled,
        List<Long> targetIds,
        List<Long> ruleIds) {
}