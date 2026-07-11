package com.itimizer.jena.dto;

import com.itimizer.jena.entity.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** API representation of a notification target. */
public record NotificationTargetDto(
        @NotNull Long id,
        @NotBlank @Size(max = ColumnLengths.DEFAULT) String name,
        @NotNull Channel channel,
        @NotBlank @Size(max = ColumnLengths.DEFAULT) String chatId,
        boolean enabled) {
}