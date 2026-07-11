package com.itimizer.jena.dto;

import jakarta.validation.constraints.NotNull;

/** Express message body of a template. */
public record ExpressTemplateDto(@NotNull String message) {
}
