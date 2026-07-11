package com.itimizer.jena.dto;

import com.itimizer.jena.entity.Event;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** Input for capturing a matched changelog item during polling. */
public record ChangelogCreateDto(
        @NotNull Event event,
        @NotNull Long item,
        @NotNull int itemIndex,
        @NotNull String issueKey,
        @NotNull Long rule,
        @NotNull Long filter,
        @NotNull Map<String, Object> context) {
}
