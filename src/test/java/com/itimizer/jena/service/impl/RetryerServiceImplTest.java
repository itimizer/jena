package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.entity.Notification;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.repository.NotificationRepository;
import com.itimizer.jena.service.NotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetryerServiceImpl Tests")
class RetryerServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;
    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ApplicationProperties.Retryer retryer;
    private RetryerServiceImpl retryerService;

    private Notification notification1;
    private Notification notification2;
    private Notification notification3;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        retryer = applicationProperties.getRetryer();
        retryerService = new RetryerServiceImpl(applicationProperties,
                notificationRepository, notificationService, meterRegistry);

        notification1 = new Notification();
        notification1.setId(1L);
        notification1.setStatus(Status.ERROR);
        notification1.setFailureCount(1);

        notification2 = new Notification();
        notification2.setId(2L);
        notification2.setStatus(Status.ERROR);
        notification2.setFailureCount(2);

        notification3 = new Notification();
        notification3.setId(3L);
        notification3.setStatus(Status.ERROR);
        notification3.setFailureCount(3);
    }

    @Test
    @DisplayName("should skip retry when disabled")
    void should_skip_retry_when_disabled() {
        retryer.setEnabled(false);

        retryerService.retry();
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("should retry all failed notifications within attempt limit")
    void should_retry_all_failed_notifications_within_attempt_limit() {
        retryer.setEnabled(true);
        retryer.setAttempt(3);
        List<Notification> failedNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                Status.ERROR, 2))
                .thenReturn(failedNotifications);

        retryerService.retry();
        verify(notificationRepository)
                .findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(Status.ERROR, 2);
        verify(notificationService).resendNotification(1L);
        verify(notificationService).resendNotification(2L);
    }

    @Test
    @DisplayName("should retry notification with failure count exactly at limit")
    void should_retry_notification_with_failure_count_exactly_at_limit() {
        retryer.setEnabled(true);
        retryer.setAttempt(2);
        List<Notification> failedNotifications = List.of(notification3);
        when(notificationRepository.findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                Status.ERROR, 1))
                .thenReturn(failedNotifications);

        retryerService.retry();
        verify(notificationRepository)
                .findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(Status.ERROR, 1);
        verify(notificationService).resendNotification(3L);
    }

    @Test
    @DisplayName("should not call resend when no failed notifications found")
    void should_not_call_resend_when_no_failed_notifications_found() {
        retryer.setEnabled(true);
        retryer.setAttempt(3);
        when(notificationRepository.findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                Status.ERROR, 2))
                .thenReturn(Collections.emptyList());

        retryerService.retry();
        verify(notificationRepository)
                .findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(Status.ERROR, 2);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("should retry remaining notifications when one fails")
    void should_retry_remaining_notifications_when_one_fails() {
        retryer.setEnabled(true);
        retryer.setAttempt(3);
        List<Notification> failedNotifications =
                Arrays.asList(notification1, notification2, notification3);
        when(notificationRepository.findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                Status.ERROR, 2))
                .thenReturn(failedNotifications);
        doThrow(new RuntimeException("Resend failed for notification 1"))
                .when(notificationService).resendNotification(1L);

        retryerService.retry();

        verify(notificationService).resendNotification(1L);
        verify(notificationService).resendNotification(2L);
        verify(notificationService).resendNotification(3L);
    }

    @Test
    @DisplayName("should not retry notification when failure count exceeds limit")
    void should_not_retry_notification_when_failure_count_exceeds_limit() {
        retryer.setEnabled(true);
        retryer.setAttempt(2);
        when(notificationRepository.findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                Status.ERROR, 1))
                .thenReturn(Collections.emptyList());

        retryerService.retry();
        verify(notificationRepository)
                .findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(Status.ERROR, 1);
        verify(notificationService, never()).resendNotification(anyLong());
    }
}