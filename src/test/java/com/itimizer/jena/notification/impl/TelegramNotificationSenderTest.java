package com.itimizer.jena.notification.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.dto.NotificationMessageDto;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramSender Tests")
class TelegramNotificationSenderTest {

    private TelegramNotificationSender telegramSender;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                .responseTimeout(Duration.ofMillis(100));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        @SuppressWarnings("unchecked")
        ObjectProvider<WebClient> webClientProvider = mock(ObjectProvider.class);
        when(webClientProvider.getIfAvailable()).thenReturn(webClient);

        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getNotification().getTelegram().setParseMode("HTML");

        telegramSender = new TelegramNotificationSender(applicationProperties, webClientProvider);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("should send notification successfully")
    void should_send_notification_successfully() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"ok\":true}"));

        NotificationMessageDto payload =
                new NotificationMessageDto("PROJ-123", "-123456", "Test message");

        ResponseEntity<String> response = telegramSender.send(payload);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("");

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8())
                .contains("\"chat_id\":\"-123456\"")
                .contains("\"text\":\"Test message\"")
                .contains("\"parse_mode\":\"HTML\"")
                .doesNotContain("\"issueKey\":\"PROJ-123\"");
    }

    @Test
    @DisplayName("should throw exception when message is null")
    @SuppressWarnings("ConstantConditions")
    void should_throw_exception_when_message_is_null() {
        assertThatThrownBy(() -> telegramSender.send(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should handle Bad Request error")
    void should_handle_bad_request_error() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error_code\":400}"));

        NotificationMessageDto payload =
                new NotificationMessageDto("PROJ-123", "123456", "Test message");

        ResponseEntity<String> response = telegramSender.send(payload);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Error calling Telegram API");
    }

    @Test
    @DisplayName("should handle Unauthorized error")
    void should_handle_unauthorized_error() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"ok\":false,\"error_code\":401,\"description\":\"Unauthorized\"}"));

        NotificationMessageDto payload =
                new NotificationMessageDto("PROJ-123", "123456", "Test message");

        ResponseEntity<String> response = telegramSender.send(payload);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Error calling Telegram API");
    }

    @Test
    @DisplayName("should handle Internal Server Error")
    void should_handle_internal_server_error() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error_code\":500}"));

        NotificationMessageDto payload =
                new NotificationMessageDto("PROJ-123", "123456", "Test message");

        ResponseEntity<String> response = telegramSender.send(payload);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Error calling Telegram API");
    }

    @Test
    @DisplayName("should handle network timeout")
    void should_handle_network_timeout() {
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(1, TimeUnit.SECONDS)
                .setBody("{\"ok\":true}")
                .setResponseCode(200));

        NotificationMessageDto payload =
                new NotificationMessageDto("PROJ-123", "123456", "Test message");

        ResponseEntity<String> response = telegramSender.send(payload);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Request timeout");
    }

    @Test
    @DisplayName("should handle network connection error")
    void should_handle_network_connection_error() {
        mockWebServer.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        NotificationMessageDto payload =
                new NotificationMessageDto("PROJ-123", "123456", "Test message");

        ResponseEntity<String> response = telegramSender.send(payload);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Failed to send");
    }
}