package com.itimizer.jena.job;

import com.itimizer.jena.service.RetryerService;
import com.itimizer.jena.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cron entry points for the three global background jobs — incremental poll, dispatch and retry —
 * each driven by its own {@code jena.*} cron property. Every run starts a fresh MDC trace id for
 * log correlation and delegates to the service layer. Per-filter SNAPSHOT digests are scheduled
 * separately by {@link SnapshotScheduleManager}.
 */
@Slf4j
@Component
@AllArgsConstructor
public class Scheduler {

    private final SchedulerService schedulerService;
    private final RetryerService retryerService;

    @Scheduled(cron = "${jena.jira.schedule.cron}", zone = "${jena.jira.schedule.zone}")
    public void fetchJiraNotificationItems() {
        var tid = UUID.randomUUID().toString();
        MDC.put("tid", tid);

        log.info("[Fetch Jira Notification Items] Job started.");
        try {
            schedulerService.findJiraNotificationItems();
            log.info("[Fetch Jira Notification Items] Job completed successfully.");
        } catch (Exception e) {
            log.error("[Fetch Jira Notification Items] Job failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Scheduled(cron = "${jena.notification.schedule.cron}",
            zone = "${jena.notification.schedule.zone}")
    public void sendJiraNotifications() {
        var tid = UUID.randomUUID().toString();
        MDC.put("tid", tid);

        log.info("[Send Jira Notifications] Job started.");
        try {
            schedulerService.sendJiraNotification();
            log.info("[Send Jira Notifications] Job completed successfully.");
        } catch (Exception e) {
            log.error("[Send Jira Notifications] Job failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Scheduled(cron = "${jena.retryer.cron}", zone = "${jena.retryer.zone}")
    public void retry() {
        var tid = UUID.randomUUID().toString();
        MDC.put("tid", tid);

        log.info("[Retry Sending Notification] Job started.");
        try {
            retryerService.retry();
            log.info("[Retry Sending Notification] Job completed successfully.");
        } catch (Exception e) {
            log.error("[Retry Sending Notification] Job failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

}
