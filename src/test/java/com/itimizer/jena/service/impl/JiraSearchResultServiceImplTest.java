package com.itimizer.jena.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.domain.JiraIssueKey;
import com.itimizer.jena.domain.JiraSearchResult;
import com.itimizer.jena.domain.JiraUser;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.JiraSearchRun;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.exception.RequestFailedException;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.JiraSearchRunRepository;
import com.itimizer.jena.service.JiraSearchClient;
import com.itimizer.jena.service.JiraSearchResultService;
import com.itimizer.jena.service.JiraUserService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import com.itimizer.jena.util.DateTimeUtil;
import com.itimizer.jena.util.JiraUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@ExtendWith(MockitoExtension.class)
@DisplayName("JiraSearchResultService Integration Tests")
class JiraSearchResultServiceImplTest {

    @Autowired
    private JiraSearchRunRepository jiraSearchRunRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;
    @Autowired
    private TransactionRunner transactionRunner;
    @Autowired
    private DateTimeUtil dateTimeUtil;

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Jira jiraProperties;
    @Mock
    private JiraSearchRunRepository jiraSearchRunRepositoryMock;
    @Mock
    private JiraUserService jiraUserService;
    @Mock
    private JiraUtil jiraUtil;
    @Mock
    private JiraSearchClient jiraSearchClient;

    private JiraSearchResultServiceImpl jiraSearchResultService;
    private JiraUser jiraUser;
    private LocalDateTime initialUpdatedAfter;
    private JiraFilter filter;

    @BeforeEach
    void setUp() throws IOException {
        jiraSearchResultService = new JiraSearchResultServiceImpl(
                transactionRunner,
                applicationProperties,
                jiraSearchRunRepositoryMock,
                jiraUserService,
                jiraUtil,
                dateTimeUtil,
                jiraSearchClient
        );

        ObjectMapper objectMapper = new ObjectMapper();
        String jiraUserJson = new String(
                new ClassPathResource("json/user/valid-user.json")
                        .getInputStream().readAllBytes());
        jiraUser = objectMapper.readValue(jiraUserJson, JiraUser.class);
        initialUpdatedAfter = LocalDateTime.of(2026, 1, 1, 0, 0);

        filter = new JiraFilter();
        filter.setId(1L);
        filter.setName("Test Filter");
        filter.setJql("project = TST");
        filter.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        jiraSearchRunRepository.deleteAll();
    }

    @Nested
    @DisplayName("search delegation tests")
    class SearchDelegationTests {

        @Test
        @DisplayName("should search the windowed JQL and return what the client found")
        void should_search_windowed_jql() {
            LocalDateTime updated = LocalDateTime.now();
            String windowedJql = "(project = TST) and updated >= '2026-01-01 00:00'";
            JiraSearchResult expected = new JiraSearchResult(
                    List.of(JiraIssueKey.builder().key("TST-1").build()));
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated)).thenReturn(windowedJql);
            when(jiraSearchClient.searchAllKeys(windowedJql, filter.getName()))
                    .thenReturn(expected);

