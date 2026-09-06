package com.itimizer.jena.service.impl;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.domain.JiraIssueKey;
import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.dto.ChangelogDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.JiraSearchRun;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.mapper.JiraIssueDeserializer;
import com.itimizer.jena.service.ChangelogService;
import com.itimizer.jena.service.JiraFilterService;
import com.itimizer.jena.service.JiraIssueService;
import com.itimizer.jena.service.JiraSearchResultService;
import com.itimizer.jena.service.NotificationService;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.util.JiraUtil;
import com.itimizer.jena.util.RuleUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulerServiceImpl Tests")
class SchedulerServiceImplTest {

    @Mock
    private JiraSearchResultService jiraSearchResultService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private JiraIssueService jiraIssueService;
    @Mock
    private ChangelogService changelogService;
    @Mock
    private RuleService ruleService;
    @Mock
    private JiraFilterService jiraFilterService;
    @Mock
    private RuleUtil ruleUtil;
    @Mock
    private JiraUtil jiraUtil;
    @Mock
    private DeserializationContext deserializationContext;

    private ExecutorService jiraPollExecutor;
    private SchedulerServiceImpl schedulerService;

    private ZonedDateTime updated;
    private JiraSearchResult jiraSearchResult;
    private JiraIssue issue;
    private JiraIssueChangelogGroup changelogGroup;
    private JiraIssueChangelogItem changelogItem;
    private Map<String, Object> fields;
    private Rule rule;
    private JiraFilter filter;
    private ChangelogDto changelogDto;

    @BeforeEach
    void setUp() throws IOException {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        jiraPollExecutor = Executors.newSingleThreadExecutor();
        schedulerService = new SchedulerServiceImpl(
                jiraSearchResultService,
                notificationService,
                jiraIssueService,
                changelogService,
                ruleService,
                jiraFilterService,
                ruleUtil,
                jiraUtil,
                meterRegistry,
                jiraPollExecutor
        );

        updated = ZonedDateTime.now().minusHours(1);

        JiraIssueKey jiraIssueKey = JiraIssueKey.builder()
                .key("TST-1")
                .build();

        jiraSearchResult = new JiraSearchResult(List.of(jiraIssueKey));

        JiraIssueDeserializer deserializer = new JiraIssueDeserializer();
        ObjectMapper objectMapper = new ObjectMapper();
        issue = deserializer.deserialize(
                objectMapper
                        .createParser(new ClassPathResource("json/issue/valid-issue.json")
                                .getFile()),
                deserializationContext);

        changelogGroup = issue.getChangelog().getLast();
        changelogItem = changelogGroup.getItems().getFirst();

        fields = new HashMap<>();
        fields.put("summary", "Test Summary");

        Template template = new Template();
        template.setId(1L);
        rule = new Rule();
        rule.setId(1L);
        rule.setTemplate(template);
        rule.setEnabled(true);
        rule.setEvent(Event.ISSUE_CREATED);
        rule.setField("My Number Field");
        rule.setFrom(null);
        rule.setFromStr(null);
        rule.setTo(null);
        rule.setToStr("1.145");
        rule.setHasChanged(false);

        filter = new JiraFilter();
        filter.setId(1L);
        filter.setName("Test Filter");
        filter.setJql("project = TST");
        filter.setEnabled(true);

        lenient().when(ruleService.hasEnabledRules(anyLong())).thenReturn(true);

        changelogDto = new ChangelogDto(
                1L,
                Event.ISSUE_UPDATED,
                10001L,
                0,
                "TST-1",
                1L,
                fields,
                Status.SUCCESS
        );
    }

    @AfterEach
    void tearDown() {
        jiraPollExecutor.shutdownNow();
    }

    @Nested
    @DisplayName("findJiraNotificationItems() method tests")
    class FindJiraNotificationItemsTests {

