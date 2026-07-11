package com.itimizer.jena.repository;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Template;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("Rule Repository Integration Tests")
class RuleRepositoryTest {

    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;

    private Template template;
    private JiraFilter filter;
    private JiraFilter otherFilter;

    @BeforeEach
    void setUp() {
        template = new Template();
        template.setName("Test Template");
        template.setContent("{\"content\": \"Test Content\"}");
        template = templateRepository.save(template);

        filter = newFilter("Filter A", "project = A");
        otherFilter = newFilter("Filter B", "project = B");
    }

    @AfterEach
    void teardown() {
        changelogRepository.deleteAll();
        ruleRepository.deleteAll();
        jiraFilterRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    @DisplayName("should find enabled rules of a filter by event")
    void should_find_enabled_rules_by_event() {
        saveRule(true, Event.ISSUE_CREATED, null, filter);
        saveRule(false, Event.ISSUE_CREATED, null, filter);
        saveRule(true, Event.ISSUE_CREATED, null, otherFilter);

        List<Rule> result = ruleRepository.findByFiltersIdAndEnabledAndEvent(
                filter.getId(), true, Event.ISSUE_CREATED);

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result).allMatch(r -> r.isEnabled()
                && r.getEvent() == Event.ISSUE_CREATED);
    }

    @Test
    @DisplayName("should find enabled rules of a filter by field and event")
    void should_find_enabled_rules_by_field_and_event() {
        saveRule(true, Event.ISSUE_UPDATED, "status", filter);
        saveRule(false, Event.ISSUE_UPDATED, "status", filter);
        saveRule(true, Event.ISSUE_CREATED, "status", filter);
        saveRule(true, Event.ISSUE_UPDATED, "priority", filter);
        saveRule(true, Event.ISSUE_UPDATED, "status", otherFilter);

        List<Rule> result = ruleRepository.findByFiltersIdAndFieldAndEnabledAndEvent(
                filter.getId(), "status", true, Event.ISSUE_UPDATED);

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.getFirst().getField()).isEqualTo("status");
        assertThat(result.getFirst().isEnabled()).isTrue();
    }

    private JiraFilter newFilter(String name, String jql) {
        JiraFilter f = new JiraFilter();
        f.setName(name);
        f.setJql(jql);
        f.setEnabled(true);
        return jiraFilterRepository.save(f);
    }

    private void saveRule(boolean enabled, Event event, String field, JiraFilter attached) {
        Rule rule = new Rule();
        rule.setTemplate(template);
        rule.setEnabled(enabled);
        rule.setEvent(event);
        rule.setField(field);
        rule.setHasChanged(true);
        rule.setFilters(Set.of(attached));
        ruleRepository.save(rule);
    }
}