            JiraSearchResult result =
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated);
            assertThat(result).isSameAs(expected);
            verify(jiraSearchClient).searchAllKeys(windowedJql, filter.getName());
        }

        @Test
        @DisplayName("should search the raw filter JQL for a snapshot")
        void should_search_raw_jql_for_snapshot() {
            JiraSearchResult expected = new JiraSearchResult(
                    List.of(JiraIssueKey.builder().key("TST-1").build()));
            when(jiraSearchClient.searchAllKeys(filter.getJql(), filter.getName()))
                    .thenReturn(expected);

            JiraSearchResult result = jiraSearchResultService.fetchSnapshotSearchResult(filter);
            assertThat(result).isSameAs(expected);
            verify(jiraSearchClient).searchAllKeys(filter.getJql(), filter.getName());
        }

        @Test
        @DisplayName("should return null when the client returns null")
        void should_return_null_when_client_returns_null() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated)).thenReturn("jql query");
            when(jiraSearchClient.searchAllKeys("jql query", filter.getName())).thenReturn(null);

            assertThat(jiraSearchResultService.fetchAllJiraSearchResult(filter, updated)).isNull();
        }

        @Test
        @DisplayName("should propagate a client failure")
        void should_propagate_client_failure() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated)).thenReturn("jql query");
            when(jiraSearchClient.searchAllKeys("jql query", filter.getName()))
                    .thenThrow(new RequestFailedException("page failed"));

            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated))
                    .isInstanceOf(RequestFailedException.class);
        }

        @Test
        @DisplayName("should throw exception when updated is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_updated_is_null() {
            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("getLastUpdated() method tests")
    class GetLastUpdatedTests {

        @Test
        @DisplayName("should return last success run time from database")
        void should_return_last_success_run_time() {
            when(jiraUserService.getJiraUser()).thenReturn(jiraUser);
            JiraSearchRun searchRun = new JiraSearchRun();
            LocalDateTime lastSuccessRun = LocalDateTime.of(2026, 1, 1, 0, 0);
            searchRun.setLastSuccessRunTime(lastSuccessRun);
            when(jiraSearchRunRepositoryMock.findByFilterId(filter.getId())).thenReturn(searchRun);

            ZonedDateTime expectedZonedDateTime =
                    ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("Antarctica/Troll"));

            ZonedDateTime result = jiraSearchResultService.getLastUpdated(filter);
            assertThat(result).isEqualTo(expectedZonedDateTime);
        }

        @Test
        @DisplayName("should return initial updated after from config when no run exists")
        void should_return_initial_updated_after_from_config() {
            when(applicationProperties.getJira()).thenReturn(jiraProperties);
            when(jiraUserService.getJiraUser()).thenReturn(jiraUser);
            when(jiraSearchRunRepositoryMock.findByFilterId(filter.getId())).thenReturn(null);
            when(jiraProperties.getInitialUpdatedAfter()).thenReturn(initialUpdatedAfter);

            ZonedDateTime expectedZonedDateTime =
                    ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("Antarctica/Troll"));

            ZonedDateTime result = jiraSearchResultService.getLastUpdated(filter);
            assertThat(result).isEqualTo(expectedZonedDateTime);
        }

        @Test
        @DisplayName("should return current time when no run exists and no initial config")
        void should_return_current_time_when_no_config() {
            when(applicationProperties.getJira()).thenReturn(jiraProperties);
            when(jiraUserService.getJiraUser()).thenReturn(jiraUser);
            when(jiraSearchRunRepositoryMock.findByFilterId(filter.getId())).thenReturn(null);
            when(jiraProperties.getInitialUpdatedAfter()).thenReturn(null);

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Antarctica/Troll"));

            ZonedDateTime result = jiraSearchResultService.getLastUpdated(filter);
            assertThat(result).isCloseTo(now, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("should use system default timezone when jira user is null")
        void should_use_system_default_timezone_when_jira_user_is_null() {
            when(applicationProperties.getJira()).thenReturn(jiraProperties);
            when(jiraUserService.getJiraUser()).thenReturn(null);
            when(jiraSearchRunRepositoryMock.findByFilterId(filter.getId())).thenReturn(null);
            when(jiraProperties.getInitialUpdatedAfter()).thenReturn(initialUpdatedAfter);

            ZonedDateTime expectedZonedDateTime =
                    ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());

            ZonedDateTime result = jiraSearchResultService.getLastUpdated(filter);
            assertThat(result).isEqualTo(expectedZonedDateTime);
        }
    }

    @Nested
    @DisplayName("saveJiraSearchResult() method tests")
    class SaveJiraSearchResultTests {

        private JiraSearchResultService searchResultService;
        private List<JiraIssueKey> issues;
        private ZonedDateTime runTime;
        private LocalDateTime utcRunTime;
        private JiraFilter persistedFilter;

        @BeforeEach
        void setUp() {
            searchResultService = new JiraSearchResultServiceImpl(
                    transactionRunner,
                    applicationProperties,
                    jiraSearchRunRepository,
                    jiraUserService,
                    jiraUtil,
                    dateTimeUtil,
                    jiraSearchClient
            );

            issues = List.of(
                    JiraIssueKey.builder()
                            .key("TST-1")
                            .build()
            );

            runTime = ZonedDateTime.now();
            utcRunTime = runTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

            JiraFilter f = new JiraFilter();
            f.setName("Save Filter");
            f.setJql("project = TST");
            f.setEnabled(true);
            persistedFilter = jiraFilterRepository.save(f);
        }

        @AfterEach
        void tearDown() {
            jiraSearchRunRepository.deleteAll();
            jiraFilterRepository.deleteAll();
        }

        @Test
        @DisplayName("should save new search run")
        void should_save_new_search_run() {
            lenient().when(applicationProperties.getJira()).thenReturn(jiraProperties);
            lenient().when(jiraProperties.getUrl()).thenReturn("https://jira.example.com");

            JiraSearchResult jiraSearchResult = new JiraSearchResult(issues);

            JiraSearchRun result = searchResultService
                    .saveJiraSearchResult(persistedFilter, jiraSearchResult, runTime);
            assertThat(result).isNotNull();
            assertThat(result.getFilter().getId()).isEqualTo(persistedFilter.getId());
            assertThat(result.getLastSuccessRunTime()).isEqualTo(utcRunTime);
            assertThat(result.getLastRunTime()).isEqualTo(utcRunTime);
            assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(result.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should save new search run when JiraSearchResult is null")
        void should_save_new_search_run_when_jira_search_result_is_null() {
            lenient().when(applicationProperties.getJira()).thenReturn(jiraProperties);
            lenient().when(jiraProperties.getUrl()).thenReturn("https://jira.example.com");

            JiraSearchRun result =
                    searchResultService.saveJiraSearchResult(persistedFilter, null, runTime);
            assertThat(result).isNotNull();
            assertThat(result.getLastSuccessRunTime()).isNull();
            assertThat(result.getLastRunTime()).isEqualTo(utcRunTime);
            assertThat(result.getStatus()).isEqualTo(Status.ERROR);
            assertThat(result.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment failure count on failed runs and reset on success")
        void should_increment_failure_count_and_reset_on_success() {
            lenient().when(applicationProperties.getJira()).thenReturn(jiraProperties);
            lenient().when(jiraProperties.getUrl()).thenReturn("https://jira.example.com");

            searchResultService.saveJiraSearchResult(persistedFilter, null, runTime);
            JiraSearchRun failedTwice =
                    searchResultService.saveJiraSearchResult(persistedFilter, null, runTime);
            assertThat(failedTwice.getFailureCount()).isEqualTo(2);

            JiraSearchResult jiraSearchResult = new JiraSearchResult(issues);
            JiraSearchRun succeeded = searchResultService
                    .saveJiraSearchResult(persistedFilter, jiraSearchResult, runTime);
            assertThat(succeeded.getFailureCount()).isEqualTo(0);
            assertThat(succeeded.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        @DisplayName("should throw exception when runTime is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_run_time_is_null() {
            JiraSearchResult jiraSearchResult = new JiraSearchResult(issues);

            assertThatThrownBy(() -> jiraSearchResultService
                        .saveJiraSearchResult(persistedFilter, jiraSearchResult, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}