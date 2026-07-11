package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.TemplateField;
import com.itimizer.jena.dto.NotificationDto;
import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.dto.NotificationRecipientDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Notification;
import com.itimizer.jena.entity.NotificationTarget;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.mapper.TemplateMapper;
import com.itimizer.jena.notification.NotificationSender;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.NotificationRepository;
import com.itimizer.jena.service.JiraIssueService;
import com.itimizer.jena.service.NotificationService;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.service.TemplateService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import com.itimizer.jena.util.JiraUtil;
import com.itimizer.jena.util.RuleUtil;
import com.itimizer.jena.util.StringUtil;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link NotificationService} that renders a template per channel and fans it out to a filter's
 * enabled targets. The injected {@link NotificationSender}s are indexed by {@link Channel} at
 * startup, so a template's per-channel content map drives which senders fire. Every send is
 * persisted as a {@link Notification} and counted on the {@code jena.notifications} meter;
 * INCREMENTAL sends carry their changelog, SNAPSHOT sends do not (audit-only).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final ApplicationProperties applicationProperties;
    private final NotificationRepository notificationRepository;
    private final JiraFilterRepository jiraFilterRepository;
    private final TransactionRunner transactionRunner;
    private final TemplateService templateService;
    private final TemplateMapper templateMapper;
    private final JiraIssueService jiraIssueService;
    private final RuleService ruleService;
    private final StringUtil stringUtil;
    private final RuleUtil ruleUtil;
    private final JiraUtil jiraUtil;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final List<NotificationSender> senders;

    private Map<Channel, NotificationSender> sendersByChannel;

    /**
     * Indexes the injected senders by their channel for O(1) dispatch lookup and enforces that
     * every {@link Channel} has a sender, so lookups never need a null check.
     */
    @PostConstruct
    void initSenders() {
        sendersByChannel = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, Function.identity()));
        var missing = EnumSet.complementOf(sendersByChannel.isEmpty()
                ? EnumSet.noneOf(Channel.class)
                : EnumSet.copyOf(sendersByChannel.keySet()));

        if (!missing.isEmpty()) {
            throw new IllegalStateException("No NotificationSender registered for: " + missing);
        }
    }

    @Override
    public void sendNotification(@NonNull Changelog changelog) {
        log.debug("Starting send notification for changelog: {}", changelog.getId());

        var rule = changelog.getRule();
        var filter = changelog.getFilter();
        var template = rule.getTemplate();

        try {
            var content = templateMapper.convertContentToDto(template.getContent());
            RuntimeException failure = null;

            if (content.telegram() != null) {
                failure = keepFirstFailure(failure, () -> routeToTargets(
                        Channel.TELEGRAM, content.telegram().message(), filter, changelog));
            }
            if (content.jiraEmail() != null) {
                failure = keepFirstFailure(failure, () ->
                        sendJiraEmail(content.jiraEmail().message(), changelog));
            }
            if (content.express() != null) {
                failure = keepFirstFailure(failure, () -> routeToTargets(
                        Channel.EXPRESS, content.express().message(), filter, changelog));
            }
            if (failure != null) {
                throw failure;
            }
        } catch (JacksonException e) {
            log.warn(e.getMessage());
        }
        log.debug("Completed sending notifications for changelog: {}", changelog.getId());
    }

    @Override
    public void sendSnapshotNotification(@NonNull JiraFilter filter, @NonNull JiraIssue issue) {
        log.debug("Starting send snapshot notification for issue {} via filter {}",
                issue.getKey(), filter.getName());
        var content = templateMapper.convertContentToDto(filter.getTemplate().getContent());
        RuntimeException failure = null;

        if (content.telegram() != null) {
            failure = keepFirstFailure(failure, () -> routeIssueToTargets(
                    Channel.TELEGRAM, content.telegram().message(), filter, issue));
        }
        if (content.jiraEmail() != null) {
            failure = keepFirstFailure(failure, () ->
                    sendSnapshotJiraEmail(content.jiraEmail().message(), issue));
        }
        if (content.express() != null) {
            failure = keepFirstFailure(failure, () -> routeIssueToTargets(
                    Channel.EXPRESS, content.express().message(), filter, issue));
        }
        if (failure != null) {
            throw failure;
        }
        log.debug("Completed send snapshot notification for issue {}", issue.getKey());
    }

    /**
     * Runs one channel's send while holding on to an earlier channel's failure, so a failing
     * channel does not suppress the remaining ones. The first failure wins; later ones are
     * attached to it as suppressed exceptions and it is rethrown after all channels have run.
     */
    private RuntimeException keepFirstFailure(RuntimeException previous, Runnable send) {
        try {
            send.run();
            return previous;
        } catch (RuntimeException e) {
            if (previous == null) {
                return e;
            }
            previous.addSuppressed(e);
            return previous;
        }
    }

    /**
     * SNAPSHOT counterpart of {@link #routeToTargets}: renders the issue's current values per
     * target.
     */
    private void routeIssueToTargets(@NonNull Channel channel, String template,
                                     @NonNull JiraFilter filter, @NonNull JiraIssue issue) {
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template cannot be empty.");
        }
        List<NotificationTarget> targets =
                jiraFilterRepository.findEnabledTargets(filter.getId(), channel);

        if (targets.isEmpty()) {
            log.debug("No {} targets for snapshot filter {}", channel, filter.getName());
            return;
        }
        var sender = sendersByChannel.get(channel);
        var content = renderSnapshotContent(channel, template, issue);

        for (var target : targets) {
            var message = new NotificationMessageDto(
                    issue.getKey(), target.getChatId(), content);
            var res = sender.send(message);
            createNotification(issue.getKey(), null, channel,
                    new NotificationRecipientDto(target.getChatId()), content, res);
        }
    }

    private void sendSnapshotJiraEmail(String template, @NonNull JiraIssue issue) {
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template cannot be empty.");
        }
        var content = renderSnapshotContent(Channel.JIRAEMAIL, template, issue);
        var message = new NotificationMessageDto(issue.getKey(), null, content);
        var res = sendersByChannel.get(Channel.JIRAEMAIL).send(message);

        createNotification(issue.getKey(), null, Channel.JIRAEMAIL,
                new NotificationRecipientDto(null), content, res);
    }

    private String renderSnapshotContent(@NonNull Channel channel, @NonNull String template,
                                         @NonNull JiraIssue issue) {
        var context = jiraUtil.getJiraIssueFieldsLatestCtx(issue);
        if (sendersByChannel.get(channel).removeFormatting()) {
            stringUtil.removeFieldHtmlTags(context);
        }
        return templateService.getContent(template, context);
    }

    @Override
    public ResponseEntity<String> resendNotification(@NonNull Long notificationId) {
        log.debug("Starting resend notification: {}", notificationId);

        var notification = notificationRepository.findById(notificationId)
                .orElse(null);

        if (notification == null) {
            throw new ObjectNotFoundException("Notification with id "
                    + notificationId + " not found.");
        }

        var channel = notification.getChannel();
        var message = new NotificationMessageDto(
                notification.getIssueKey(),
                notification.getTarget().chatId(),
                notification.getContent());
        var response = sendersByChannel.get(channel).send(message);

        Status status;
        var failureCount = notification.getFailureCount();

        if (response == null) {
            status = Status.SKIPPED;
        } else if (response.getStatusCode().is2xxSuccessful()) {
            status = Status.SUCCESS;
        } else {
            status = Status.ERROR;
            failureCount++;
        }

        if (notification.getStatus() != status
                || notification.getFailureCount() != failureCount) {
            notification.setStatus(status);
            notification.setFailureCount(failureCount);
            updateNotification(notification);
        }

        log.debug("Completed resending notification: {}", notificationId);
        log.trace("Notification resend response: {}", response);
        return response;
    }

    @Override
    public List<NotificationDto> dryRun(@NonNull Long ruleId,
                                        @NonNull String issueKey,
                                        @NonNull Channel channel) {
        if (issueKey.isEmpty()) {
            throw new ValidationException("Issue key cannot be empty.");
        }

        var jiraIssue = jiraIssueService.fetchJiraIssue(issueKey);

        if (jiraIssue == null) {
            throw new ObjectNotFoundException("Cannot get jira issue with key "
                    + issueKey + ".");
        }
        var rule = ruleService.fetch(ruleId);

        if (rule == null) {
            throw new ObjectNotFoundException("Notification rule with id "
                    + ruleId + " not found.");
        }
        if (!rule.isEnabled()) {
            return new ArrayList<>();
        }

        var template = rule.getTemplate();
        String messageTemplate = null;

        try {
            var templateContentDto =
                    templateMapper.convertContentToDto(template.getContent());
            if (channel == Channel.TELEGRAM && templateContentDto.telegram() != null) {
                messageTemplate = templateContentDto.telegram().message();
            } else if (channel == Channel.JIRAEMAIL && templateContentDto.jiraEmail() != null) {
                messageTemplate = templateContentDto.jiraEmail().message();
            } else if (channel == Channel.EXPRESS && templateContentDto.express() != null) {
                messageTemplate = templateContentDto.express().message();
            }
        } catch (JacksonException e) {
            log.warn(e.getMessage());
        }

        if (messageTemplate == null) {
            throw new ValidationException("Template " + template.getId()
                    + " has no content for channel " + channel + ".");
        }

        List<NotificationDto> notifications = new ArrayList<>();

        if (rule.getEvent() == Event.ISSUE_UPDATED) {

            for (var group : jiraIssue.getChangelog()) {
                var itemCount = 0;
                for (var item : group.getItems()) {
                    var convertedItem =
                            jiraUtil.convertFieldNameToKey(item, jiraIssue.getNames());

                    if (ruleUtil.isItemMatchesRule(convertedItem, rule)) {
                        var fields =
                                jiraUtil.getJiraIssueFieldsCtx(jiraIssue, group.getId());

                        if (shouldRemoveJiraFormatting(channel)) {
                            stringUtil.removeFieldHtmlTags(fields);
                        }
                        var message = templateService
                                .getContent(messageTemplate, fields);

                        notifications.add(new NotificationDto(ruleId, template.getId(), channel,
                                issueKey, group.getId(), itemCount, message));
                    }
                    itemCount++;
                }
            }
        } else if (rule.getEvent() == Event.ISSUE_CREATED) {
            var fields = jiraUtil.getJiraIssueFieldsCtx(jiraIssue, false);

            if (shouldRemoveJiraFormatting(channel)) {
                stringUtil.removeFieldHtmlTags(fields);
            }
            var message = templateService
                    .getContent(messageTemplate, fields);

            notifications.add(new NotificationDto(ruleId, template.getId(), channel,
                    issueKey, 0L, 0, message));
        }
        return notifications;
    }

    @Override
    public Map<String, Object> getContext(@NonNull String issueKey, Long historyId) {
        if (issueKey.isBlank()) {
            throw new ValidationException("Issue key cannot be empty.");
        }
        log.debug("Starting build context for issue {} (historyId: {})", issueKey, historyId);

        var jiraIssue = jiraIssueService.fetchJiraIssue(issueKey);

        if (jiraIssue == null) {
            throw new ObjectNotFoundException("Cannot get jira issue with key "
                    + issueKey + ".");
        }
        var changelog = jiraIssue.getChangelog();

        Map<String, Object> context;

        if (historyId == null) {
            context = changelog.isEmpty()
                    ? jiraUtil.getJiraIssueFieldsCtx(jiraIssue, true)
                    : jiraUtil.getJiraIssueFieldsCtx(jiraIssue, changelog.getLast().getId());
        } else {
            var found = changelog.stream()
                    .anyMatch(group -> group.getId() == historyId);

            if (!found) {
                throw new ObjectNotFoundException("Changelog item with id " + historyId
                        + " not found for issue " + issueKey + ".");
            }
            context = jiraUtil.getJiraIssueFieldsCtx(jiraIssue, historyId);
        }

        log.debug("Completed build context for issue {} (historyId: {})", issueKey, historyId);
        return context;
    }

    private void routeToTargets(@NonNull Channel channel, String template,
                                @NonNull JiraFilter filter, @NonNull Changelog changelog) {
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template cannot be empty.");
        }
        log.debug("Starting routing {} notification for changelog {} via filter {}",
                channel, changelog.getId(), filter.getName());

        List<NotificationTarget> targets =
                jiraFilterRepository.findEnabledTargets(filter.getId(), channel);

        if (targets.isEmpty()) {
            log.debug("No {} targets for filter {}", channel, filter.getName());
            return;
        }
        var sender = sendersByChannel.get(channel);
        var context = buildContext(channel, changelog);
        var content = templateService.getContent(template, context);
        log.trace("{} notification message for changelog {}: {}",
                channel, changelog.getId(), content);

        for (var target : targets) {
            var message = new NotificationMessageDto(
                    changelog.getIssueKey(), target.getChatId(), content);

            var res = sender.send(message);

            createNotification(changelog.getIssueKey(), changelog, channel,
                    new NotificationRecipientDto(target.getChatId()), content, res);
            log.trace("{} notification response for {}: {}",
                    channel, target.getChatId(), res);
        }
        log.debug("Completed routing {} notification for changelog: {}",
                channel, changelog.getId());
    }

    private void sendJiraEmail(String template, @NonNull Changelog changelog) {
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template cannot be empty.");
        }
        log.debug("Starting sending Jira Email notification for changelog: {}", changelog.getId());

        var context = buildContext(Channel.JIRAEMAIL, changelog);
        var content = templateService.getContent(template, context);
        var message = new NotificationMessageDto(changelog.getIssueKey(), null, content);

        var res = sendersByChannel.get(Channel.JIRAEMAIL).send(message);

        createNotification(changelog.getIssueKey(), changelog, Channel.JIRAEMAIL,
                new NotificationRecipientDto(null), content, res);
        log.debug("Completed sending Jira Email notification for changelog: {}",
                changelog.getId());
    }

    /**
     * Field context for an INCREMENTAL send: the snapshot captured on the changelog at match time,
     * with Jira html formatting stripped if the channel's sender requests it.
     */
    private Map<String, Object> buildContext(@NonNull Channel channel,
                                             @NonNull Changelog changelog) {
        Map<String, Object> context = new HashMap<>(changelog.getContext());

        if (sendersByChannel.get(channel).removeFormatting()) {
            removeJiraFormatting(context);
        }
        return context;
    }

    private void removeJiraFormatting(Map<String, Object> context) {
        for (var entry : context.entrySet()) {
            var templateField =
                    objectMapper.convertValue(entry.getValue(), TemplateField.class);
            templateField.setStringValue(
                    stringUtil.removeHtmlTags(templateField.getStringValue()));
            entry.setValue(templateField);
        }
    }

    /**
     * Persists a send outcome and counts it. Maps the send response to a status: {@code null} →
     * {@code SKIPPED} (channel unconfigured), {@code 2xx} → {@code SUCCESS}, anything else →
     * {@code ERROR} (incrementing the failure count so the retry job can pick it up). A
     * {@code null} changelog marks a SNAPSHOT (audit-only) notification, which the retry job
     * excludes.
     */
    private void createNotification(@NonNull String issueKey,
                                    Changelog changelog,
                                    @NonNull Channel channel,
                                    @NonNull NotificationRecipientDto target,
                                    @NonNull String content,
                                    ResponseEntity<String> response) {
        Status status;
        var count = 0;

        if (response == null) {
            status = Status.SKIPPED;
        } else if (response.getStatusCode().is2xxSuccessful()) {
            status = Status.SUCCESS;
        } else {
            status = Status.ERROR;
            count++;
        }
        final var failureCount = count;

        meterRegistry.counter("jena.notifications",
                "channel", channel.name(),
                "status", status.name()).increment();

        transactionRunner.doInTransaction(() -> notificationRepository.save(new Notification(
                issueKey,
                changelog,
                channel,
                target,
                content,
                status,
                failureCount)));
    }

    private boolean shouldRemoveJiraFormatting(@NonNull Channel channel) {
        return channel == Channel.TELEGRAM
                && Boolean.TRUE.equals(applicationProperties.getNotification()
                        .getTelegram().getRemoveJiraFormatting());
    }

    private void updateNotification(@NonNull Notification notification) {
        transactionRunner.doInTransaction(() -> notificationRepository.save(notification));
    }
}
