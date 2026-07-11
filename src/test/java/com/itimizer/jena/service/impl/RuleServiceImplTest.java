package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.repository.ChangelogRepository;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static com.itimizer.jena.entity.Event.ISSUE_CREATED;
import static com.itimizer.jena.entity.Event.ISSUE_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("RuleService Integration Tests")
class RuleServiceImplTest {

    @Autowired
    private RuleService ruleService;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;
    @Autowired
    private TransactionRunner transactionRunner;

    private Template template;
    private JiraFilter filter;
    private RuleCreateDto ruleCreateDto;

    @BeforeEach
    void setUp() {
        template = new Template();
        template.setName("template");
        template.setContent("{\"content\": \"Test Content\"}");
        template = templateRepository.save(template);

        filter = new JiraFilter();
        filter.setName("Test Filter");
        filter.setJql("project = TST");
        filter.setEnabled(true);
        filter = jiraFilterRepository.save(filter);

        ruleCreateDto = new RuleCreateDto(
                template.getId(),
                true,
                ISSUE_UPDATED,
                "status",
                "1",
                "Open",
                "2",
                "Resolved",
                false
        );
    }

    @AfterEach
    void tearDown() {
        changelogRepository.deleteAll();
        ruleRepository.deleteAll();
        jiraFilterRepository.deleteAll();
        templateRepository.deleteAll();
    }

