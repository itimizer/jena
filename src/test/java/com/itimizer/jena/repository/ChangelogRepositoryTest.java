package com.itimizer.jena.repository;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.entity.Template;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("Changelog Repository Integration Tests")
class ChangelogRepositoryTest {

    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;

    private Rule rule;
    private JiraFilter filter;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        Template template = new Template();
        template.setName("Test Template");
        template.setContent("{\"content\": \"Test Content\"}");
        template = templateRepository.save(template);

        rule = new Rule();
        rule.setTemplate(template);
        rule.setEnabled(true);
        rule.setEvent(Event.ISSUE_CREATED);
        rule.setHasChanged(true);
        rule = ruleRepository.save(rule);

        filter = new JiraFilter();
        filter.setName("Test Filter");
        filter.setJql("project = TST");
        filter.setEnabled(true);
        filter = jiraFilterRepository.save(filter);

        context = Map.of();
    }

    @AfterEach
    void teardown() {
        changelogRepository.deleteAll();
        ruleRepository.deleteAll();
        jiraFilterRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    @DisplayName("should find changelogs with null status")
    void should_find_changelogs_with_null_status() {
        Changelog changelogWithNullStatus = new Changelog();
        changelogWithNullStatus.setEvent(Event.ISSUE_CREATED);
        changelogWithNullStatus.setItem(10000L);
        changelogWithNullStatus.setItemIndex(0);
        changelogWithNullStatus.setIssueKey("PROJ-1");
        changelogWithNullStatus.setRule(rule);
        changelogWithNullStatus.setFilter(filter);
        changelogWithNullStatus.setContext(context);
        changelogRepository.save(changelogWithNullStatus);

        Changelog changelogWithStatus = new Changelog();
        changelogWithStatus.setEvent(Event.ISSUE_UPDATED);
        changelogWithStatus.setItem(10001L);
        changelogWithStatus.setItemIndex(0);
        changelogWithStatus.setIssueKey("PROJ-2");
        changelogWithStatus.setRule(rule);
        changelogWithStatus.setFilter(filter);
        changelogWithStatus.setContext(context);
        changelogWithStatus.setStatus(Status.SUCCESS);
        changelogRepository.save(changelogWithStatus);

        List<Changelog> result = changelogRepository.findByStatusIsNull();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isNull();
    }

    @Test
    @DisplayName("should reject duplicate changelog with same natural key")
    void should_reject_duplicate_changelog_with_same_natural_key() {
        Changelog changelog = new Changelog(context, rule, filter, "PROJ-3", 1, 20000L);
        changelog.setEvent(Event.ISSUE_UPDATED);
        changelogRepository.saveAndFlush(changelog);

        Changelog duplicate = new Changelog(context, rule, filter, "PROJ-3", 1, 20000L);
        duplicate.setEvent(Event.ISSUE_UPDATED);

        assertThatThrownBy(() -> changelogRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should find changelog by natural key")
    void should_find_changelog_by_natural_key() {
        Changelog changelog = new Changelog(context, rule, filter, "PROJ-4", 2, 30000L);
        changelog.setEvent(Event.ISSUE_UPDATED);
        changelog = changelogRepository.save(changelog);

        assertThat(changelogRepository
                .findByEventAndItemAndItemIndexAndIssueKeyAndRuleIdAndFilterId(
                        Event.ISSUE_UPDATED, 30000L, 2, "PROJ-4", rule.getId(), filter.getId()))
                .isPresent()
                .get()
                .extracting(Changelog::getId)
                .isEqualTo(changelog.getId());

        assertThat(changelogRepository
                .findByEventAndItemAndItemIndexAndIssueKeyAndRuleIdAndFilterId(
                        Event.ISSUE_CREATED, 30000L, 2, "PROJ-4", rule.getId(), filter.getId()))
                .isEmpty();
    }
}