package com.itimizer.jena.dto;

/** The recipient address persisted on a notification ({@code chatId}, null for Jira Email). */
public record NotificationRecipientDto(String chatId) {
}