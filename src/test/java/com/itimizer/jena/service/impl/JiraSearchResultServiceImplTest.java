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
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.JiraSearchRunRepository;
import com.itimizer.jena.service.JiraSearchResultService;
import com.itimizer.jena.service.JiraUserService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import com.itimizer.jena.util.DateTimeUtil;
import com.itimizer.jena.util.JiraUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
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

    private MockWebServer mockWebServer;
    private WebClient webClient;
    private JiraSearchResultServiceImpl jiraSearchResultService;
    private String issuesSearchResponse;
    private String issuesSearchResponseEmpty;
    private String issuesSearchResponsePage1;
    private String issuesSearchResponsePage2;
    private JiraUser jiraUser;
    private LocalDateTime initialUpdatedAfter;
    private JiraFilter filter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                .responseTimeout(Duration.ofMillis(100));

        webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        issuesSearchResponse = new String(new ClassPathResource("json/search/issues.json")
                .getInputStream().readAllBytes());
        issuesSearchResponseEmpty = new String(
                new ClassPathResource("json/search/issues-empty.json")
                        .getInputStream().readAllBytes());
        issuesSearchResponsePage1 = new String(
                new ClassPathResource("json/search/issues-page1.json")
                        .getInputStream().readAllBytes());
        issuesSearchResponsePage2 = new String(
                new ClassPathResource("json/search/issues-page2.json")
                        .getInputStream().readAllBytes());


        jiraSearchResultService = new JiraSearchResultServiceImpl(
                transactionRunner,
                applicationProperties,
                jiraSearchRunRepositoryMock,
                jiraUserService,
                jiraUtil,
                dateTimeUtil,
                webClient
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
    void tearDown() throws IOException {
        jiraSearchRunRepository.deleteAll();
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("fetchAllJiraSearchResult() method tests")
    class FetchAllJiraSearchResultTests {

        @Test
        @DisplayName("should fetch all pages of search results")
        void should_fetch_all_pages() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated))
                    .thenReturn("project in ('TST') and updated >= '2026-01-01'");

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issuesSearchResponsePage1));

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issuesSearchResponsePage2));

            JiraSearchResult result =
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated);
            assertThat(result).isNotNull();
            assertThat(result.getIssues()).hasSize(2);
            assertThat(result.getIssues()).extracting(JiraIssueKey::getKey)
                    .contains("TST-1", "TST-2");
            assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle single page result")
        void should_handle_single_page() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated))
                    .thenReturn("project in ('TST') and updated >= '2026-01-01'");

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issuesSearchResponse));

            JiraSearchResult result =
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated);
            assertThat(result).isNotNull();
            assertThat(result.getIssues()).hasSize(8);
            assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw exception when first page fails")
        void should_throw_exception_when_first_page_fails() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated))
                    .thenReturn("project in ('TST') and updated >= '2026-01-01'");

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Server Error"));

            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated))
                    .isInstanceOf(HttpServerErrorException.class);
        }

        @Test
        @DisplayName("should handle empty result")
        void should_handle_empty_result() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated))
                    .thenReturn("project in ('TST') and updated >= '2026-01-01'");

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issuesSearchResponseEmpty));

            JiraSearchResult result =
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated);
            assertThat(result).isNotNull();
            assertThat(result.getIssues()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw exception when updated is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_updated_is_null() {
            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception when network timeout")
        void should_throw_exception_when_network_timeout() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated)).thenReturn("jql query");

            mockWebServer.enqueue(new MockResponse()
                    .setBodyDelay(1, TimeUnit.SECONDS)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(issuesSearchResponse)
                    .setResponseCode(200));

            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated))
                    .isInstanceOf(Exception.class)
                    .hasRootCauseInstanceOf(ReadTimeoutException.class);
        }

        @Test
        @DisplayName("should throw exception when network connection error")
        void should_throw_exception_when_network_connection_error() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated)).thenReturn("jql query");
            mockWebServer.enqueue(new MockResponse()
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated))
                    .isInstanceOf(WebClientRequestException.class);
        }

        @Test
        @DisplayName("should throw exception when Bad Request error")
        void should_throw_exception_when_bad_request_error() {
            LocalDateTime updated = LocalDateTime.now();
            when(jiraUtil.getJiraSearchJql(filter.getJql(), updated)).thenReturn("jql query");

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody("{\"errorMessages\":[\"The value 'TST' does not exist.\"]}"));

            assertThatThrownBy(() ->
                    jiraSearchResultService.fetchAllJiraSearchResult(filter, updated))
                    .isInstanceOf(HttpClientErrorException.class);
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
                    webClient
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

            JiraSearchResult jiraSearchResult = new JiraSearchResult(0, 50, 2, issues);

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

            JiraSearchResult jiraSearchResult = new JiraSearchResult(0, 50, 1, issues);
            JiraSearchRun succeeded = searchResultService
                    .saveJiraSearchResult(persistedFilter, jiraSearchResult, runTime);
            assertThat(succeeded.getFailureCount()).isEqualTo(0);
            assertThat(succeeded.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        @DisplayName("should throw exception when runTime is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_run_time_is_null() {
            JiraSearchResult jiraSearchResult = new JiraSearchResult(0, 50, 2, issues);

            assertThatThrownBy(() -> jiraSearchResultService
                        .saveJiraSearchResult(persistedFilter, jiraSearchResult, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}