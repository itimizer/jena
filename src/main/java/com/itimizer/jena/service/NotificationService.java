package com.itimizer.jena.service;

import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.dto.NotificationDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Notification;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Renders templates into messages and delivers them to the channels configured on a filter's
 * targets. Covers both the INCREMENTAL path (from a matched {@link Changelog}) and the SNAPSHOT
 * path (one message per issue from the filter's own template), plus the API-facing resend, dry-run
 * and context-preview helpers.
 */
public interface NotificationService {

    /**
     * Renders the changelog's matched rule template and sends it to the owning filter's targets,
     * persisting a {@link Notification} per send (INCREMENTAL).
     */
    void sendNotification(@NonNull Changelog changelog);

    /**
     * Renders the SNAPSHOT filter's own template against the issue's current field values and sends
     * one message to the filter's targets. The resulting notification is audit-only — not retried.
     */
    void sendSnapshotNotification(@NonNull JiraFilter filter, @NonNull JiraIssue issue);

    /** Re-attempts delivery of a single previously failed notification. */
    ResponseEntity<String> resendNotification(@NonNull Long notificationId);

    /**
     * Renders the message a rule would produce for an issue on a channel, without sending, queuing,
     * or touching the polling cursor — the engine behind the dry-run endpoint.
     */
    List<NotificationDto> dryRun(@NonNull Long ruleId,
                                 @NonNull String issueKey,
                                 @NonNull Channel channel);

    /**
     * Fields context that would be exposed to a template for an issue, taken from the latest
     * changelog or, when {@code historyId} is given, from that specific history point.
     */
    Map<String, Object> getContext(@NonNull String issueKey, Long historyId);
}
