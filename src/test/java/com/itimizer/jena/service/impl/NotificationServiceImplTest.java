package com.itimizer.jena.service.impl;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.TemplateField;
import com.itimizer.jena.dto.NotificationDto;
import com.itimizer.jena.dto.NotificationMessageDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Notification;
import com.itimizer.jena.entity.NotificationTarget;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Status;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.mapper.JiraIssueDeserializer;
import com.itimizer.jena.notification.NotificationSender;
import com.itimizer.jena.repository.ChangelogRepository;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.NotificationRepository;
import com.itimizer.jena.repository.NotificationTargetRepository;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import com.itimizer.jena.service.JiraIssueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@WithMockUser(roles = "USER")
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {ContainersConfig.class})
@TestPropertySource(properties = {
    "jena.notification.telegram.token=123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "jena.notification.telegram.chat-id=123456789",
    "jena.notification.telegram.parse-mode=HTML",
    "jena.notification.telegram.remove-jira-formatting=true",
    "jena.notification.express.chat-id=987654321"
})
@DisplayName("NotificationService Integration Tests")
class NotificationServiceImplTest {

    @Autowired
    private ChangelogRepository changelogRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;
    @Autowired
    private NotificationTargetRepository notificationTargetRepository;
    @Autowired
    private NotificationServiceImpl notificationService;

    @MockitoSpyBean
    @Qualifier("telegramNotificationSender")
    private NotificationSender telegramNotificationSender;
    @MockitoSpyBean
    @Qualifier("jiraEmailNotificationSender")
    private NotificationSender jiraEmailNotificationSender;
    @MockitoSpyBean
    @Qualifier("expressNotificationSender")
    private NotificationSender expressNotificationSender;
    @MockitoSpyBean
    private JiraIssueService jiraIssueService;

    private Changelog changelog;
    private Rule rule;
    private Template template;
    private JiraFilter filter;

    @BeforeEach
    void setUp() {
        String templateContent =
                """
                    {
                        "telegram": {
                          "message": "[(${summary?.stringValue})]: telegram message"
                        },
                        "jira-email": {
                          "message": "[[${key?.value}]]: email message"
                        },
                        "express": {
                          "message": "[[${key?.value}]]: express message"
                        }
                    }
                """;

        template = new Template();
        template.setName("Test Template");
        template.setContent(templateContent);
        template = templateRepository.save(template);

        rule = new Rule();
        rule.setTemplate(template);
        rule.setEnabled(true);
        rule.setEvent(Event.ISSUE_UPDATED);
        rule.setField("customfield_10000");
        rule.setHasChanged(true);
        ruleRepository.save(rule);

        filter = newFilterWithTargets("Test Filter", "telegram-chat", "express-chat");

        changelog = new Changelog();
        changelog.setEvent(Event.ISSUE_UPDATED);
        changelog.setItem(10000L);
        changelog.setItemIndex(0);
        changelog.setIssueKey("TST-1");
        changelog.setRule(rule);
        changelog.setFilter(filter);
        changelog.setContext(Map.of("key", new TemplateField("1000", "TST-1"),
                "summary", new TemplateField("", "<p>Issue Summary</p>")));
        changelog.setStatus(Status.SUCCESS);
        changelog = changelogRepository.save(changelog);
    }