    private void createRuleAttachedToFilter() {
        ruleService.create(ruleCreateDto);
        transactionRunner.doInTransaction(() -> {
            Rule rule = ruleRepository.findAll().getFirst();
            rule.getFilters().add(filter);
            return ruleRepository.save(rule);
        });
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateMethodTests {

        @Test
        @DisplayName("should create rule successfully when template exists")
        void should_create_rule_successfully_when_template_exists() {
            RuleDto result = ruleService.create(ruleCreateDto);
            assertThat(result).isNotNull();
            assertThat(result.id()).isNotNull();
            assertThat(result.template()).isEqualTo(template.getId());
            assertThat(result.enabled()).isTrue();
            assertThat(result.event()).isEqualTo(ISSUE_UPDATED);
            assertThat(result.field()).isEqualTo("status");
            assertThat(result.from()).isEqualTo("1");
            assertThat(result.fromStr()).isEqualTo("Open");
            assertThat(result.to()).isEqualTo("2");
            assertThat(result.toStr()).isEqualTo("Resolved");
            assertThat(result.hasChanged()).isFalse();
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when template does not exist")
        void should_throw_object_not_found_exception_when_template_does_not_exist() {
            RuleCreateDto dtoWithInvalidTemplate = new RuleCreateDto(
                    999L,
                    true,
                    ISSUE_UPDATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );

            assertThatThrownBy(() -> ruleService.create(dtoWithInvalidTemplate))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when DTO is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_dto_is_null() {
            assertThatThrownBy(() -> ruleService.create(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("update() method tests")
    class UpdateMethodTests {

        @Test
        @DisplayName("should update rule successfully")
        void should_update_rule_successfully() {
            RuleDto created = ruleService.create(ruleCreateDto);

            RuleDto updateDto = new RuleDto(
                    created.id(),
                    template.getId(),
                    false,
                    ISSUE_CREATED,
                    "priority",
                    "3",
                    "Medium",
                    "4",
                    "High",
                    true
            );

            RuleDto updated = ruleService.update(updateDto);
            assertThat(updated).isNotNull();
            assertThat(updated.id()).isEqualTo(created.id());
            assertThat(updated.template()).isEqualTo(template.getId());
            assertThat(updated.enabled()).isFalse();
            assertThat(updated.event()).isEqualTo(ISSUE_CREATED);
            assertThat(updated.field()).isEqualTo("priority");
            assertThat(updated.from()).isEqualTo("3");
            assertThat(updated.fromStr()).isEqualTo("Medium");
            assertThat(updated.to()).isEqualTo("4");
            assertThat(updated.toStr()).isEqualTo("High");
            assertThat(updated.hasChanged()).isTrue();
        }

        @Test
        @DisplayName("should reassign template when update points to a different template")
        void should_reassign_template_when_update_points_to_different_template() {
            Template otherTemplate = new Template();
            otherTemplate.setName("other-template");
            otherTemplate.setContent("{\"content\": \"Other Content\"}");
            otherTemplate = templateRepository.save(otherTemplate);

            RuleDto created = ruleService.create(ruleCreateDto);

            RuleDto updateDto = new RuleDto(
                    created.id(),
                    otherTemplate.getId(),
                    true,
                    ISSUE_UPDATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );

            RuleDto updated = ruleService.update(updateDto);
            assertThat(updated.id()).isEqualTo(created.id());
            assertThat(updated.template()).isEqualTo(otherTemplate.getId());
            // original template must still exist (its identifier must not be mutated)
            assertThat(templateRepository.findById(template.getId())).isPresent();
            assertThat(templateRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when rule does not exist")
        void should_throw_object_not_found_exception_when_rule_does_not_exist() {
            RuleDto updateDto = new RuleDto(
                    999L,
                    template.getId(),
                    true,
                    ISSUE_UPDATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );

            assertThatThrownBy(() -> ruleService.update(updateDto))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when DTO is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_dto_is_null() {
            assertThatThrownBy(() -> ruleService.create(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when template does not exist")
        void should_throw_object_not_found_exception_when_template_does_not_exist() {
            RuleDto created = ruleService.create(ruleCreateDto);

            RuleDto updateDto = new RuleDto(
                    created.id(),
                    999L,
                    true,
                    ISSUE_UPDATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );

            assertThatThrownBy(() -> ruleService.update(updateDto))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Nested
        @DisplayName("get() method tests")
        class GetMethodTests {

            @Test
            @DisplayName("should get rule by ID successfully")
            void should_get_rule_by_id_successfully() {
                RuleDto created = ruleService.create(ruleCreateDto);

                RuleDto result = ruleService.get(created.id());
                assertThat(result).isNotNull();
                assertThat(result.id()).isEqualTo(created.id());
                assertThat(result.template()).isEqualTo(template.getId());
                assertThat(result.enabled()).isTrue();
                assertThat(result.field()).isEqualTo("status");
                assertThat(result.from()).isEqualTo("1");
                assertThat(result.fromStr()).isEqualTo("Open");
                assertThat(result.to()).isEqualTo("2");
                assertThat(result.toStr()).isEqualTo("Resolved");
                assertThat(result.hasChanged()).isFalse();
            }
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when rule not found")
        void should_throw_object_not_found_exception_when_rule_not_found() {
            assertThatThrownBy(() -> ruleService.get(999L))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when rule is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_rule_is_null() {
            assertThatThrownBy(() -> ruleService.get(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("fetch() method tests")
    class FetchMethodTests {

        @Test
        @DisplayName("should fetch rule by ID successfully")
        void should_fetch_rule_by_id_successfully() {
            RuleDto created = ruleService.create(ruleCreateDto);

            Rule result = ruleService.fetch(created.id());
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(created.id());
            assertThat(result.getTemplate().getId()).isEqualTo(template.getId());
            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getEvent()).isEqualTo(ISSUE_UPDATED);
            assertThat(result.getField()).isEqualTo("status");
            assertThat(result.getFrom()).isEqualTo("1");
            assertThat(result.getFromStr()).isEqualTo("Open");
            assertThat(result.getTo()).isEqualTo("2");
            assertThat(result.getToStr()).isEqualTo("Resolved");
            assertThat(result.isHasChanged()).isFalse();
        }

        @Test
        @DisplayName("should return null when rule not found")
        void should_return_null_when_rule_not_found() {
            Rule result = ruleService.fetch(999L);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when rule is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_rule_is_null() {
            assertThatThrownBy(() -> ruleService.fetch(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("findRulesByEvent() method tests")
    class FindRulesByEventMethodTests {

        @Test
        @DisplayName("should find enabled rules of a filter by event")
        void should_find_enabled_rules_by_event() {
            createRuleAttachedToFilter();

            List<Rule> result = ruleService.findRulesByEvent(filter.getId(), true, ISSUE_UPDATED);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().isEnabled()).isTrue();
            assertThat(result.getFirst().getEvent()).isEqualTo(ISSUE_UPDATED);
            assertThat(result.getFirst().getField()).isEqualTo("status");
        }

        @Test
        @DisplayName("should return empty list when no rules found for event")
        void should_return_empty_list_when_no_rules_found_for_event() {
            List<Rule> result = ruleService.findRulesByEvent(filter.getId(), true, ISSUE_CREATED);
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when event is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_event_is_null() {
            assertThatThrownBy(() -> ruleService.findRulesByEvent(filter.getId(), true, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("findRulesByField() method tests")
    class FindRulesByFieldMethodTests {

        @Test
        @DisplayName("should find rules of a filter by field, enabled status and event")
        void should_find_rules_by_field_enabled_status_and_event() {
            createRuleAttachedToFilter();

            List<Rule> result =
                    ruleService.findRulesByField(filter.getId(), "status", true, ISSUE_UPDATED);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getField()).isEqualTo("status");
            assertThat(result.getFirst().isEnabled()).isTrue();
            assertThat(result.getFirst().getEvent()).isEqualTo(ISSUE_UPDATED);
        }

        @Test
        @DisplayName("should return empty list when no rules found for event")
        void should_return_empty_list_when_no_rules_found_for_event() {
            List<Rule> result =
                    ruleService.findRulesByField(filter.getId(), "status", false, ISSUE_CREATED);
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when variables is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_event_is_null() {
            assertThatThrownBy(() ->
                    ruleService.findRulesByField(filter.getId(), "status", true, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() ->
                    ruleService.findRulesByField(filter.getId(), null, true, ISSUE_CREATED))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}