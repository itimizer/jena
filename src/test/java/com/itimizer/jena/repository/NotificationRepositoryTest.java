package com.itimizer.jena.repository;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.NotificationRecipientDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Notification;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.entity.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("Notification Repository Integration Tests")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;

    Changelog changelog;
    NotificationRecipientDto target;

    @BeforeEach
    void setUp() {
        Template template = new Template();
        template.setName("Test Template");
        template.setContent("{\"content\": \"Test Content\"}");
        template = templateRepository.save(template);

        Rule rule = new Rule();
        rule.setTemplate(template);
        rule.setEnabled(true);
        rule.setEvent(Event.ISSUE_CREATED);
        rule.setHasChanged(true);
        rule = ruleRepository.save(rule);

        JiraFilter filter = new JiraFilter();
        filter.setName("Test Filter");
        filter.setJql("project = TST");
        filter.setEnabled(true);
        filter = jiraFilterRepository.save(filter);

        changelog = new Changelog();
        changelog.setEvent(Event.ISSUE_CREATED);
        changelog.setItem(10000L);
        changelog.setItemIndex(0);
        changelog.setIssueKey("PROJ-1");
        changelog.setRule(rule);
        changelog.setFilter(filter);
        Map<String, Object> context = Map.of();
        changelog.setContext(context);
        changelog = changelogRepository.save(changelog);
        target = new NotificationRecipientDto("123456");
    }

    @Test
    @DisplayName("should find notifications with ERROR status and failure count within threshold")
    void should_find_retry_notifications() {
        Notification retryableNotification = new Notification();
        retryableNotification.setChangelog(changelog);
        retryableNotification.setIssueKey("PROJ-1");
        retryableNotification.setChannel(Channel.TELEGRAM);
        retryableNotification.setTarget(target);
        retryableNotification.setContent("Test Content");
        retryableNotification.setStatus(Status.ERROR);
        retryableNotification.setFailureCount(2);

        Notification anotherRetryableNotification = new Notification();
        anotherRetryableNotification.setChangelog(changelog);
        anotherRetryableNotification.setIssueKey("PROJ-1");
        anotherRetryableNotification.setChannel(Channel.TELEGRAM);
        anotherRetryableNotification.setTarget(target);
        anotherRetryableNotification.setContent("Test Content");
        anotherRetryableNotification.setStatus(Status.ERROR);
        anotherRetryableNotification.setFailureCount(1);

        Notification nonRetryableNotification = new Notification();
        nonRetryableNotification.setChangelog(changelog);
        nonRetryableNotification.setIssueKey("PROJ-1");
        nonRetryableNotification.setChannel(Channel.TELEGRAM);
        nonRetryableNotification.setTarget(target);
        nonRetryableNotification.setContent("Test Content");
        nonRetryableNotification.setStatus(Status.ERROR);
        nonRetryableNotification.setFailureCount(5);

        Notification successNotification = new Notification();
        successNotification.setChangelog(changelog);
        successNotification.setIssueKey("PROJ-1");
        successNotification.setChannel(Channel.TELEGRAM);
        successNotification.setTarget(target);
        successNotification.setContent("Test Content");
        successNotification.setStatus(Status.SUCCESS);
        successNotification.setFailureCount(0);

        notificationRepository.save(retryableNotification);
        notificationRepository.save(anotherRetryableNotification);
        notificationRepository.save(nonRetryableNotification);
        notificationRepository.save(successNotification);

        List<Notification> result =
                notificationRepository
                        .findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
                        Status.ERROR, 3);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> n.getStatus() == Status.ERROR && n.getFailureCount() <= 3);
    }
}