    private JiraFilter newFilterWithTargets(String name, String telegramChatId,
                                            String expressChatId) {
        NotificationTarget telegramTarget = new NotificationTarget();
        telegramTarget.setName(name + " Telegram");
        telegramTarget.setChannel(Channel.TELEGRAM);
        telegramTarget.setChatId(telegramChatId);
        telegramTarget.setEnabled(true);
        telegramTarget = notificationTargetRepository.save(telegramTarget);

        NotificationTarget expressTarget = new NotificationTarget();
        expressTarget.setName(name + " Express");
        expressTarget.setChannel(Channel.EXPRESS);
        expressTarget.setChatId(expressChatId);
        expressTarget.setEnabled(true);
        expressTarget = notificationTargetRepository.save(expressTarget);

        JiraFilter f = new JiraFilter();
        f.setName(name);
        f.setJql("project = TST");
        f.setEnabled(true);
        f.getTargets().add(telegramTarget);
        f.getTargets().add(expressTarget);
        return jiraFilterRepository.save(f);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        changelogRepository.deleteAll();
        ruleRepository.deleteAll();
        jiraFilterRepository.deleteAll();
        notificationTargetRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Nested
    @DisplayName("sendNotification() method tests")
    class SendNotificationMethodTests {

        @Test
        @DisplayName("should send notifications successfully")
        void should_send_notifications_successfully() {
            doReturn(new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK))
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                    .when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.OK))
                    .when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            assertThat(notifications.get(0).getChangelog().getId())
                    .isEqualTo(changelog.getId());
            assertThat(notifications.get(0).getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(notifications.get(0).getChannel()).isEqualTo(Channel.TELEGRAM);
            assertThat(notifications.get(0).getContent())
                    .isEqualTo("Issue Summary: telegram message");

            assertThat(notifications.get(1).getChangelog().getId()).isEqualTo(changelog.getId());
            assertThat(notifications.get(1).getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(notifications.get(1).getChannel()).isEqualTo(Channel.JIRAEMAIL);
            assertThat(notifications.get(1).getContent()).isEqualTo("1000: email message");

            assertThat(notifications.get(2).getChangelog().getId()).isEqualTo(changelog.getId());
            assertThat(notifications.get(2).getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(notifications.get(2).getChannel()).isEqualTo(Channel.EXPRESS);
            assertThat(notifications.get(2).getContent()).isEqualTo("1000: express message");
        }

        @Test
        @DisplayName("should create notification with ERROR status when sending fails")
        void should_create_notification_with_error_status_when_sending_fails() {
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"",
                    HttpStatus.BAD_REQUEST))
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>("\"errorMessages\":[\"Unrecognized token\"]",
                    HttpStatus.BAD_REQUEST))
                    .when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"",
                    HttpStatus.BAD_REQUEST))
                    .when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            assertThat(notifications.get(0).getStatus()).isEqualTo(Status.ERROR);
            assertThat(notifications.get(0).getFailureCount()).isEqualTo(1);

            assertThat(notifications.get(1).getStatus()).isEqualTo(Status.ERROR);
            assertThat(notifications.get(1).getFailureCount()).isEqualTo(1);

            assertThat(notifications.get(2).getStatus()).isEqualTo(Status.ERROR);
            assertThat(notifications.get(2).getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create notification with SKIPPED status when channel not configured")
        void should_create_notification_with_skipped_status_when_channel_not_configured() {
            doReturn(null).when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(null).when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(null).when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            assertThat(notifications).allSatisfy(notification -> {
                assertThat(notification.getStatus()).isEqualTo(Status.SKIPPED);
                assertThat(notification.getFailureCount()).isEqualTo(0);
            });
        }

        @Test
        @DisplayName("should handle only telegram template gracefully")
        void should_handle_only_telegram_template_gracefully() {
            Template emailOnlyTemplate = new Template();
            emailOnlyTemplate.setName("email-only-template");
            emailOnlyTemplate.setContent(
                    """
                        {
                            "telegram": {
                                "message": "[[${key?.value}]]: telegram message"
                            }
                        }
                    """);
            emailOnlyTemplate = templateRepository.save(emailOnlyTemplate);

            Rule emailOnlyRule = new Rule();
            emailOnlyRule.setTemplate(emailOnlyTemplate);
            emailOnlyRule.setEnabled(true);
            emailOnlyRule.setEvent(Event.ISSUE_CREATED);
            emailOnlyRule.setHasChanged(false);
            emailOnlyRule = ruleRepository.save(emailOnlyRule);

            Changelog emailOnlyChangelog = new Changelog();
            emailOnlyChangelog.setEvent(Event.ISSUE_CREATED);
            emailOnlyChangelog.setItem(10000L);
            emailOnlyChangelog.setItemIndex(0);
            emailOnlyChangelog.setIssueKey("TST-1");
            emailOnlyChangelog.setRule(emailOnlyRule);
            emailOnlyChangelog.setFilter(filter);
            emailOnlyChangelog.setContext(Map.of("key", new TemplateField("1000", "TST-1")));
            emailOnlyChangelog = changelogRepository.save(emailOnlyChangelog);

            doReturn(new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK))
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(emailOnlyChangelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(1);
            assertThat(notifications.getFirst().getChannel()).isEqualTo(Channel.TELEGRAM);
        }

        @Test
        @DisplayName("should route only to the changelog filter's own targets")
        void should_route_only_to_filter_own_targets() {
            newFilterWithTargets("Other Filter", "other-telegram-chat", "other-express-chat");

            doReturn(new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK))
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                    .when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.OK))
                    .when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            assertThat(notifications)
                    .filteredOn(n -> n.getChannel() == Channel.TELEGRAM)
                    .singleElement()
                    .satisfies(n -> assertThat(n.getTarget().chatId()).isEqualTo("telegram-chat"));
            assertThat(notifications)
                    .filteredOn(n -> n.getChannel() == Channel.EXPRESS)
                    .singleElement()
                    .satisfies(n -> assertThat(n.getTarget().chatId()).isEqualTo("express-chat"));
        }

        @Test
        @DisplayName("should throw exception when changelog is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_changelog_is_null() {
            assertThatThrownBy(() -> notificationService.sendNotification(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("resendNotification() method tests")
    class ResendNotificationMethodTests {

        @Test
        @DisplayName("should resend notification successfully")
        void should_resend_notification_successfully() {
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"",
                            HttpStatus.BAD_REQUEST),
                    new ResponseEntity<>("\"{\"ok\":true}\"", HttpStatus.OK))
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>("\"errorMessages\":[\"Unrecognized token\"]",
                            HttpStatus.BAD_REQUEST),
                    new ResponseEntity<>(HttpStatus.NO_CONTENT))
                    .when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"",
                            HttpStatus.BAD_REQUEST),
                    new ResponseEntity<>(HttpStatus.OK))
                    .when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            notificationService.resendNotification(notifications.get(0).getId());
            notificationService.resendNotification(notifications.get(1).getId());
            notificationService.resendNotification(notifications.get(2).getId());

            notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            assertThat(notifications.get(0).getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(notifications.get(0).getFailureCount()).isEqualTo(1);

            assertThat(notifications.get(1).getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(notifications.get(1).getFailureCount()).isEqualTo(1);

            assertThat(notifications.get(2).getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(notifications.get(2).getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should mark notification as SKIPPED on resend when channel not configured")
        void should_mark_notification_skipped_on_resend_when_channel_not_configured() {
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"", HttpStatus.BAD_REQUEST))
                    .doReturn(null)
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                    .when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.OK))
                    .when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);
            Notification telegramNotification = notifications.getFirst();
            assertThat(telegramNotification.getStatus()).isEqualTo(Status.ERROR);
            assertThat(telegramNotification.getFailureCount()).isEqualTo(1);

            notificationService.resendNotification(telegramNotification.getId());

            Notification resent = notificationRepository
                    .findById(telegramNotification.getId()).orElseThrow();
            assertThat(resent.getStatus()).isEqualTo(Status.SKIPPED);
            assertThat(resent.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment failure count when resend fails")
        void should_increment_failure_count_when_resend_fails() {
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"",
                            HttpStatus.BAD_REQUEST),
                    new ResponseEntity<>("\"{\"error_code\":400}\"",
                            HttpStatus.BAD_REQUEST))
                    .when(telegramNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>("\"errorMessages\":[\"Unrecognized token\"]",
                            HttpStatus.BAD_REQUEST),
                    new ResponseEntity<>("\"errorMessages\":[\"Unrecognized token\"]",
                            HttpStatus.BAD_REQUEST))
                    .when(jiraEmailNotificationSender)
                    .send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>("\"{\"error_code\":400}\"",
                            HttpStatus.BAD_REQUEST),
                    new ResponseEntity<>("\"{\"error_code\":400}\"",
                            HttpStatus.BAD_REQUEST))
                    .when(expressNotificationSender)
                    .send(any(NotificationMessageDto.class));

            notificationService.sendNotification(changelog);
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            notificationService.resendNotification(notifications.get(0).getId());
            notificationService.resendNotification(notifications.get(1).getId());
            notificationService.resendNotification(notifications.get(2).getId());

            notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);

            assertThat(notifications.get(0).getStatus()).isEqualTo(Status.ERROR);
            assertThat(notifications.get(0).getFailureCount()).isEqualTo(2);

            assertThat(notifications.get(1).getStatus()).isEqualTo(Status.ERROR);
            assertThat(notifications.get(1).getFailureCount()).isEqualTo(2);

            assertThat(notifications.get(2).getStatus()).isEqualTo(Status.ERROR);
            assertThat(notifications.get(2).getFailureCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when notification not found")
        void should_throw_object_not_found_exception_when_notification_not_found() {
            assertThatThrownBy(() -> notificationService.resendNotification(999L))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when notification id is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_notification_id_is_null() {
            assertThatThrownBy(() -> notificationService.resendNotification(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("dryRun() method tests")
    class DryRunMethodTests {

        @Mock
        private DeserializationContext deserializationContext;

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
        }

        @Test
        @DisplayName("should generate dry run notifications for matching rules")
        void should_generate_dry_run_notifications_for_matching_rules() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            List<NotificationDto> results = notificationService.dryRun(rule.getId(),
                    "TST-1", Channel.TELEGRAM);
            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().rule()).isEqualTo(rule.getId());
            assertThat(results.getFirst().issueKey()).isEqualTo("TST-1");
            assertThat(results.getFirst().channel()).isEqualTo(Channel.TELEGRAM);
            assertThat(results.getFirst().message())
                    .isEqualTo("Test Issue Summary: telegram message");
        }

        @Test
        @DisplayName("should generate dry run notifications with ISSUE_CREATED event")
        void should_generate_dry_run_notifications_with_issue_created_event() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Template template = new Template();
            template.setName("issue-created-template");
            template.setContent(
                    """
                        {
                            "telegram": {
                                "message": "My Number Field: [[${customfield_10000?.stringValue}]]"
                            }
                        }
                    """);
            template = templateRepository.save(template);

            Rule issueCreatedRule = new Rule();
            issueCreatedRule.setTemplate(template);
            issueCreatedRule.setEnabled(true);
            issueCreatedRule.setEvent(Event.ISSUE_CREATED);
            issueCreatedRule = ruleRepository.save(issueCreatedRule);

            List<NotificationDto> results =
                    notificationService.dryRun(issueCreatedRule.getId(), "TST-1", Channel.TELEGRAM);
            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().rule()).isEqualTo(issueCreatedRule.getId());
            assertThat(results.getFirst().issueKey()).isEqualTo("TST-1");
            assertThat(results.getFirst().channel()).isEqualTo(Channel.TELEGRAM);
            assertThat(results.getFirst().message()).isEqualTo("My Number Field: ");
        }

        @Test
        @DisplayName("should generate dry run notifications with ISSUE_UPDATED event")
        void should_generate_dry_run_notifications_with_issue_updated_event() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Template template = new Template();
            template.setName("issue-created-template");
            template.setContent(
                    """
                        {
                            "telegram": {
                                "message": "My Number Field: [[${customfield_10000?.stringValue}]]"
                            }
                        }
                    """);
            template = templateRepository.save(template);

            Rule issueUpdatedRule = new Rule();
            issueUpdatedRule.setTemplate(template);
            issueUpdatedRule.setEnabled(true);
            issueUpdatedRule.setEvent(Event.ISSUE_UPDATED);
            issueUpdatedRule.setField("customfield_10000");
            issueUpdatedRule = ruleRepository.save(issueUpdatedRule);

            List<NotificationDto> results =
                    notificationService.dryRun(issueUpdatedRule.getId(), "TST-1", Channel.TELEGRAM);
            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().rule()).isEqualTo(issueUpdatedRule.getId());
            assertThat(results.getFirst().issueKey()).isEqualTo("TST-1");
            assertThat(results.getFirst().channel()).isEqualTo(Channel.TELEGRAM);
            assertThat(results.getFirst().message()).isEqualTo("My Number Field: 1.457");
        }

        @Test
        @DisplayName("should throw validation exception when template has no content for channel")
        void should_throw_validation_exception_when_template_has_no_content_for_channel() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Template telegramOnlyTemplate = new Template();
            telegramOnlyTemplate.setName("telegram-only-template");
            telegramOnlyTemplate.setContent(
                    """
                        {
                            "telegram": {
                                "message": "My Number Field: [[${customfield_10000?.stringValue}]]"
                            }
                        }
                    """);
            telegramOnlyTemplate = templateRepository.save(telegramOnlyTemplate);

            Rule telegramOnlyRule = new Rule();
            telegramOnlyRule.setTemplate(telegramOnlyTemplate);
            telegramOnlyRule.setEnabled(true);
            telegramOnlyRule.setEvent(Event.ISSUE_UPDATED);
            telegramOnlyRule.setField("customfield_10000");
            Rule savedRule = ruleRepository.save(telegramOnlyRule);

            assertThatThrownBy(() ->
                    notificationService.dryRun(savedRule.getId(), "TST-1", Channel.EXPRESS))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("has no content for channel EXPRESS");
        }

        @Test
        @DisplayName("should generate dry run notifications for TELEGRAM channel")
        void should_generate_dry_run_notifications_for_telegram_channel() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Template template = new Template();
            template.setName("telegram-template");
            template.setContent(
                    """
                        {
                            "telegram": {
                                "message": "My Number Field: [[${customfield_10000?.stringValue}]]"
                            }
                        }
                    """);
            template = templateRepository.save(template);

            Rule telegramRule = new Rule();
            telegramRule.setTemplate(template);
            telegramRule.setEnabled(true);
            telegramRule.setEvent(Event.ISSUE_UPDATED);
            telegramRule.setField("customfield_10000");
            telegramRule = ruleRepository.save(telegramRule);

            List<NotificationDto> results =
                    notificationService.dryRun(telegramRule.getId(), "TST-1", Channel.TELEGRAM);
            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().rule()).isEqualTo(telegramRule.getId());
            assertThat(results.getFirst().issueKey()).isEqualTo("TST-1");
            assertThat(results.getFirst().channel()).isEqualTo(Channel.TELEGRAM);
            assertThat(results.getFirst().message()).isEqualTo("My Number Field: 1.457");
        }

        @Test
        @DisplayName("should generate dry run notifications for JIRAEMAIL channel")
        void should_generate_dry_run_notifications_for_jiraemail_channel() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Template template = new Template();
            template.setName("jiraemail-template");
            template.setContent(
                    """
                        {
                            "jira-email": {
                                "message": "My Number Field: [[${customfield_10000?.stringValue}]]"
                            }
                        }
                    """);
            template = templateRepository.save(template);

            Rule jiraEmailRule = new Rule();
            jiraEmailRule.setTemplate(template);
            jiraEmailRule.setEnabled(true);
            jiraEmailRule.setEvent(Event.ISSUE_UPDATED);
            jiraEmailRule.setField("customfield_10000");
            jiraEmailRule = ruleRepository.save(jiraEmailRule);

            List<NotificationDto> results =
                    notificationService.dryRun(jiraEmailRule.getId(), "TST-1", Channel.JIRAEMAIL);
            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().rule()).isEqualTo(jiraEmailRule.getId());
            assertThat(results.getFirst().issueKey()).isEqualTo("TST-1");
            assertThat(results.getFirst().channel()).isEqualTo(Channel.JIRAEMAIL);
            assertThat(results.getFirst().message()).isEqualTo("My Number Field: 1.457");
        }

        @Test
        @DisplayName("should generate dry run notifications for EXPRESS channel")
        void should_generate_dry_run_notifications_for_express_channel() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Template template = new Template();
            template.setName("express-template");
            template.setContent(
                    """
                        {
                            "express": {
                                "message": "My Number Field: [[${customfield_10000?.stringValue}]]"
                            }
                        }
                    """);
            template = templateRepository.save(template);

            Rule expressRule = new Rule();
            expressRule.setTemplate(template);
            expressRule.setEnabled(true);
            expressRule.setEvent(Event.ISSUE_UPDATED);
            expressRule.setField("customfield_10000");
            expressRule = ruleRepository.save(expressRule);

            List<NotificationDto> results =
                    notificationService.dryRun(expressRule.getId(), "TST-1", Channel.EXPRESS);
            assertThat(results).isNotNull();
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().rule()).isEqualTo(expressRule.getId());
            assertThat(results.getFirst().issueKey()).isEqualTo("TST-1");
            assertThat(results.getFirst().channel()).isEqualTo(Channel.EXPRESS);
            assertThat(results.getFirst().message()).isEqualTo("My Number Field: 1.457");
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when rule not found")
        void should_throw_object_not_found_exception_when_rule_not_found() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            assertThatThrownBy(() -> notificationService.dryRun(999L, "TST-1", Channel.TELEGRAM))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when variables is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_variables_is_null() {
            assertThatThrownBy(() -> notificationService.dryRun(null, "TST-1", Channel.TELEGRAM))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> notificationService.dryRun(999L, null, Channel.TELEGRAM))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> notificationService.dryRun(999L, "TST-1", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when issue not found")
        void should_throw_object_not_found_exception_when_issue_not_found() {
            doReturn(null).when(jiraIssueService).fetchJiraIssue(anyString());

            assertThatThrownBy(() -> notificationService.dryRun(rule.getId(),
                    "NONEXISTENT-1", Channel.TELEGRAM))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when issue is empty")
        void should_throw_exception_when_issue_is_empty() {
            assertThatThrownBy(() -> notificationService.dryRun(999L, "", Channel.TELEGRAM))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should return empty list when no changelog items match rule")
        void should_return_empty_list_when_no_changelog_items_match_rule() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Rule notMatchedRule = new Rule();
            notMatchedRule.setTemplate(template);
            notMatchedRule.setEnabled(false);
            notMatchedRule.setEvent(Event.ISSUE_UPDATED);
            notMatchedRule.setField("");
            notMatchedRule.setHasChanged(false);
            notMatchedRule = ruleRepository.save(notMatchedRule);

            List<NotificationDto> results =
                    notificationService.dryRun(notMatchedRule.getId(), "TST-1", Channel.TELEGRAM);
            assertThat(results).isNotNull();
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("getContext() method tests")
    class GetContextMethodTests {

        @Mock
        private DeserializationContext deserializationContext;

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
        }

        @Test
        @DisplayName("should build context for latest changelog group when historyId is null")
        void should_build_context_for_latest_changelog_group_when_history_id_is_null() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Map<String, Object> context = notificationService.getContext("TST-1", null);

            assertThat(context).isNotEmpty();
            assertThat(((TemplateField) context.get("key")).getStringValue()).isEqualTo("TST-1");
            assertThat(((TemplateField) context.get("customfield_10000")).getStringValue())
                    .isEqualTo("1.457");
        }

        @Test
        @DisplayName("should build latest field state when issue has no changelog")
        void should_build_latest_field_state_when_issue_has_no_changelog() throws IOException {
            JiraIssueDeserializer deserializer = new JiraIssueDeserializer();
            ObjectMapper objectMapper = new ObjectMapper();
            JiraIssue noChangelogIssue = deserializer.deserialize(
                    objectMapper
                            .createParser(new ClassPathResource(
                                    "json/issue/valid-issue-no-changelog.json").getFile()),
                    deserializationContext);
            doReturn(noChangelogIssue).when(jiraIssueService).fetchJiraIssue(anyString());

            Map<String, Object> context = notificationService.getContext("TST-1", null);

            assertThat(context).isNotEmpty();
            assertThat(((TemplateField) context.get("key")).getStringValue()).isEqualTo("TST-1");
            assertThat(((TemplateField) context.get("customfield_10000")).getStringValue())
                    .isEqualTo("1.457");
        }

        @Test
        @DisplayName("should build context for a specific changelog group id")
        void should_build_context_for_specific_changelog_group_id() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            Map<String, Object> context = notificationService.getContext("TST-1", 10000L);

            assertThat(context).isNotEmpty();
            assertThat(((TemplateField) context.get("key")).getStringValue()).isEqualTo("TST-1");
            assertThat(((TemplateField) context.get("customfield_10000")).getStringValue())
                    .isEmpty();
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when historyId does not exist")
        void should_throw_object_not_found_exception_when_history_id_does_not_exist() {
            doReturn(issue).when(jiraIssueService).fetchJiraIssue(anyString());

            assertThatThrownBy(() -> notificationService.getContext("TST-1", 99999L))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when issue not found")
        void should_throw_object_not_found_exception_when_issue_not_found() {
            doReturn(null).when(jiraIssueService).fetchJiraIssue(anyString());

            assertThatThrownBy(() -> notificationService.getContext("NONEXISTENT-1", null))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ValidationException when issue key is blank")
        void should_throw_validation_exception_when_issue_key_is_blank() {
            assertThatThrownBy(() -> notificationService.getContext("  ", null))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should throw exception when issue key is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_issue_key_is_null() {
            assertThatThrownBy(() -> notificationService.getContext(null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("sendSnapshotNotification() method tests")
    class SendSnapshotNotificationMethodTests {

        @Mock
        private DeserializationContext deserializationContext;

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
        }

        private JiraFilter newSnapshotFilter() {
            JiraFilter f = newFilterWithTargets(
                    "Snapshot Filter", "snap-telegram", "snap-express");
            f.setMode(FilterMode.SNAPSHOT);
            f.setTemplate(template);
            return jiraFilterRepository.save(f);
        }

        @Test
        @DisplayName("should send one notification per channel for a snapshot issue")
        void should_send_one_notification_per_channel() {
            doReturn(new ResponseEntity<>("{\"ok\":true}", HttpStatus.OK))
                    .when(telegramNotificationSender).send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                    .when(jiraEmailNotificationSender).send(any(NotificationMessageDto.class));
            doReturn(new ResponseEntity<>(HttpStatus.OK))
                    .when(expressNotificationSender).send(any(NotificationMessageDto.class));

            JiraFilter snapshotFilter = newSnapshotFilter();

            notificationService.sendSnapshotNotification(snapshotFilter, issue);

            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(3);
            assertThat(notifications).allSatisfy(notification -> {
                assertThat(notification.getChangelog()).isNull();
                assertThat(notification.getIssueKey()).isEqualTo(issue.getKey());
                assertThat(notification.getStatus()).isEqualTo(Status.SUCCESS);
            });
            assertThat(notifications).extracting(Notification::getChannel)
                    .containsExactlyInAnyOrder(
                            Channel.TELEGRAM, Channel.JIRAEMAIL, Channel.EXPRESS);
        }

        @Test
        @DisplayName("should skip a channel that has no enabled targets")
        void should_skip_channel_without_targets() {
            doReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                    .when(jiraEmailNotificationSender).send(any(NotificationMessageDto.class));

            JiraFilter snapshotFilter = new JiraFilter();
            snapshotFilter.setName("Email-only Snapshot");
            snapshotFilter.setJql("project = TST");
            snapshotFilter.setEnabled(true);
            snapshotFilter.setMode(FilterMode.SNAPSHOT);
            snapshotFilter.setTemplate(template);
            snapshotFilter = jiraFilterRepository.save(snapshotFilter);

            notificationService.sendSnapshotNotification(snapshotFilter, issue);

            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(1);
            assertThat(notifications.getFirst().getChannel()).isEqualTo(Channel.JIRAEMAIL);
            assertThat(notifications.getFirst().getChangelog()).isNull();
            assertThat(notifications.getFirst().getIssueKey()).isEqualTo(issue.getKey());
        }
    }
}