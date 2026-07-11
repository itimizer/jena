package com.itimizer.jena.job;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.event.JiraFilterChangedEvent;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Owns the live {@link CronTrigger} registrations for SNAPSHOT filters on the dedicated snapshot
 * {@link TaskScheduler}. Registers all enabled snapshot filters once the context is ready, then
 * keeps the registrations in sync with filter edits via {@link JiraFilterChangedEvent}. All
 * mutation is synchronized and keyed by filter id so a filter has at most one active trigger.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotScheduleManager {

    private final TaskScheduler snapshotTaskScheduler;
    private final SchedulerService schedulerService;
    private final JiraFilterRepository jiraFilterRepository;
    private final ApplicationProperties applicationProperties;

    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /** Registers triggers for every enabled snapshot filter once the application is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        jiraFilterRepository.findByModeAndEnabledTrue(FilterMode.SNAPSHOT).forEach(this::schedule);
    }

    /**
     * Reacts to a committed filter change: re-registers the filter, or cancels its trigger if the
     * filter was deleted. Runs only {@code AFTER_COMMIT} so the trigger never reflects a
     * rolled-back edit.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFilterChanged(JiraFilterChangedEvent event) {
        var filter = jiraFilterRepository.findById(event.filterId()).orElse(null);

        if (filter == null) {
            cancel(event.filterId());
        } else {
            schedule(filter);
        }
    }

    /**
     * (Re)registers a filter's trigger: cancels any existing one first, then schedules a new
     * {@link CronTrigger} only if the filter is enabled, still SNAPSHOT and has a cron. The zone
     * falls back to {@code jena.jira.snapshot.default-zone} when the filter sets none.
     */
    public synchronized void schedule(JiraFilter filter) {
        cancel(filter.getId());

        if (!filter.isEnabled() || filter.getMode() != FilterMode.SNAPSHOT
                || filter.getScheduleCron() == null) {
            return;
        }
        var zoneId = filter.getScheduleZone() != null
                ? filter.getScheduleZone()
                : applicationProperties.getJira().getSnapshot().getDefaultZone();
        var trigger = new CronTrigger(filter.getScheduleCron(), ZoneId.of(zoneId));
        var id = filter.getId();
        var future =
                snapshotTaskScheduler.schedule(() -> schedulerService.digestFilter(id), trigger);

        if (future != null) {
            tasks.put(id, future);
            log.info("Scheduled snapshot filter {} [{}] cron='{}' zone='{}'",
                    filter.getName(), id, filter.getScheduleCron(), zoneId);
        }
    }

    /** Cancels and forgets the filter's trigger if one is registered. */
    public synchronized void cancel(Long id) {
        var future = tasks.remove(id);

        if (future != null) {
            future.cancel(false);
            log.info("Cancelled snapshot schedule for filter {}", id);
        }
    }
}
