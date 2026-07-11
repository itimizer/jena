package com.itimizer.jena.notification.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.entity.Channel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends to Telegram {@code sendMessage} API ({@code chat_id} + {@code text}, optional
 * {@code parse_mode}). Strips Jira html formatting when {@code jena.notification.telegram
 * .remove-jira-formatting} is set.
 */
@Component
@Qualifier("telegramSender")
public class TelegramNotificationSender extends AbstractNotificationSender {

    private final ApplicationProperties.Notification.Telegram telegramProperties;

    public TelegramNotificationSender(
            ApplicationProperties applicationProperties,
            @Qualifier("telegramWebClient") ObjectProvider<WebClient> webClientProvider) {
        super(webClientProvider.getIfAvailable());
        this.telegramProperties = applicationProperties.getNotification().getTelegram();
    }

    @Override
    public Channel getChannel() {
        return Channel.TELEGRAM;
    }

    @Override
    public boolean removeFormatting() {
        return Boolean.TRUE.equals(telegramProperties.getRemoveJiraFormatting());
    }

    @Override
    protected String channel() {
        return "Telegram";
    }

    @Override
    protected String unconfiguredMessage() {
        return "Telegram token is null";
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> buildRequest(WebClient webClient,
                                                           NotificationMessageDto message) {
        Map<String, String> body = new HashMap<>();
        body.put("chat_id", message.chatId());
        body.put("text", message.content());
        if (telegramProperties.getParseMode() != null) {
            body.put("parse_mode", telegramProperties.getParseMode());
        }
        return webClient
                .post()
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}