        @Test
        @DisplayName("should process jira issues and create changelogs for matched rules")
        void should_process_jira_issues_and_create_changelogs_for_matched_rules() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(jiraSearchResult);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(issue);
            when(jiraUtil.isIssueCreatedAfter(eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(false);
            when(jiraIssueService.getJiraIssueChangelogGroups(eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(List.of(changelogGroup));
            when(jiraUtil.convertFieldNameToKey(eq(changelogItem), any()))
                    .thenReturn(changelogItem);
            when(ruleService.findRulesByField(filter.getId(), "My Number Field",
                    true, Event.ISSUE_UPDATED))
                    .thenReturn(List.of(rule));
            when(ruleUtil.isItemMatchesRule(eq(changelogItem), eq(rule)))
                    .thenReturn(true);
            when(jiraUtil.getJiraIssueFieldsCtx(eq(issue), anyLong()))
                    .thenReturn(fields);
            when(changelogService.create(any(ChangelogCreateDto.class)))
                    .thenReturn(changelogDto);

            schedulerService.findJiraNotificationItems();
            verify(jiraSearchResultService).getLastUpdated(filter);
            verify(jiraSearchResultService)
                    .fetchAllJiraSearchResult(eq(filter), any(LocalDateTime.class));
            verify(jiraSearchResultService).saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class));
            verify(jiraIssueService).fetchJiraIssue("TST-1");
            verify(jiraIssueService)
                    .getJiraIssueChangelogGroups(eq(issue), any(ZonedDateTime.class));
            verify(changelogService).create(any(ChangelogCreateDto.class));
        }

        @Test
        @DisplayName("should handle issue created after updated date")
        void should_handle_issue_created_after_updated_date() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(jiraSearchResult);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(issue);
            when(jiraUtil.isIssueCreatedAfter(eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(true);
            when(ruleService.findRulesByEvent(filter.getId(), true, Event.ISSUE_CREATED))
                    .thenReturn(List.of(rule));
            when(jiraUtil.getJiraIssueFieldsCtx(eq(issue), eq(false)))
                    .thenReturn(fields);
            when(changelogService.create(any(ChangelogCreateDto.class)))
                    .thenReturn(changelogDto);

            schedulerService.findJiraNotificationItems();
            verify(ruleService).findRulesByEvent(filter.getId(), true, Event.ISSUE_CREATED);
            ArgumentCaptor<ChangelogCreateDto> dtoCaptor =
                    ArgumentCaptor.forClass(ChangelogCreateDto.class);
            verify(changelogService).create(dtoCaptor.capture());
            assertThat(dtoCaptor.getValue().event()).isEqualTo(Event.ISSUE_CREATED);
            assertThat(dtoCaptor.getValue().filter()).isEqualTo(filter.getId());
        }

        @Test
        @DisplayName("should skip processing when no issues found")
        void should_skip_processing_when_no_issues_found() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            JiraSearchResult emptyResult = new JiraSearchResult(List.of());
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(emptyResult);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            schedulerService.findJiraNotificationItems();
            verify(jiraIssueService, never()).fetchJiraIssue(anyString());
        }

        @Test
        @DisplayName("should skip processing when search result is null")
        void should_skip_processing_when_search_result_is_null() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(null);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter), isNull(),
                    any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            schedulerService.findJiraNotificationItems();
            verify(jiraIssueService, never()).fetchJiraIssue(anyString());
        }

        @Test
        @DisplayName("should skip processing when updated is null")
        void should_skip_processing_when_updated_is_null() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(null);

