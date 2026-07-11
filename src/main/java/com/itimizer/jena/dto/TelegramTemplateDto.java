package com.itimizer.jena.dto;

import jakarta.validation.constraints.NotNull;

/** Telegram message body of a template. */
public record TelegramTemplateDto(@NotNull String message) {
}
