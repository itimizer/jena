package com.itimizer.jena.notification.impl;

import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.service.JwtService;
import com.itimizer.jena.service.impl.ExpressJwtService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends to Express's API, authenticating each request with a fresh JWT from
 * {@link ExpressJwtService}.
 */
@Component
@Qualifier("expressSender")
public class ExpressNotificationSender extends AbstractNotificationSender {

    private final JwtService jwtService;

    public ExpressNotificationSender(
            @Qualifier("expressJwtService") JwtService jwtService,
            @Qualifier("expressWebClient") ObjectProvider<WebClient> webClientProvider) {
        super(webClientProvider.getIfAvailable());
        this.jwtService = jwtService;
    }

    @Override
    public Channel getChannel() {
        return Channel.EXPRESS;
    }

    @Override
    protected String channel() {
        return "Express";
    }

    @Override
    protected String unconfiguredMessage() {
        return "Express url is null";
    }

    @Override
    protected boolean retryOnUnauthorized() {
        return true;
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> buildRequest(WebClient webClient,
                                                           NotificationMessageDto message) {
        Map<String, String> notificationBody = new HashMap<>();
        notificationBody.put("status", "ok");
        notificationBody.put("body", message.content());

        Map<String, Object> postBody = new HashMap<>();
        postBody.put("group_chat_id", message.chatId());
        postBody.put("notification", notificationBody);

        return webClient
                .post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(postBody);
    }
}