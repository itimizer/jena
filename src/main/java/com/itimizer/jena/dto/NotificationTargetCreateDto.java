package com.itimizer.jena.dto;

import com.itimizer.jena.entity.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body to create a notification target. */
public record NotificationTargetCreateDto(
        @NotBlank @Size(max = ColumnLengths.DEFAULT) String name,
        @NotNull Channel channel,
        @NotBlank @Size(max = ColumnLengths.DEFAULT) String chatId,
        boolean enabled) {
}