package com.itimizer.jena.notification.impl;

import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.entity.Channel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Sends email via Jira's {@code issue/{key}/notify} endpoint. Has no chat target — recipients
 * (watchers, assignee, etc.) are encoded in the rendered template body.
 */
@Component
@Qualifier("jiraEmailSender")
public class JiraEmailNotificationSender extends AbstractNotificationSender {

    public JiraEmailNotificationSender(@Qualifier("jiraWebClient") WebClient webClient) {
        super(webClient);
    }

    @Override
    public Channel getChannel() {
        return Channel.JIRAEMAIL;
    }

    @Override
    protected String channel() {
        return "Jira Email";
    }

    @Override
    protected String unconfiguredMessage() {
        return "Jira web client is null";
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> buildRequest(WebClient webClient,
                                                           NotificationMessageDto message) {
        var issueKey = message.issueKey();
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/2/issue/{issueKey}/notify")
                        .build(issueKey))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(message.content());
    }
}