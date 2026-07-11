package com.itimizer.jena.mapper;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueBasicObject;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.domain.JiraIssueField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIssueDeserializer Tests")
class JiraIssueDeserializerTest {

    @Mock
    private DeserializationContext deserializationContext;

    private JiraIssueDeserializer deserializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        deserializer = new JiraIssueDeserializer();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("should deserialize valid issue")
    void should_deserialize_valid_issue() throws IOException {
        JiraIssue issue = deserializer.deserialize(
                objectMapper
                        .createParser(new ClassPathResource("json/issue/valid-issue.json")
                                .getFile()),
                deserializationContext);

        assertThat(issue).isNotNull();
        assertThat(issue.getId()).isEqualTo(10000L);
        assertThat(issue.getKey()).isEqualTo("TST-1");
        assertThat(issue.getSelf()).isEqualTo("http://localhost:8090/jira/rest/api/latest/issue/10010");
        assertThat(issue.getSummary()).isEqualTo("Test Issue Summary");
        assertThat(issue.getDescription()).isEqualTo("Test Issue Description");

        assertThat(issue.getStatus()).isNotNull();
        assertThat(issue.getStatus().getId()).isEqualTo(3L);
        assertThat(issue.getStatus().getName()).isEqualTo("In Progress");

        assertThat(issue.getIssueType()).isNotNull();
        assertThat(issue.getIssueType().getId()).isEqualTo(1L);
        assertThat(issue.getIssueType().getName()).isEqualTo("Bug");

        assertThat(issue.getProject()).isNotNull();
        assertThat(issue.getProject().getId()).isEqualTo(10000L);
        assertThat(issue.getProject().getKey()).isEqualTo("TST");
        assertThat(issue.getProject().getName()).isEqualTo("Test Project");

        assertThat(issue.getComponents()).isNotNull();
        assertThat(issue.getComponents()).hasSize(2);
        assertThat(issue.getComponents())
                .extracting(JiraIssueBasicObject::getName)
                .contains("Component A", "Component B");
        assertThat(issue.getComponents())
                .extracting(JiraIssueBasicObject::getId)
                .contains(10000L, 10001L);

        assertThat(issue.getCreator()).isNotNull();
        assertThat(issue.getCreator().getKey()).isEqualTo("admin");
        assertThat(issue.getCreator().getName()).isEqualTo("admin");
        assertThat(issue.getCreator().getEmailAddress()).isEqualTo("admin@example.com");
        assertThat(issue.getCreator().getDisplayName()).isEqualTo("Administrator");
        assertThat(issue.getCreator().isActive()).isTrue();
        assertThat(issue.getCreator().getTimeZone()).isEqualTo("Antarctica/Troll");

        assertThat(issue.getReporter()).isNotNull();
        assertThat(issue.getReporter().getKey()).isEqualTo("admin");
        assertThat(issue.getReporter().getName()).isEqualTo("admin");
        assertThat(issue.getReporter().getEmailAddress()).isEqualTo("admin@example.com");
        assertThat(issue.getReporter().getDisplayName()).isEqualTo("Administrator");
        assertThat(issue.getReporter().isActive()).isTrue();
        assertThat(issue.getReporter().getTimeZone()).isEqualTo("Antarctica/Troll");

        assertThat(issue.getAssignee()).isNotNull();
        assertThat(issue.getAssignee().getKey()).isEqualTo("admin");
        assertThat(issue.getAssignee().getName()).isEqualTo("admin");
        assertThat(issue.getAssignee().getEmailAddress()).isEqualTo("admin@example.com");
        assertThat(issue.getAssignee().getDisplayName()).isEqualTo("Administrator");
        assertThat(issue.getAssignee().isActive()).isTrue();
        assertThat(issue.getAssignee().getTimeZone()).isEqualTo("Antarctica/Troll");

        assertThat(issue.getResolution()).isNotNull();
        assertThat(issue.getResolution().getId()).isEqualTo(10000L);
        assertThat(issue.getResolution().getName()).isEqualTo("Done");

        assertThat(issue.getCreated()).isNotNull();
        assertThat(issue.getCreated()).isEqualTo("2026-01-01T00:00:00.000+02:00");
        assertThat(issue.getUpdated()).isNotNull();
        assertThat(issue.getUpdated()).isEqualTo("2026-02-01T00:00:00.000+02:00");
        assertThat(issue.getResolutiondate()).isNotNull();
        assertThat(issue.getResolutiondate()).isEqualTo("2026-12-31T23:59:59.999+02:00");
        assertThat(issue.getDuedate()).isNotNull();
        assertThat(issue.getDuedate()).isEqualTo("2026-12-31T00:00");

        assertThat(issue.getPriority()).isNotNull();
        assertThat(issue.getPriority().getId()).isEqualTo(3L);
        assertThat(issue.getPriority().getName()).isEqualTo("Major");

        assertThat(issue.getFixVersions()).isNotNull();
        assertThat(issue.getFixVersions()).hasSize(1);
        assertThat(issue.getFixVersions())
                .extracting(JiraIssueBasicObject::getName)
                .contains("1.1");
        assertThat(issue.getFixVersions())
                .extracting(JiraIssueBasicObject::getId)
                .contains(10000L);

        assertThat(issue.getVersions()).isNotNull();
        assertThat(issue.getVersions()).hasSize(2);
        assertThat(issue.getVersions())
                .extracting(JiraIssueBasicObject::getName)
                .contains("1", "1.1");
        assertThat(issue.getVersions())
                .extracting(JiraIssueBasicObject::getId)
                .contains(10001L, 10000L);

        assertThat(issue.getTimeoriginalestimate()).isEqualTo(25200L);
        assertThat(issue.getTimeestimate()).isEqualTo(21600L);
        assertThat(issue.getTimespent()).isEqualTo(8700L);

        assertThat(issue.getLabels()).isNotNull();
        assertThat(issue.getLabels()).hasSize(2);
        assertThat(issue.getLabels()).contains("bug", "critical");

        assertThat(issue.getIssueFields()).isNotEmpty();
        JiraIssueField customfield10000 = issue.getIssueFields().stream()
                .filter(field -> field.getKey().equals("customfield_10000"))
                .findFirst()
                .orElse(null);
        assertThat(customfield10000).isNotNull();
        assertThat(customfield10000.getKey()).isEqualTo("customfield_10000");
        assertThat(customfield10000.getName()).isEqualTo("My Number Field");
        assertThat(customfield10000.getType()).isEqualTo("number");
        assertThat(customfield10000.getCustom())
                .isEqualTo("com.atlassian.jira.plugin.system.customfieldtypes:float");
        assertThat(customfield10000.getValue().asString()).isEqualTo("1.457");
        JiraIssueField customfield10001 = issue.getIssueFields().stream()
                .filter(field -> field.getKey().equals("customfield_10001"))
                .findFirst()
                .orElse(null);
        assertThat(customfield10001).isNotNull();
        assertThat(customfield10001.getKey()).isEqualTo("customfield_10001");
        assertThat(customfield10001.getName()).isEqualTo("My Radio buttons");
        assertThat(customfield10001.getType()).isEqualTo("string");
        assertThat(customfield10001.getCustom())
                .isEqualTo("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons");
        assertThatNoException().isThrownBy(() -> objectMapper
                .readTree(customfield10001.getValue().toString()));
        JiraIssueField customfield10002 = issue.getIssueFields().stream()
                .filter(field -> field.getKey().equals("customfield_10002"))
                .findFirst()
                .orElse(null);
        assertThat(customfield10002).isNotNull();
        assertThat(customfield10002.getKey()).isEqualTo("customfield_10002");
        assertThat(customfield10002.getName()).isEqualTo("My Text Field");
        assertThat(customfield10002.getType()).isEqualTo("string");
        assertThat(customfield10002.getCustom())
                .isEqualTo("com.atlassian.jira.plugin.system.customfieldtypes:textarea");
        assertThat(customfield10002.getValue()).isNull();

        assertThat(issue.getChangelog()).isNotEmpty();
        JiraIssueChangelogGroup changelogGroup = issue.getChangelog().stream()
                .filter(group -> group.getId() == 10000L)
                .findFirst()
                .orElse(null);
        assertThat(changelogGroup).isNotNull();
        assertThat(changelogGroup.getAuthor().getKey()).isEqualTo("admin");
        assertThat(changelogGroup.getCreated()).isEqualTo("2026-01-01T12:00:00.00+02:00");
        JiraIssueChangelogItem changelogItem = changelogGroup.getItems().stream()
                .filter(item -> item.getField().equals("Component"))
                .findFirst()
                .orElse(null);
        assertThat(changelogItem).isNotNull();
        assertThat(changelogItem.getField()).isEqualTo("Component");
        assertThat(changelogItem.getFrom()).isNull();
        assertThat(changelogItem.getTo()).isEqualTo("10001");

        assertThat(issue.getNames())
                .isNotEmpty()
                .containsEntry("Affects Version/s", "versions");
    }

    @Test
    @DisplayName("should deserialize null issue")
    void should_deserialize_null_issue() throws IOException {
        JiraIssue issue = deserializer.deserialize(
                objectMapper
                        .createParser(new ClassPathResource("json/issue/null-issue.json")
                                .getFile()),
                deserializationContext);

        assertThat(issue).isNotNull();
        assertThat(issue.getDescription()).isNull();
        assertThat(issue.getIssueFields()).isEmpty();
        assertThat(issue.getChangelog()).isEmpty();
        assertThat(issue.getNames())
                .isNotEmpty()
                .containsEntry("Summary", "summary");
    }

    @Test
    @DisplayName("should handle invalid json")
    void should_handle_invalid_json() {
        assertThatThrownBy(() -> deserializer.deserialize(
                objectMapper
                        .createParser(new ClassPathResource("json/issue/non-issue.xml").getFile()),
                deserializationContext))
                .isInstanceOf(StreamReadException.class);
    }
}