            schedulerService.findJiraNotificationItems();
            verify(jiraSearchResultService, never())
                    .fetchAllJiraSearchResult(any(JiraFilter.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should handle multiple changelog groups")
        void should_handle_multiple_changelog_groups() {
            JiraIssueChangelogGroup group2 = issue.getChangelog().getFirst();

            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(jiraSearchResult);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(issue);
            when(jiraUtil.isIssueCreatedAfter(eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(false);
            when(jiraIssueService.getJiraIssueChangelogGroups(
                    eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(List.of(changelogGroup, group2));
            when(jiraUtil.convertFieldNameToKey(any(JiraIssueChangelogItem.class), any()))
                    .thenReturn(changelogItem);
            when(ruleService.findRulesByField(anyLong(), anyString(), eq(true),
                    eq(Event.ISSUE_UPDATED)))
                    .thenReturn(List.of(rule));
            when(ruleUtil.isItemMatchesRule(any(JiraIssueChangelogItem.class), any(Rule.class)))
                    .thenReturn(true);
            when(jiraUtil.getJiraIssueFieldsCtx(eq(issue), anyLong()))
                    .thenReturn(fields);
            when(changelogService.create(any(ChangelogCreateDto.class)))
                    .thenReturn(changelogDto);

            schedulerService.findJiraNotificationItems();
            verify(changelogService, times(5)).create(any(ChangelogCreateDto.class));
        }

        @Test
        @DisplayName("should rethrow when listing enabled filters fails")
        void should_rethrow_when_listing_filters_fails() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL))
                    .thenThrow(new RuntimeException("Database connection error"));

            assertThatThrownBy(() -> schedulerService.findJiraNotificationItems())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");
        }

        @Test
        @DisplayName("should save search result only after processing completes")
        void should_save_search_result_only_after_processing_completes() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(jiraSearchResult);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(issue);
            when(jiraUtil.isIssueCreatedAfter(eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(false);
            when(jiraIssueService.getJiraIssueChangelogGroups(eq(issue), any(ZonedDateTime.class)))
                    .thenReturn(List.of(changelogGroup));
            when(jiraUtil.convertFieldNameToKey(eq(changelogItem), any()))
                    .thenReturn(changelogItem);
            when(ruleService.findRulesByField(filter.getId(), "My Number Field",
                    true, Event.ISSUE_UPDATED))
                    .thenReturn(List.of(rule));
            when(ruleUtil.isItemMatchesRule(eq(changelogItem), eq(rule)))
                    .thenReturn(true);
            when(jiraUtil.getJiraIssueFieldsCtx(eq(issue), anyLong()))
                    .thenReturn(fields);
            when(changelogService.create(any(ChangelogCreateDto.class)))
                    .thenReturn(changelogDto);

            schedulerService.findJiraNotificationItems();

            InOrder inOrder = inOrder(changelogService, jiraSearchResultService);
            inOrder.verify(changelogService).create(any(ChangelogCreateDto.class));
            inOrder.verify(jiraSearchResultService).saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("should skip issue when it no longer exists in jira")
        void should_skip_issue_when_it_no_longer_exists_in_jira() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(jiraSearchResult);
            when(jiraSearchResultService.saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());
            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(null);

            schedulerService.findJiraNotificationItems();

            verify(changelogService, never()).create(any(ChangelogCreateDto.class));
            verify(jiraSearchResultService).saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("should record failed run and not advance watermark when processing fails")
        void should_not_advance_watermark_when_processing_fails() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenReturn(jiraSearchResult);
            when(jiraIssueService.fetchJiraIssue("TST-1"))
                    .thenThrow(new RuntimeException("Jira is unavailable"));

            schedulerService.findJiraNotificationItems();

            verify(jiraSearchResultService)
                    .saveJiraSearchResult(eq(filter), isNull(), any(ZonedDateTime.class));
            verify(jiraSearchResultService, never()).saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("should record failed run and not advance watermark when search fetch fails")
        void should_record_failed_run_when_search_fetch_fails() {
            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL)).thenReturn(List.of(filter));
            when(jiraSearchResultService.getLastUpdated(filter)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(filter),
                    any(LocalDateTime.class)))
                    .thenThrow(new RuntimeException("Jira search is unavailable"));

            schedulerService.findJiraNotificationItems();

            verify(jiraIssueService, never()).fetchJiraIssue(anyString());
            verify(jiraSearchResultService)
                    .saveJiraSearchResult(eq(filter), isNull(), any(ZonedDateTime.class));
            verify(jiraSearchResultService, never()).saveJiraSearchResult(eq(filter),
                    any(JiraSearchResult.class), any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("should isolate failures so other filters still poll")
        void should_isolate_failures_across_filters() {
            JiraFilter healthy = new JiraFilter();
            healthy.setId(2L);
            healthy.setName("Healthy Filter");
            healthy.setJql("project = OK");
            healthy.setEnabled(true);

            when(jiraFilterService.findEnabled(FilterMode.INCREMENTAL))
                    .thenReturn(List.of(filter, healthy));

            when(jiraSearchResultService.getLastUpdated(filter))
                    .thenThrow(new RuntimeException("filter A failed"));

            when(jiraSearchResultService.getLastUpdated(healthy)).thenReturn(updated);
            when(jiraSearchResultService.fetchAllJiraSearchResult(eq(healthy),
                    any(LocalDateTime.class)))
                    .thenReturn(new JiraSearchResult(List.of()));
            when(jiraSearchResultService.saveJiraSearchResult(eq(healthy),
                    any(JiraSearchResult.class), any(ZonedDateTime.class)))
                    .thenReturn(new JiraSearchRun());

            schedulerService.findJiraNotificationItems();

            verify(jiraSearchResultService)
                    .saveJiraSearchResult(eq(filter), isNull(), any(ZonedDateTime.class));
            verify(jiraSearchResultService).saveJiraSearchResult(eq(healthy),
                    any(JiraSearchResult.class), any(ZonedDateTime.class));
        }
    }

    @Nested
    @DisplayName("sendJiraNotification() method tests")
    class SendJiraNotificationTests {

        private Changelog changelog1;
        private Changelog changelog2;

        @BeforeEach
        void setUp() {
            changelog1 = new Changelog();
            changelog1.setId(1L);

            changelog2 = new Changelog();
            changelog2.setId(2L);
        }

        @Test
        @DisplayName("should send notifications successfully")
        void should_send_notifications_successfully() {
            List<Changelog> newChangelogs = List.of(changelog1, changelog2);
            when(changelogService.findNewChangelog()).thenReturn(newChangelogs);

            schedulerService.sendJiraNotification();
            verify(changelogService).findNewChangelog();
            verify(notificationService).sendNotification(changelog1);
            verify(notificationService).sendNotification(changelog2);
            verify(changelogService).update(changelog1);
            verify(changelogService).update(changelog2);

            assertThat(changelog1.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(changelog2.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        @DisplayName("should send notification for single changelog")
        void should_send_notification_for_single_changelog() {
            when(changelogService.findNewChangelog()).thenReturn(List.of(changelog1));

            schedulerService.sendJiraNotification();
            verify(notificationService).sendNotification(changelog1);
            verify(changelogService).update(changelog1);
            assertThat(changelog1.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        @DisplayName("should skip when no new changelogs found")
        void should_skip_when_no_new_changelogs_found() {
            when(changelogService.findNewChangelog()).thenReturn(Collections.emptyList());

            schedulerService.sendJiraNotification();
            verify(changelogService).findNewChangelog();
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("should mark changelog as ERROR and not rethrow when sending fails")
        void should_mark_changelog_as_error_when_sending_notification_fails() {
            when(changelogService.findNewChangelog()).thenReturn(List.of(changelog1));
            doThrow(new RuntimeException("Notification send failed"))
                    .when(notificationService).sendNotification(changelog1);

            schedulerService.sendJiraNotification();

            assertThat(changelog1.getStatus()).isEqualTo(Status.ERROR);
            verify(changelogService).update(changelog1);
        }

        @Test
        @DisplayName("should handle exception when finding changelogs fails")
        void should_handle_exception_when_finding_changelogs_fails() {
            when(changelogService.findNewChangelog())
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> schedulerService.sendJiraNotification())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("should handle exception when updating changelog fails")
        void should_handle_exception_when_updating_changelog_fails() {
            when(changelogService.findNewChangelog()).thenReturn(List.of(changelog1));
            doThrow(new RuntimeException("Update failed"))
                    .when(changelogService).update(changelog1);

            assertThatThrownBy(() -> schedulerService.sendJiraNotification())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Update failed");
            verify(notificationService).sendNotification(changelog1);
            assertThat(changelog1.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        @DisplayName("should process multiple changelogs even if one fails")
        void should_process_multiple_changelogs_even_if_one_fails() {
            when(changelogService.findNewChangelog()).thenReturn(List.of(changelog1, changelog2));
            doThrow(new RuntimeException("Send failed for changelog 1"))
                    .when(notificationService).sendNotification(changelog1);

            schedulerService.sendJiraNotification();

            verify(notificationService).sendNotification(changelog1);
            verify(notificationService).sendNotification(changelog2);
            verify(changelogService).update(changelog1);
            verify(changelogService).update(changelog2);
            assertThat(changelog1.getStatus()).isEqualTo(Status.ERROR);
            assertThat(changelog2.getStatus()).isEqualTo(Status.SUCCESS);
        }
    }

    @Nested
    @DisplayName("digestFilter() method tests")
    class DigestFilterTests {

        private JiraFilter snapshotFilter;

        @BeforeEach
        void setUpSnapshot() {
            snapshotFilter = new JiraFilter();
            snapshotFilter.setId(2L);
            snapshotFilter.setName("Snapshot Filter");
            snapshotFilter.setJql("project = TST AND status = Open");
            snapshotFilter.setEnabled(true);
            snapshotFilter.setMode(FilterMode.SNAPSHOT);
        }

        private JiraSearchResult resultWith(String... keys) {
            var issueKeys = new ArrayList<JiraIssueKey>();
            for (var key : keys) {
                issueKeys.add(JiraIssueKey.builder().key(key).build());
            }
            return new JiraSearchResult(issueKeys);
        }

        @Test
        @DisplayName("should send one snapshot notification per returned issue")
        void should_send_one_notification_per_issue() {
            when(jiraFilterService.findEnabledSnapshot(2L)).thenReturn(snapshotFilter);
            when(jiraSearchResultService.fetchSnapshotSearchResult(snapshotFilter))
                    .thenReturn(resultWith("TST-1", "TST-2"));
            when(jiraIssueService.fetchJiraIssue(anyString())).thenReturn(issue);

            schedulerService.digestFilter(2L);

            verify(notificationService, times(2))
                    .sendSnapshotNotification(eq(snapshotFilter), eq(issue));
            verify(jiraSearchResultService, never()).saveJiraSearchResult(any(), any(), any());
            verify(jiraSearchResultService, never()).fetchAllJiraSearchResult(any(), any());
        }

        @Test
        @DisplayName("should skip when snapshot filter not found or disabled")
        void should_skip_when_filter_not_found() {
            when(jiraFilterService.findEnabledSnapshot(99L)).thenReturn(null);

            schedulerService.digestFilter(99L);

            verify(jiraSearchResultService, never()).fetchSnapshotSearchResult(any());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("should skip when no issues are returned")
        void should_skip_when_no_issues() {
            when(jiraFilterService.findEnabledSnapshot(2L)).thenReturn(snapshotFilter);
            when(jiraSearchResultService.fetchSnapshotSearchResult(snapshotFilter))
                    .thenReturn(resultWith());

            schedulerService.digestFilter(2L);

            verify(jiraIssueService, never()).fetchJiraIssue(anyString());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("should skip a missing issue and continue with the rest")
        void should_skip_missing_issue_and_continue() {
            when(jiraFilterService.findEnabledSnapshot(2L)).thenReturn(snapshotFilter);
            when(jiraSearchResultService.fetchSnapshotSearchResult(snapshotFilter))
                    .thenReturn(resultWith("TST-1", "TST-2"));
            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(null);
            when(jiraIssueService.fetchJiraIssue("TST-2")).thenReturn(issue);

            schedulerService.digestFilter(2L);

            verify(notificationService, times(1))
                    .sendSnapshotNotification(eq(snapshotFilter), eq(issue));
        }

        @Test
        @DisplayName("should continue with remaining issues when one send fails")
        void should_continue_when_one_issue_fails() {
            when(jiraFilterService.findEnabledSnapshot(2L)).thenReturn(snapshotFilter);
            when(jiraSearchResultService.fetchSnapshotSearchResult(snapshotFilter))
                    .thenReturn(resultWith("TST-1", "TST-2"));
            when(jiraIssueService.fetchJiraIssue(anyString())).thenReturn(issue);
            doThrow(new RuntimeException("send failed"))
                    .doNothing()
                    .when(notificationService).sendSnapshotNotification(any(), any());

            schedulerService.digestFilter(2L);

            verify(notificationService, times(2))
                    .sendSnapshotNotification(eq(snapshotFilter), eq(issue));
        }

        @Test
        @DisplayName("should not rethrow when sending a snapshot notification fails")
        void should_not_rethrow_when_send_fails() {
            when(jiraFilterService.findEnabledSnapshot(2L)).thenReturn(snapshotFilter);
            when(jiraSearchResultService.fetchSnapshotSearchResult(snapshotFilter))
                    .thenReturn(resultWith("TST-1"));
            when(jiraIssueService.fetchJiraIssue("TST-1")).thenReturn(issue);
            doThrow(new RuntimeException("send failed"))
                    .when(notificationService).sendSnapshotNotification(any(), any());

            schedulerService.digestFilter(2L);

            verify(notificationService).sendSnapshotNotification(eq(snapshotFilter), eq(issue));
        }
    }
}
