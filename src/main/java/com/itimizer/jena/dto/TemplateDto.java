package com.itimizer.jena.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** API representation of a template. */
public record TemplateDto(
        @NotNull Long id,
        @NotNull @Size(max = ColumnLengths.DEFAULT) String name,
        @NotNull TemplateContentDto content) {
}
