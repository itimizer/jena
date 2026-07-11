package com.itimizer.jena.job;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.event.JiraFilterChangedEvent;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotScheduleManager Tests")
class SnapshotScheduleManagerTest {

    @Mock
    private TaskScheduler snapshotTaskScheduler;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private JiraFilterRepository jiraFilterRepository;
    @Mock
    private ScheduledFuture<?> future;

    private SnapshotScheduleManager manager;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setJira(new ApplicationProperties.Jira());
        manager = new SnapshotScheduleManager(
                snapshotTaskScheduler, schedulerService, jiraFilterRepository,
                applicationProperties);
    }

    private JiraFilter snapshotFilter(long id, String cron) {
        JiraFilter filter = new JiraFilter();
        filter.setId(id);
        filter.setName("snap-" + id);
        filter.setMode(FilterMode.SNAPSHOT);
        filter.setEnabled(true);
        filter.setScheduleCron(cron);
        return filter;
    }

    @Test
    @DisplayName("should register a trigger for an enabled snapshot filter with a cron")
    void should_register_trigger() {
        doReturn(future).when(snapshotTaskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        manager.schedule(snapshotFilter(1L, "0 0 8 * * *"));

        verify(snapshotTaskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    @DisplayName("should not schedule an incremental filter")
    void should_not_schedule_incremental() {
        JiraFilter filter = snapshotFilter(1L, "0 0 8 * * *");
        filter.setMode(FilterMode.INCREMENTAL);
        filter.setScheduleCron(null);

        manager.schedule(filter);

        verifyNoInteractions(snapshotTaskScheduler);
    }

    @Test
    @DisplayName("should not schedule a snapshot filter without a cron")
    void should_not_schedule_without_cron() {
        manager.schedule(snapshotFilter(1L, null));

        verifyNoInteractions(snapshotTaskScheduler);
    }

    @Test
    @DisplayName("should not schedule a disabled snapshot filter")
    void should_not_schedule_disabled() {
        JiraFilter filter = snapshotFilter(1L, "0 0 8 * * *");
        filter.setEnabled(false);

        manager.schedule(filter);

        verifyNoInteractions(snapshotTaskScheduler);
    }

    @Test
    @DisplayName("should cancel the previous trigger when rescheduling")
    void should_cancel_previous_on_reschedule() {
        doReturn(future).when(snapshotTaskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        manager.schedule(snapshotFilter(1L, "0 0 8 * * *"));
        manager.schedule(snapshotFilter(1L, "0 0 9 * * *"));

        verify(future).cancel(false);
        verify(snapshotTaskScheduler, times(2))
                .schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    @DisplayName("should cancel a registered trigger")
    void should_cancel_registered_trigger() {
        doReturn(future).when(snapshotTaskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        manager.schedule(snapshotFilter(1L, "0 0 8 * * *"));

        manager.cancel(1L);

        verify(future).cancel(false);
    }

    @Test
    @DisplayName("should reschedule on change event when the filter still exists")
    void should_reschedule_on_change_event() {
        when(jiraFilterRepository.findById(1L))
                .thenReturn(Optional.of(snapshotFilter(1L, "0 0 8 * * *")));
        doReturn(future).when(snapshotTaskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        manager.onFilterChanged(new JiraFilterChangedEvent(1L));

        verify(snapshotTaskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    @DisplayName("should cancel on change event when the filter no longer exists")
    void should_cancel_on_change_event_when_deleted() {
        doReturn(future).when(snapshotTaskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        manager.schedule(snapshotFilter(1L, "0 0 8 * * *"));
        when(jiraFilterRepository.findById(1L)).thenReturn(Optional.empty());

        manager.onFilterChanged(new JiraFilterChangedEvent(1L));

        verify(future).cancel(false);
    }

    @Test
    @DisplayName("should schedule all enabled snapshot filters on bootstrap")
    void should_schedule_all_on_bootstrap() {
        when(jiraFilterRepository.findByModeAndEnabledTrue(FilterMode.SNAPSHOT))
                .thenReturn(List.of(snapshotFilter(1L, "0 0 8 * * *"),
                        snapshotFilter(2L, "0 0 9 * * *")));
        doReturn(future).when(snapshotTaskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        manager.bootstrap();

        verify(snapshotTaskScheduler, times(2))
                .schedule(any(Runnable.class), any(Trigger.class));
    }
}