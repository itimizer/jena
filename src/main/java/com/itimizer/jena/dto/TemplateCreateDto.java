package com.itimizer.jena.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body to create a template. */
public record TemplateCreateDto(
        @NotNull @Size(max = ColumnLengths.DEFAULT) String name,
        @NotNull TemplateContentDto content) {
}
