package com.itimizer.jena.dto;

import com.itimizer.jena.notification.NotificationSender;

/**
 * A ready-to-send message handed to a
 * {@link NotificationSender}.
 */
public record NotificationMessageDto(
        String issueKey,
        String chatId,
        String content) {
}
