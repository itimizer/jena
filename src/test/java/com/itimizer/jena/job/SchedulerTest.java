package com.itimizer.jena.job;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.service.RetryerService;
import com.itimizer.jena.service.SchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {ContainersConfig.class})
@TestPropertySource(properties = {
    "jena.jira.schedule.cron=*/5 * * * * *",
    "jena.jira.schedule.zone=UTC",
    "jena.notification.schedule.cron=*/5 * * * * *",
    "jena.notification.schedule.zone=UTC",
    "jena.retryer.cron=*/5 * * * * *",
    "jena.retryer.zone=UTC",
    "jena.retryer.enabled=true"
})
@DisplayName("Scheduler Tests")
class SchedulerTest {

    @MockitoBean
    private SchedulerService schedulerService;
    @MockitoBean
    private RetryerService retryerService;

    @Nested
    @DisplayName("fetchJiraNotificationItems() method tests")
    class FetchJiraNotificationItemsTests {

        @Test
        @DisplayName("should execute fetchJiraNotificationItems on schedule")
        void should_execute_fetchJiraNotificationItems_on_schedule() {
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            verify(schedulerService, atLeast(1)).findJiraNotificationItems()
            );
        }
    }

    @Nested
    @DisplayName("sendJiraNotifications() method tests")
    class SendJiraNotificationsTests {

        @Test
        @DisplayName("should execute sendJiraNotifications on schedule")
        void should_execute_sendJiraNotifications_on_schedule() {
            await().atMost(15, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            verify(schedulerService, atLeast(1)).sendJiraNotification()
            );
        }
    }

    @Nested
    @DisplayName("retry() method tests")
    class RetryTests {

        @Test
        @DisplayName("should execute retry on schedule")
        void should_execute_retry_on_schedule() {
            await().atMost(20, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            verify(retryerService, atLeast(1)).retry()
            );
        }
    }
}