package com.itimizer.jena.service.impl;

import com.itimizer.jena.dto.JiraFilterCreateDto;
import com.itimizer.jena.dto.JiraFilterDto;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.event.JiraFilterChangedEvent;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.mapper.JiraFilterMapper;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.NotificationTargetRepository;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import com.itimizer.jena.service.JqlValidator;
import com.itimizer.jena.transactionalmanager.TransactionAction;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraFilterServiceImpl Tests")
class JiraFilterServiceImplTest {

    @Mock
    private JiraFilterRepository jiraFilterRepository;
    @Mock
    private NotificationTargetRepository notificationTargetRepository;
    @Mock
    private RuleRepository ruleRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private JiraFilterMapper jiraFilterMapper;
    @Mock
    private JqlValidator jqlValidator;
    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JiraFilterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JiraFilterServiceImpl(
                jiraFilterRepository, notificationTargetRepository, ruleRepository,
                templateRepository, jiraFilterMapper, jqlValidator, transactionRunner,
                eventPublisher);
    }

    @SuppressWarnings("unchecked")
    private void runTransactionsInline() {
        lenient().when(transactionRunner.doInTransaction(any()))
                .thenAnswer(invocation ->
                        ((TransactionAction<Object>) invocation.getArgument(0)).get());
    }

    private JiraFilterCreateDto createDto(FilterMode mode, Long templateId,
                                          String cron, String zone) {
        return new JiraFilterCreateDto(
                "Filter", "project = TST", mode, templateId, cron, zone, true);
    }

    @Test
    @DisplayName("create should reject a SNAPSHOT filter without a templateId")
    void create_snapshot_without_template() {
        assertThatThrownBy(() -> service.create(createDto(FilterMode.SNAPSHOT, null, null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("create should reject an INCREMENTAL filter with a templateId")
    void create_incremental_with_template() {
        assertThatThrownBy(() -> service.create(createDto(FilterMode.INCREMENTAL, 5L, null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("create should reject an INCREMENTAL filter with a scheduleCron")
    void create_incremental_with_cron() {
        assertThatThrownBy(() ->
                service.create(createDto(FilterMode.INCREMENTAL, null, "0 0 8 * * *", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("create should reject a SNAPSHOT filter with an invalid cron")
    void create_snapshot_invalid_cron() {
        assertThatThrownBy(() ->
                service.create(createDto(FilterMode.SNAPSHOT, 5L, "not-a-cron", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("create should reject a SNAPSHOT filter with an invalid zone")
    void create_snapshot_invalid_zone() {
        assertThatThrownBy(() ->
                service.create(createDto(FilterMode.SNAPSHOT, 5L, "0 0 8 * * *", "Bad/Zone")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("create should fail when the SNAPSHOT template does not exist")
    void create_snapshot_missing_template() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.create(createDto(FilterMode.SNAPSHOT, 99L, "0 0 8 * * *", "UTC")))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    @DisplayName("create should resolve the template and publish a change event")
    void create_snapshot_publishes_event() {
        Template template = new Template();
        template.setId(5L);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));

        JiraFilter mapped = new JiraFilter();
        when(jiraFilterMapper.fromDto(any())).thenReturn(mapped);
        JiraFilter saved = new JiraFilter();
        saved.setId(7L);
        when(jiraFilterRepository.save(mapped)).thenReturn(saved);
        runTransactionsInline();

        service.create(createDto(FilterMode.SNAPSHOT, 5L, "0 0 8 * * *", "UTC"));

        verify(eventPublisher).publishEvent(new JiraFilterChangedEvent(7L));
    }

    @Test
    @DisplayName("attachRule should reject attaching a rule to a SNAPSHOT filter")
    void attach_rule_to_snapshot() {
        JiraFilter snapshot = new JiraFilter();
        snapshot.setId(1L);
        snapshot.setMode(FilterMode.SNAPSHOT);
        when(jiraFilterRepository.findById(1L)).thenReturn(Optional.of(snapshot));
        runTransactionsInline();

        assertThatThrownBy(() -> service.attachRule(1L, 2L))
                .isInstanceOf(ValidationException.class);
        verify(ruleRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("update should reject switching a rule-bearing filter to SNAPSHOT")
    void update_switch_to_snapshot_with_rules() {
        Template template = new Template();
        template.setId(5L);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));

        JiraFilter existing = new JiraFilter();
        existing.setId(1L);
        existing.setMode(FilterMode.INCREMENTAL);
        existing.getRules().add(new Rule());
        when(jiraFilterRepository.findById(1L)).thenReturn(Optional.of(existing));
        runTransactionsInline();

        var dto = new JiraFilterDto(
                1L, "Filter", "project = TST", FilterMode.SNAPSHOT, 5L, "0 0 8 * * *", "UTC",
                true, null, null);

        assertThatThrownBy(() -> service.update(dto))
                .isInstanceOf(ValidationException.class);
    }
}
