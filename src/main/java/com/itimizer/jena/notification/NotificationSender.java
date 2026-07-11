package com.itimizer.jena.notification;

import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.entity.Channel;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;

/**
 * Strategy for delivering a rendered message to one {@link Channel}. Implementations are discovered
 * by Spring and indexed by {@link #getChannel()} so a template's per-channel content fans out to
 * the right sender.
 */
public interface NotificationSender {

    /**
     * Identifies which channel this sender delivers to.
     *
     * @return the channel this sender handles
     */
    Channel getChannel();

    /**
     * Delivers the message. Returns the upstream response, or {@code null} when the channel is not
     * configured (treated as {@code SKIPPED}); a non-2xx response signals a failure to record.
     *
     * @param message the rendered message to deliver
     * @return the upstream response, or {@code null} when the channel is unconfigured
     */
    ResponseEntity<String> send(@NonNull NotificationMessageDto message);

    /**
     * Indicates whether Jira html formatting should be stripped before sending (e.g. Telegram).
     *
     * @return {@code true} if formatting should be stripped, {@code false} to send as-is
     */
    default boolean removeFormatting() {
        return false;
    }
}
