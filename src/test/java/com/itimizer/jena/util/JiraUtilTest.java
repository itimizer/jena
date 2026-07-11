package com.itimizer.jena.util;

import com.itimizer.jena.exception.ValidationException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.domain.TemplateField;
import com.itimizer.jena.mapper.JiraIssueDeserializer;
import com.itimizer.jena.mapper.field.FieldMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraUtil Tests")
class JiraUtilTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private DeserializationContext deserializationContext;

    @InjectMocks
    private JiraUtil jiraUtil;

    private JiraIssue issue;

    @BeforeEach
    void setUp() throws IOException {
        JiraIssueDeserializer deserializer = new JiraIssueDeserializer();
        ObjectMapper objectMapper = new ObjectMapper();
        issue = deserializer.deserialize(
                objectMapper
                        .createParser(new ClassPathResource("json/issue/valid-issue.json")
                                .getFile()),
                deserializationContext);
        StringUtil stringUtil = new StringUtil();
        FieldMapper fieldMapper = new FieldMapper(applicationProperties);
        jiraUtil = new JiraUtil(applicationProperties, stringUtil, fieldMapper);

    }

    @Nested
    @DisplayName("getJiraSearchJql() method tests")
    class GetJiraSearchJqlTests {

        @Test
        @DisplayName("should append the time window to the filter base JQL")
        void should_generate_valid_jql_search_string() {
            LocalDateTime updated = LocalDateTime.of(2026, 1, 1, 0, 0);

            String result = jiraUtil.getJiraSearchJql(
                    "project in ('TST','PROJ','DEV') and issuetype in ('Bug','Task','Story')",
                    updated);
            assertThat(result).isEqualTo("(project in ('TST','PROJ','DEV') and "
                    + "issuetype in ('Bug','Task','Story')) and updated >= '2026-01-01 00:00'");
        }

        @Test
        @DisplayName("should generate empty JQL search string when updated is null")
        void should_generate_empty_jql_search_string_when_updated_is_null() {
            String result = jiraUtil.getJiraSearchJql("project = TST", null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reject a blank base JQL to avoid an unbounded search")
        void should_reject_blank_base_jql() {
            LocalDateTime updated = LocalDateTime.of(2026, 1, 1, 0, 0);

            assertThatThrownBy(() -> jiraUtil.getJiraSearchJql("  ", updated))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("getJiraIssueChangelogGroups() method tests")
    class GetJiraIssueChangelogGroupsTests {

        @Test
        @DisplayName("should return changelog groups created after specified date")
        void should_return_changelog_groups_after_specified_date() {
            ZonedDateTime cutoffDate = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));

            List<JiraIssueChangelogGroup> result =
                    jiraUtil.getJiraIssueChangelogGroups(issue, cutoffDate);
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(group -> group.getCreated().isAfter(cutoffDate));
        }

        @Test
        @DisplayName("should return empty list when no changelog after date")
        void should_return_empty_list_when_no_changelog_after_date() {
            ZonedDateTime cutoffDate = ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

            List<JiraIssueChangelogGroup> result =
                    jiraUtil.getJiraIssueChangelogGroups(issue, cutoffDate);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when issue is null")
        void should_return_empty_list_when_issue_is_null() {
            ZonedDateTime cutoffDate = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

            List<JiraIssueChangelogGroup> result =
                    jiraUtil.getJiraIssueChangelogGroups(null, cutoffDate);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getJiraIssueFieldsCtx(JiraIssue, boolean) method tests")
    class GetJiraIssueFieldsCtxBooleanTests {

        @Test
        @DisplayName("should return map with all fields")
        void should_return_map_with_all_fields() {
            ApplicationProperties.Jira jiraConfig = new ApplicationProperties.Jira();
            jiraConfig.setNames(Map.of("fixVersions", "Fix Version",
                    "versions", "Version",
                    "components", "Component"));
            when(applicationProperties.getJira()).thenReturn(jiraConfig);

            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(issue, true);
            assertThat(result).containsKeys("baseUrl", "key", "status", "issuetype", "project",
                    "summary", "description", "creator", "reporter", "assignee", "resolution",
                    "created", "updated", "resolutiondate", "duedate", "priority", "components",
                    "fixVersions", "versions", "timeoriginalestimate", "timeestimate", "timespent",
                    "labels", "customfield_10000", "customfield_10001", "customfield_10002");
        }

        @Test
        @DisplayName("should return empty creator/reporter fields when they are null")
        void should_return_empty_creator_reporter_when_null() {
            ApplicationProperties.Jira jiraConfig = new ApplicationProperties.Jira();
            jiraConfig.setNames(Map.of("fixVersions", "Fix Version",
                    "versions", "Version",
                    "components", "Component"));
            when(applicationProperties.getJira()).thenReturn(jiraConfig);

            JiraIssue issueWithoutPeople = issueWithNullCreatorAndReporter();

            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(issueWithoutPeople, true);

            TemplateField creatorField = (TemplateField) result.get("creator");
            assertThat(creatorField).isNotNull();
            assertThat(creatorField.getValue()).isNull();
            assertThat(creatorField.getStringValue()).isNull();

            TemplateField reporterField = (TemplateField) result.get("reporter");
            assertThat(reporterField).isNotNull();
            assertThat(reporterField.getValue()).isNull();
            assertThat(reporterField.getStringValue()).isNull();
        }

        @Test
        @DisplayName("should return latest issue values")
        void should_return_latest_issue_values() {
            ApplicationProperties.Jira jiraConfig = new ApplicationProperties.Jira();
            jiraConfig.setNames(Map.of("fixVersions", "Fix Version",
                    "versions", "Version",
                    "components", "Component"));
            when(applicationProperties.getJira()).thenReturn(jiraConfig);

            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(issue, true);

            TemplateField keyField = (TemplateField) result.get("key");
            assertThat(keyField.getValue()).isEqualTo("10000");
            assertThat(keyField.getStringValue()).isEqualTo("TST-1");

            TemplateField statusField = (TemplateField) result.get("status");
            assertThat(statusField.getValue()).isEqualTo("3");
            assertThat(statusField.getStringValue()).isEqualTo("In Progress");

            TemplateField issueTypeField = (TemplateField) result.get("issuetype");
            assertThat(issueTypeField.getValue()).isEqualTo("1");
            assertThat(issueTypeField.getStringValue()).isEqualTo("Bug");

            TemplateField priorityField = (TemplateField) result.get("priority");
            assertThat(priorityField.getValue()).isEqualTo("3");
            assertThat(priorityField.getStringValue()).isEqualTo("Major");

            TemplateField componentsField = (TemplateField) result.get("components");
            assertThat(componentsField.getValue()).contains("10000", "10001");
            assertThat(componentsField.getStringValue()).contains("Component A", "Component B");

            TemplateField fixVersionsField = (TemplateField) result.get("fixVersions");
            assertThat(fixVersionsField.getValue()).contains("10000");
            assertThat(fixVersionsField.getStringValue()).contains("1.1");

            TemplateField versionsField = (TemplateField) result.get("versions");
            assertThat(versionsField.getValue()).contains("10000", "10001");
            assertThat(versionsField.getStringValue()).contains("1", "1.1");

            TemplateField creatorField = (TemplateField) result.get("creator");
            assertThat(creatorField.getValue()).isEqualTo("admin");
            assertThat(creatorField.getStringValue()).isEqualTo("Administrator");

            TemplateField reporterField = (TemplateField) result.get("reporter");
            assertThat(reporterField.getValue()).isEqualTo("admin");
            assertThat(reporterField.getStringValue()).isEqualTo("Administrator");

            TemplateField assigneeField = (TemplateField) result.get("assignee");
            assertThat(assigneeField.getValue()).isEqualTo("admin");
            assertThat(assigneeField.getStringValue()).isEqualTo("Administrator");

            TemplateField labelsField = (TemplateField) result.get("labels");
            assertThat(labelsField.getValue()).isNull();
            assertThat(labelsField.getStringValue()).contains("bug", "critical");

            TemplateField createdField = (TemplateField) result.get("created");
            assertThat(createdField.getValue()).contains("2026-01-01T00:00:00+0200");
            assertThat(createdField.getStringValue()).contains("01.01.2026");

            TemplateField dueDateField = (TemplateField) result.get("duedate");
            assertThat(dueDateField.getValue()).isEqualTo("2026-12-31");
            assertThat(dueDateField.getStringValue()).isEqualTo("2026-12-31 00:00:00.0");

            TemplateField resolutionDateField = (TemplateField) result.get("resolutiondate");
            assertThat(resolutionDateField.getValue()).isEqualTo("2026-12-31T23:59:59+0200");
            assertThat(resolutionDateField.getStringValue()).isEqualTo("31.12.2026 23:59");

            TemplateField timeOriginalEstimateField =
                    (TemplateField) result.get("timeoriginalestimate");
            assertThat(timeOriginalEstimateField.getValue()).isEqualTo("25200");
            assertThat(timeOriginalEstimateField.getStringValue()).isEqualTo("25200");

            TemplateField timeEstimateField = (TemplateField) result.get("timeestimate");
            assertThat(timeEstimateField.getValue()).isEqualTo("21600");
            assertThat(timeEstimateField.getStringValue()).isEqualTo("21600");

            TemplateField timeSpentField = (TemplateField) result.get("timespent");
            assertThat(timeSpentField.getValue()).isEqualTo("8700");
            assertThat(timeSpentField.getStringValue()).isEqualTo("8700");

            TemplateField customfield10000 = (TemplateField) result.get("customfield_10000");
            assertThat(customfield10000.getValue()).isNull();
            assertThat(customfield10000.getStringValue()).isEqualTo("1.457");

            TemplateField customfield10001 = (TemplateField) result.get("customfield_10001");
            assertThat(customfield10001.getValue()).isEqualTo("10001");
            assertThat(customfield10001.getStringValue()).isEqualTo("Another");

            TemplateField customfield10002 = (TemplateField) result.get("customfield_10002");
            assertThat(customfield10002.getValue()).isNull();
            assertThat(customfield10002.getStringValue()).isNull();

        }

        @Test
        @DisplayName("should return earliest issue values")
        void should_return_earliest_issue_values() {
            ApplicationProperties.Jira jiraConfig = new ApplicationProperties.Jira();
            jiraConfig.setNames(Map.of("fixVersions", "Fix Version",
                    "versions", "Version",
                    "components", "Component"));
            when(applicationProperties.getJira()).thenReturn(jiraConfig);

            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(issue, false);

            TemplateField componentsField = (TemplateField) result.get("components");
            assertThat(componentsField.getValue()).isEqualTo("10000");
            assertThat(componentsField.getStringValue()).isEqualTo("Component A");

            TemplateField fixVersionsField = (TemplateField) result.get("fixVersions");
            assertThat(fixVersionsField.getValue()).isNull();
            assertThat(fixVersionsField.getStringValue()).isEmpty();

            TemplateField versionsField = (TemplateField) result.get("versions");
            assertThat(versionsField.getValue()).isNull();
            assertThat(versionsField.getStringValue()).isEmpty();

            TemplateField customfield10000 = (TemplateField) result.get("customfield_10000");
            assertThat(customfield10000.getValue()).isNull();
            assertThat(customfield10000.getStringValue()).isNull();
        }

        @Test
        @DisplayName("should return empty map when issue is null")
        void should_return_empty_map_when_issue_is_null_boolean() {
            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(null, true);
            assertThat(result).isEmpty();
        }

    }

    @Nested
    @DisplayName("getJiraIssueFieldsCtx(JiraIssue, long) method tests")
    class GetJiraIssueFieldsCtxHistoryIdTests {

        @Test
        @DisplayName("should return map with all fields")
        void should_return_map_with_all_fields() {
            ApplicationProperties.Jira jiraConfig = new ApplicationProperties.Jira();
            jiraConfig.setNames(Map.of("fixVersions", "Fix Version",
                    "versions", "Version",
                    "components", "Component"));
            when(applicationProperties.getJira()).thenReturn(jiraConfig);

            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(issue, 10000L);
            assertThat(result).containsKeys("baseUrl", "key", "status", "issuetype", "project",
                    "summary", "description", "creator", "reporter", "assignee", "resolution",
                    "created", "updated", "resolutiondate", "duedate", "priority", "components",
                    "fixVersions", "versions", "timeoriginalestimate", "timeestimate", "timespent",
                    "labels", "customfield_10000", "customfield_10001", "customfield_10002");
        }

        @Test
        @DisplayName("should return issue values including chages up to specified history id")
        void should_return_issue_values_including_chages_up_to_specified_history_id() {
            ApplicationProperties.Jira jiraConfig = new ApplicationProperties.Jira();
            jiraConfig.setNames(Map.of("fixVersions", "Fix Version",
                    "versions", "Version",
                    "components", "Component"));
            when(applicationProperties.getJira()).thenReturn(jiraConfig);

            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(issue, 10000L);

            TemplateField keyField = (TemplateField) result.get("key");
            assertThat(keyField.getValue()).isEqualTo("10000");
            assertThat(keyField.getStringValue()).isEqualTo("TST-1");

            TemplateField statusField = (TemplateField) result.get("status");
            assertThat(statusField.getValue()).isEqualTo("3");
            assertThat(statusField.getStringValue()).isEqualTo("In Progress");

            TemplateField issueTypeField = (TemplateField) result.get("issuetype");
            assertThat(issueTypeField.getValue()).isEqualTo("1");
            assertThat(issueTypeField.getStringValue()).isEqualTo("Bug");

            TemplateField priorityField = (TemplateField) result.get("priority");
            assertThat(priorityField.getValue()).isEqualTo("3");
            assertThat(priorityField.getStringValue()).isEqualTo("Major");

            TemplateField componentsField = (TemplateField) result.get("components");
            assertThat(componentsField.getValue()).contains("10000", "10001");
            assertThat(componentsField.getStringValue()).contains("Component A", "Component B");

            TemplateField fixVersionsField = (TemplateField) result.get("fixVersions");
            assertThat(fixVersionsField.getValue()).contains("10000");
            assertThat(fixVersionsField.getStringValue()).contains("1.1");

            TemplateField versionsField = (TemplateField) result.get("versions");
            assertThat(versionsField.getValue()).contains("10000", "10001");
            assertThat(versionsField.getStringValue()).contains("1", "1.1");

            TemplateField creatorField = (TemplateField) result.get("creator");
            assertThat(creatorField.getValue()).isEqualTo("admin");
            assertThat(creatorField.getStringValue()).isEqualTo("Administrator");

            TemplateField reporterField = (TemplateField) result.get("reporter");
            assertThat(reporterField.getValue()).isEqualTo("admin");
            assertThat(reporterField.getStringValue()).isEqualTo("Administrator");

            TemplateField assigneeField = (TemplateField) result.get("assignee");
            assertThat(assigneeField.getValue()).isEqualTo("admin");
            assertThat(assigneeField.getStringValue()).isEqualTo("Administrator");

            TemplateField labelsField = (TemplateField) result.get("labels");
            assertThat(labelsField.getValue()).isNull();
            assertThat(labelsField.getStringValue()).contains("bug", "critical");

            TemplateField createdField = (TemplateField) result.get("created");
            assertThat(createdField.getValue()).contains("2026-01-01T00:00:00+0200");
            assertThat(createdField.getStringValue()).contains("01.01.2026");

            TemplateField dueDateField = (TemplateField) result.get("duedate");
            assertThat(dueDateField.getValue()).isEqualTo("2026-12-31");
            assertThat(dueDateField.getStringValue()).isEqualTo("2026-12-31 00:00:00.0");

            TemplateField resolutionDateField = (TemplateField) result.get("resolutiondate");
            assertThat(resolutionDateField.getValue()).isEqualTo("2026-12-31T23:59:59+0200");
            assertThat(resolutionDateField.getStringValue()).isEqualTo("31.12.2026 23:59");

            TemplateField timeOriginalEstimateField =
                    (TemplateField) result.get("timeoriginalestimate");
            assertThat(timeOriginalEstimateField.getValue()).isEqualTo("25200");
            assertThat(timeOriginalEstimateField.getStringValue()).isEqualTo("25200");

            TemplateField timeEstimateField = (TemplateField) result.get("timeestimate");
            assertThat(timeEstimateField.getValue()).isEqualTo("21600");
            assertThat(timeEstimateField.getStringValue()).isEqualTo("21600");

            TemplateField timeSpentField = (TemplateField) result.get("timespent");
            assertThat(timeSpentField.getValue()).isEqualTo("8700");
            assertThat(timeSpentField.getStringValue()).isEqualTo("8700");

            TemplateField customfield10000 = (TemplateField) result.get("customfield_10000");
            assertThat(customfield10000.getValue()).isNull();
            assertThat(customfield10000.getStringValue()).isEmpty();

            TemplateField customfield10001 = (TemplateField) result.get("customfield_10001");
            assertThat(customfield10001.getValue()).isEqualTo("10001");
            assertThat(customfield10001.getStringValue()).isEqualTo("Another");

            TemplateField customfield10002 = (TemplateField) result.get("customfield_10002");
            assertThat(customfield10002.getValue()).isNull();
            assertThat(customfield10002.getStringValue()).isNull();
        }

        @Test
        @DisplayName("should return empty map when issue is null")
        void should_return_empty_map_when_issue_is_null_history_id() {
            Map<String, Object> result = jiraUtil.getJiraIssueFieldsCtx(null, 10000L);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isIssueCreatedAfter() method tests")
    class IsIssueCreatedAfterTests {

        @Test
        @DisplayName("should return true when issue created after specified date")
        void should_return_true_when_issue_created_after_specified_date() {
            ZonedDateTime before =
                    ZonedDateTime.of(2025, 12, 31, 23, 59, 59, 0, ZoneId.of("+0200"));

            boolean result = jiraUtil.isIssueCreatedAfter(issue, before);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when issue created before specified date")
        void should_return_false_when_issue_created_before_specified_date() {
            ZonedDateTime after = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));

            boolean result = jiraUtil.isIssueCreatedAfter(issue, after);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when issue created at same time as specified date")
        void should_return_false_when_issue_created_at_same_time() {
            ZonedDateTime sameTime = issue.getCreated();

            boolean result = jiraUtil.isIssueCreatedAfter(issue, sameTime);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("convertFieldNameToKey() method tests")
    @TestPropertySource(properties = {
        "jena.jira.names.components=Component",
    })
    class ConvertFieldNameToKeyTests {

        @Mock
        private ApplicationProperties.Jira jira;

        @BeforeEach
        void setUp() {
            Map<String, String> customNames = new HashMap<>();
            customNames.put("Component", "components");

            lenient().when(applicationProperties.getJira()).thenReturn(jira);
            lenient().when(jira.getReversedNames()).thenReturn(customNames);
        }

        @Test
        @DisplayName("should convert field name to key")
        void should_convert_field_name_to_key() {
            JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                    .field("My Custom Field")
                    .from("1")
                    .fromString("Old value")
                    .to("2")
                    .toString("New Value")
                    .build();
            Map<String, String> names = new HashMap<>();
            names.put("My Custom Field", "customfield_10000");
            names.put("Summary", "summary");

            JiraIssueChangelogItem result = jiraUtil.convertFieldNameToKey(item, names);
            assertThat(result).isNotNull();
            assertThat(result.getField()).isEqualTo("customfield_10000");
            assertThat(result.getFrom()).isEqualTo("1");
            assertThat(result.getFromString()).isEqualTo("Old value");
            assertThat(result.getTo()).isEqualTo("2");
            assertThat(result.getToString()).isEqualTo("New Value");
        }

        @Test
        @DisplayName("should convert special field")
        void should_convert_special_field() {
            JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                    .field("Component")
                    .from(null)
                    .fromString(null)
                    .to("10000")
                    .toString("Component A")
                    .build();
            Map<String, String> names = new HashMap<>();

            JiraIssueChangelogItem result = jiraUtil.convertFieldNameToKey(item, names);
            assertThat(result).isNotNull();
            assertThat(result.getField()).isEqualTo("components");
            assertThat(result.getFrom()).isNull();
            assertThat(result.getFromString()).isNull();
            assertThat(result.getTo()).isEqualTo("10000");
            assertThat(result.getToString()).isEqualTo("Component A");
        }

        @Test
        @DisplayName("should return same item when field is not in names map")
        void should_return_same_item_when_field_is_not_in_names_map() {
            JiraIssueChangelogItem unknownFieldItem = JiraIssueChangelogItem.builder()
                    .field("customfield_99999")
                    .from("1")
                    .fromString("Value1")
                    .to("2")
                    .toString("Value2")
                    .build();
            Map<String, String> names = new HashMap<>();
            names.put("My Custom Field", "customfield_10000");

            JiraIssueChangelogItem result = jiraUtil.convertFieldNameToKey(unknownFieldItem, names);
            assertThat(result).isNotNull();
            assertThat(result.getField()).isEqualTo("customfield_99999");
            assertThat(result.getFrom()).isEqualTo("1");
            assertThat(result.getFromString()).isEqualTo("Value1");
        }

        @Test
        @DisplayName("should return same item when names map is empty")
        void should_return_same_item_when_names_map_is_empty() {
            JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                    .field("My Custom Field")
                    .from("1")
                    .fromString("Old value")
                    .to("2")
                    .toString("New Value")
                    .build();
            Map<String, String> emptyNames = new HashMap<>();

            JiraIssueChangelogItem result = jiraUtil.convertFieldNameToKey(item, emptyNames);
            assertThat(result.getField()).isEqualTo("My Custom Field");
        }

        @Test
        @DisplayName("should throw exception when item is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_item_is_null() {
            assertThatThrownBy(() -> jiraUtil.convertFieldNameToKey(null, new HashMap<>()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception when names is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_names_is_null() {
            JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                    .field("My Custom Field")
                    .from("1")
                    .fromString("Old value")
                    .to("2")
                    .toString("New Value")
                    .build();

            assertThatThrownBy(() -> jiraUtil.convertFieldNameToKey(item, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    private JiraIssue issueWithNullCreatorAndReporter() {
        return new JiraIssue(
                issue.getId(),
                issue.getSelf(),
                issue.getKey(),
                issue.getStatus(),
                issue.getIssueType(),
                issue.getProject(),
                issue.getComponents(),
                issue.getSummary(),
                issue.getDescription(),
                null,
                null,
                issue.getAssignee(),
                issue.getResolution(),
                issue.getCreated(),
                issue.getUpdated(),
                issue.getResolutiondate(),
                issue.getDuedate(),
                issue.getPriority(),
                issue.getFixVersions(),
                issue.getVersions(),
                issue.getTimeoriginalestimate(),
                issue.getTimeestimate(),
                issue.getTimespent(),
                issue.getLabels(),
                issue.getIssueFields(),
                issue.getChangelog(),
                issue.getNames());
    }
}