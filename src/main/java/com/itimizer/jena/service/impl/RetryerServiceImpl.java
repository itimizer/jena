package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.repository.NotificationRepository;
import com.itimizer.jena.service.NotificationService;
import com.itimizer.jena.service.RetryerService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link RetryerService} that re-sends notifications left in {@code ERROR} below the attempt limit.
 * Gated by {@code jena.retryer.enabled}; the {@code changelogIsNotNull} filter excludes snapshot
 * (audit-only) notifications. Each attempt increments the {@code jena.retry.attempts} meter.
 */
@Slf4j
@Service
public class RetryerServiceImpl implements RetryerService {

    private final ApplicationProperties.Retryer retryerApplicationProperties;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final MeterRegistry meterRegistry;

    public RetryerServiceImpl(ApplicationProperties applicationProperties,
                              NotificationRepository notificationRepository,
                              NotificationService notificationService,
                              MeterRegistry meterRegistry) {
        this.retryerApplicationProperties = applicationProperties.getRetryer();
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void retry() {
        if (!retryerApplicationProperties.getEnabled()) {
            log.info("Retry notifications skipped due configuration.");
            return;
        }
        int attempt = retryerApplicationProperties.getAttempt();
        var notification = notificationRepository
                .findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                        Status.ERROR, attempt - 1);

        log.debug("Retry notification {}", notification);
        for (var n : notification) {
            try {
                meterRegistry.counter("jena.retry.attempts").increment();
                notificationService.resendNotification(n.getId());
            } catch (Exception e) {
                log.error("Error occurred while retrying notification {}: {}",
                        n.getId(), e.getMessage(), e);
            }
        }
    }
}
