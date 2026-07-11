package com.itimizer.jena.service.impl;

import com.itimizer.jena.dto.JiraFilterCreateDto;
import com.itimizer.jena.dto.JiraFilterDto;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.event.JiraFilterChangedEvent;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.mapper.JiraFilterMapper;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.NotificationTargetRepository;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import com.itimizer.jena.service.JiraFilterService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link JiraFilterService} backed by JPA. Enforces the per-mode invariants (SNAPSHOT requires a
 * template and a valid cron/zone; INCREMENTAL forbids them and owns rules) and validates JQL
 * against Jira before persisting. Every create/update/delete publishes a
 * {@link JiraFilterChangedEvent} so the snapshot scheduler can (re)register or cancel the filter's
 * cron trigger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraFilterServiceImpl implements JiraFilterService {

    private final JiraFilterRepository jiraFilterRepository;
    private final NotificationTargetRepository notificationTargetRepository;
    private final RuleRepository ruleRepository;
    private final TemplateRepository templateRepository;
    private final JiraFilterMapper jiraFilterMapper;
    private final JiraJqlValidator jiraJqlValidator;
    private final TransactionRunner transactionRunner;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public JiraFilterDto create(@NonNull JiraFilterCreateDto dto) {
        jiraJqlValidator.validate(dto.jql());
        var template = validateAndResolveTemplate(dto.mode(), dto.templateId(),
                dto.scheduleCron(), dto.scheduleZone());
        var filter = jiraFilterMapper.fromDto(dto);
        filter.setTemplate(template);

        return jiraFilterMapper.toDto(transactionRunner.doInTransaction(() -> {
            var saved = jiraFilterRepository.save(filter);
            eventPublisher.publishEvent(new JiraFilterChangedEvent(saved.getId()));
            return saved;
        }));
    }

    @Override
    public JiraFilterDto update(@NonNull JiraFilterDto dto) {
        jiraJqlValidator.validate(dto.jql());
        var template = validateAndResolveTemplate(dto.mode(), dto.templateId(),
                dto.scheduleCron(), dto.scheduleZone());

        return jiraFilterMapper.toDto(transactionRunner.doInTransaction(() -> {
            var filter = jiraFilterRepository.findById(dto.id()).orElse(null);

            if (filter == null) {
                throw new ObjectNotFoundException("Jira filter with id "
                        + dto.id() + " not found.");
            }
            if (dto.mode() == FilterMode.SNAPSHOT && !filter.getRules().isEmpty()) {
                throw new ValidationException(
                        "Cannot switch a filter with attached rules to SNAPSHOT mode.");
            }
            jiraFilterMapper.updateFromDto(dto, filter);
            filter.setTemplate(template);
            var saved = jiraFilterRepository.save(filter);
            eventPublisher.publishEvent(new JiraFilterChangedEvent(saved.getId()));
            return saved;
        }));
    }

    @Override
    public JiraFilterDto get(@NonNull Long id) {
        var filter = jiraFilterRepository.findById(id).orElse(null);

        if (filter == null) {
            throw new ObjectNotFoundException("Jira filter with id " + id + " not found.");
        }
        return jiraFilterMapper.toDto(filter);
    }

    @Override
    public List<JiraFilterDto> list() {
        return jiraFilterRepository.findAll().stream()
                .map(jiraFilterMapper::toDto)
                .toList();
    }

    @Override
    public void delete(@NonNull Long id) {
        transactionRunner.doInTransaction(() -> {
            if (!jiraFilterRepository.existsById(id)) {
                throw new ObjectNotFoundException("Jira filter with id " + id + " not found.");
            }
            jiraFilterRepository.deleteById(id);
            eventPublisher.publishEvent(new JiraFilterChangedEvent(id));
            return null;
        });
    }

    @Override
    public void attachTarget(@NonNull Long filterId, @NonNull Long targetId) {
        transactionRunner.doInTransaction(() -> {
            var filter = jiraFilterRepository.findById(filterId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Jira filter with id " + filterId + " not found."));
            var target = notificationTargetRepository.findById(targetId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Notification target with id " + targetId + " not found."));
            filter.getTargets().add(target);
            return jiraFilterRepository.save(filter);
        });
    }

    @Override
    public void detachTarget(@NonNull Long filterId, @NonNull Long targetId) {
        transactionRunner.doInTransaction(() -> {
            var filter = jiraFilterRepository.findById(filterId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Jira filter with id " + filterId + " not found."));
            filter.getTargets().removeIf(t -> t.getId().equals(targetId));
            return jiraFilterRepository.save(filter);
        });
    }

    @Override
    public void attachRule(@NonNull Long filterId, @NonNull Long ruleId) {
        transactionRunner.doInTransaction(() -> {
            var filter = jiraFilterRepository.findById(filterId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Jira filter with id " + filterId + " not found."));

            if (filter.getMode() == FilterMode.SNAPSHOT) {
                throw new ValidationException(
                        "Cannot attach a rule to a SNAPSHOT filter.");
            }
            var rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Notification rule with id " + ruleId + " not found."));
            rule.getFilters().add(filter);
            return ruleRepository.save(rule);
        });
    }

    @Override
    public void detachRule(@NonNull Long filterId, @NonNull Long ruleId) {
        transactionRunner.doInTransaction(() -> {
            var rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Notification rule with id " + ruleId + " not found."));
            rule.getFilters().removeIf(f -> f.getId().equals(filterId));
            return ruleRepository.save(rule);
        });
    }

    @Override
    public List<JiraFilter> findEnabled(@NonNull FilterMode mode) {
        return jiraFilterRepository.findByModeAndEnabledTrue(mode);
    }

    @Override
    public JiraFilter findEnabledSnapshot(long id) {
        return jiraFilterRepository
                .findByIdAndModeAndEnabledTrue(id, FilterMode.SNAPSHOT)
                .orElse(null);
    }

    @Override
    public JiraFilter fetch(@NonNull Long id) {
        return jiraFilterRepository.findById(id).orElse(null);
    }

    /**
     * Enforces the mode contract and returns the resolved template (or {@code null} for
     * INCREMENTAL): SNAPSHOT requires a template plus a valid cron/zone; INCREMENTAL must carry
     * neither a template nor a schedule.
     *
     * @throws ValidationException if the mode-specific fields are missing, disallowed, or malformed
     */
    private Template validateAndResolveTemplate(@NonNull FilterMode mode, Long templateId,
                                                String scheduleCron, String scheduleZone) {
        if (mode == FilterMode.SNAPSHOT) {
            if (templateId == null) {
                throw new ValidationException("templateId is required for SNAPSHOT filters.");
            }
            validateCron(scheduleCron);
            validateZone(scheduleZone);
            return templateRepository.findById(templateId)
                    .orElseThrow(() -> new ObjectNotFoundException(
                            "Template with id " + templateId + " not found."));
        }
        if (templateId != null) {
            throw new ValidationException("templateId must be null for INCREMENTAL filters.");
        }
        if (scheduleCron != null) {
            throw new ValidationException("scheduleCron is not allowed for INCREMENTAL filters.");
        }
        return null;
    }

    private void validateCron(String cron) {
        if (cron != null && !CronExpression.isValidExpression(cron)) {
            throw new ValidationException("Invalid scheduleCron expression: " + cron);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void validateZone(String zone) {
        if (zone == null) {
            return;
        }
        try {
            ZoneId.of(zone);
        } catch (DateTimeException e) {
            throw new ValidationException("Invalid scheduleZone: " + zone);
        }
    }
}
