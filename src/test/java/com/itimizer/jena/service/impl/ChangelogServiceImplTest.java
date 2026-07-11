package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.dto.ChangelogDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.repository.ChangelogRepository;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("ChangelogService Integration Tests")
class ChangelogServiceImplTest {

    @Autowired
    private ChangelogServiceImpl changelogService;
    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;

    private Rule rule;
    private JiraFilter filter;

    @BeforeEach
    void setUp() {
        Template template = new Template();
        template.setName("Test Template");
        template.setContent("{\"content\": \"Test Content\"}");
        template = templateRepository.save(template);

        rule = new Rule();
        rule.setTemplate(template);
        rule.setEnabled(true);
        rule.setEvent(Event.ISSUE_UPDATED);
        rule.setHasChanged(true);
        rule = ruleRepository.save(rule);

        filter = new JiraFilter();
        filter.setName("Test Filter");
        filter.setJql("project = TST");
        filter.setEnabled(true);
        filter = jiraFilterRepository.save(filter);
    }

    @AfterEach
    void teardown() {
        changelogRepository.deleteAll();
        ruleRepository.deleteAll();
        jiraFilterRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateTests {

        @Test
        @DisplayName("should create changelog successfully when rule and filter exist")
        void should_create_changelog_successfully() {
            ChangelogCreateDto dto = new ChangelogCreateDto(
                    Event.ISSUE_UPDATED,
                    10000L,
                    0,
                    "TST-1",
                    rule.getId(),
                    filter.getId(),
                    Map.of("summary", "Test Summary")
            );

            ChangelogDto result = changelogService.create(dto);
            assertThat(result).isNotNull();
            assertThat(result.id()).isNotNull();
            assertThat(result.event()).isEqualTo(Event.ISSUE_UPDATED);
            assertThat(result.issueKey()).isEqualTo("TST-1");
            assertThat(result.rule()).isEqualTo(rule.getId());

            Changelog saved = changelogRepository.findById(result.id()).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getRule().getId()).isEqualTo(rule.getId());
            assertThat(saved.getFilter().getId()).isEqualTo(filter.getId());
        }

        @Test
        @DisplayName("should throw exception when rule does not exist on create")
        void should_throw_exception_when_rule_not_exists_on_create() {
            ChangelogCreateDto dto = new ChangelogCreateDto(
                    Event.ISSUE_UPDATED,
                    1000L,
                    0,
                    "TST-1",
                    999L,
                    filter.getId(),
                    Map.of("summary", "Test Summary")
            );

            assertThatThrownBy(() -> changelogService.create(dto))
                    .isInstanceOf(ObjectNotFoundException.class)
                    .hasMessageContaining("Notification rule with id 999 not found");

            List<Changelog> all = changelogRepository.findAll();
            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when filter does not exist on create")
        void should_throw_exception_when_filter_not_exists_on_create() {
            ChangelogCreateDto dto = new ChangelogCreateDto(
                    Event.ISSUE_UPDATED,
                    1000L,
                    0,
                    "TST-1",
                    rule.getId(),
                    999L,
                    Map.of("summary", "Test Summary")
            );

            assertThatThrownBy(() -> changelogService.create(dto))
                    .isInstanceOf(ObjectNotFoundException.class)
                    .hasMessageContaining("Jira filter with id 999 not found");

            assertThat(changelogRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when DTO is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_dto_is_null() {
            assertThatThrownBy(() -> changelogService.create(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should return existing changelog instead of creating a duplicate")
        void should_return_existing_changelog_instead_of_creating_duplicate() {
            ChangelogCreateDto dto = new ChangelogCreateDto(
                    Event.ISSUE_UPDATED,
                    10000L,
                    0,
                    "TST-1",
                    rule.getId(),
                    filter.getId(),
                    Map.of("summary", "Test Summary")
            );

            ChangelogDto first = changelogService.create(dto);
            ChangelogDto second = changelogService.create(dto);

            assertThat(second).isNotNull();
            assertThat(second.id()).isEqualTo(first.id());
            assertThat(changelogRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("should create one changelog per filter for the same rule and issue")
        void should_create_one_changelog_per_filter() {
            JiraFilter otherFilter = new JiraFilter();
            otherFilter.setName("Other Filter");
            otherFilter.setJql("project = OTH");
            otherFilter.setEnabled(true);
            otherFilter = jiraFilterRepository.save(otherFilter);

            ChangelogCreateDto first = new ChangelogCreateDto(Event.ISSUE_UPDATED, 10000L, 0,
                    "TST-1", rule.getId(), filter.getId(), Map.of("summary", "S"));
            ChangelogCreateDto second = new ChangelogCreateDto(Event.ISSUE_UPDATED, 10000L, 0,
                    "TST-1", rule.getId(), otherFilter.getId(), Map.of("summary", "S"));

            ChangelogDto a = changelogService.create(first);
            ChangelogDto b = changelogService.create(second);

            assertThat(a.id()).isNotEqualTo(b.id());
            assertThat(changelogRepository.findAll()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("update(ChangelogDto) method tests")
    class UpdateDtoTests {

        @Test
        @DisplayName("should update changelog successfully")
        void should_update_changelog_successfully() {
            Changelog changelog = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-1",
                    0,
                    10000L
            );
            changelog.setEvent(Event.ISSUE_UPDATED);
            changelog = changelogRepository.save(changelog);

            ChangelogDto updateDto = new ChangelogDto(
                    changelog.getId(),
                    Event.ISSUE_UPDATED,
                    10000L,
                    2,
                    "TST-2",
                    rule.getId(),
                    Map.of("status", "DONE"),
                    Status.SUCCESS
            );

            ChangelogDto result = changelogService.update(updateDto);
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(changelog.getId());
            assertThat(result.status()).isEqualTo(Status.SUCCESS);

            Changelog updated = changelogRepository.findById(result.id()).orElse(null);
            assertThat(updated).isNotNull();
            assertThat(updated.getIssueKey()).isEqualTo("TST-2");
            assertThat(updated.getContext()).containsEntry("status", "DONE");
            assertThat(updated.getStatus()).isEqualTo(Status.SUCCESS);
        }

        @Test
        @DisplayName("should throw exception when changelog does not exist on update")
        void should_throw_exception_when_changelog_not_exists_on_update() {
            ChangelogDto updateDto = new ChangelogDto(
                    999L,
                    Event.ISSUE_UPDATED,
                    456L,
                    2,
                    "TST-2",
                    rule.getId(),
                    Map.of("summary", "Test Summary"),
                    null
            );

            assertThatThrownBy(() -> changelogService.update(updateDto))
                    .isInstanceOf(ObjectNotFoundException.class)
                    .hasMessageContaining("Changelog with id 999 not found");
        }

        @Test
        @DisplayName("should throw exception when rule does not exist on update")
        void should_throw_exception_when_rule_not_exists_on_update() {
            Changelog changelog = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-3",
                    0,
                    789L
            );
            changelog.setEvent(Event.ISSUE_UPDATED);
            changelog = changelogRepository.save(changelog);

            ChangelogDto updateDto = new ChangelogDto(
                    changelog.getId(),
                    Event.ISSUE_UPDATED,
                    789L,
                    1,
                    "TST-3",
                    999L,
                    Map.of("summary", "Test Summary"),
                    null
            );

            assertThatThrownBy(() -> changelogService.update(updateDto))
                    .isInstanceOf(ObjectNotFoundException.class)
                    .hasMessageContaining("Notification rule with id 999 not found");
        }

        @Test
        @DisplayName("should throw exception when DTO is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_dto_is_null() {
            assertThatThrownBy(() -> changelogService.update((ChangelogDto) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("update(Changelog) method tests")
    class UpdateEntityTests {

        @Test
        @DisplayName("should update changelog entity successfully")
        void should_update_changelog_entity_successfully() {
            Changelog changelog = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-4",
                    0,
                    10000L
            );
            changelog.setEvent(Event.ISSUE_UPDATED);
            changelog = changelogRepository.save(changelog);
            changelog.setStatus(Status.ERROR);
            changelog.setContext(Map.of("status", "DONE"));

            Changelog result = changelogService.update(changelog);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(Status.ERROR);
            assertThat(result.getContext()).containsEntry("status", "DONE");

            Changelog persisted = changelogRepository.findById(result.getId()).orElse(null);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getStatus()).isEqualTo(Status.ERROR);
            assertThat(persisted.getContext()).containsEntry("status", "DONE");
        }

        @Test
        @DisplayName("should throw exception when rule does not exist on entity update")
        void should_throw_exception_when_rule_not_exists_on_entity_update() {
            Changelog changelog = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-5",
                    0,
                    10000L
            );
            changelog.setEvent(Event.ISSUE_UPDATED);
            changelog = changelogRepository.save(changelog);
            Rule nonExistentRule = new Rule();
            nonExistentRule.setId(999L);
            changelog.setRule(nonExistentRule);
            Changelog finalChangelog = changelog;

            assertThatThrownBy(() -> changelogService.update(finalChangelog))
                    .isInstanceOf(ObjectNotFoundException.class)
                    .hasMessageContaining("Notification rule with id 999 not found");
        }

        @Test
        @DisplayName("should throw exception when changelog entity is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_changelog_entity_is_null() {
            assertThatThrownBy(() -> changelogService.update((Changelog) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("findNewChangelog() method tests")
    class FindNewChangelogTests {

        @Test
        @DisplayName("should find new changelogs with status null")
        void should_find_new_changelogs_successfully() {
            Changelog changelog1 = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-6",
                    0,
                    333L
            );
            changelog1.setEvent(Event.ISSUE_UPDATED);
            changelogRepository.save(changelog1);

            Changelog changelog2 = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-7",
                    0,
                    444L
            );
            changelog2.setEvent(Event.ISSUE_UPDATED);
            changelogRepository.save(changelog2);

            Changelog processedChangelog = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-8",
                    0,
                    555L
            );
            processedChangelog.setEvent(Event.ISSUE_UPDATED);
            processedChangelog.setStatus(Status.SUCCESS);
            changelogRepository.save(processedChangelog);

            List<Changelog> result = changelogService.findNewChangelog();
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Changelog::getStatus).containsOnlyNulls();
            assertThat(result).extracting(Changelog::getIssueKey)
                    .containsExactlyInAnyOrder("TST-6", "TST-7");
        }

        @Test
        @DisplayName("should return empty list when no new changelogs found")
        void should_return_empty_list_when_no_new_changelogs_found() {
            Changelog changelog = new Changelog(
                    Map.of("summary", "Test Summary"),
                    rule,
                    filter,
                    "TST-9",
                    0,
                    666L
            );
            changelog.setEvent(Event.ISSUE_UPDATED);
            changelog.setStatus(Status.SUCCESS);
            changelogRepository.save(changelog);

            List<Changelog> result = changelogService.findNewChangelog();
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should find new changelogs when multiple exist")
        void should_find_new_changelogs_when_multiple_exist() {
            for (int i = 0; i < 5; i++) {
                Changelog changelog = new Changelog(
                        Map.of("summary", "Test Summary"),
                        rule,
                        filter,
                        "TST-" + (100 + i),
                        i,
                        (1000L + i)
                );
                changelog.setEvent(Event.ISSUE_UPDATED);
                changelogRepository.save(changelog);
            }

            List<Changelog> result = changelogService.findNewChangelog();
            assertThat(result).hasSize(5);
            assertThat(result).allMatch(c -> c.getStatus() == null);
        }
    }
}