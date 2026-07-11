package com.itimizer.jena.service.impl;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.mapper.RuleMapper;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.service.TemplateService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link RuleService} backed by JPA. Create/update verify the referenced template exists; the
 * {@code findRulesBy*} queries scope to a filter's linked rules and back the poller's matching
 * engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final RuleMapper ruleMapper;
    private final TemplateService templateService;
    private final TransactionRunner transactionRunner;

    @Override
    public RuleDto create(@NonNull RuleCreateDto dto) {
        if (templateService.fetch(dto.template()) == null) {
            throw new ObjectNotFoundException("Notification template with id "
                    + dto.template() + " not found.");
        }
        var rule = ruleMapper.fromDto(dto);
        return ruleMapper.toDto(transactionRunner.doInTransaction(() ->
                ruleRepository.save(rule)));
    }

    @Override
    public RuleDto update(@NonNull RuleDto dto) {
        var template = templateService.fetch(dto.template());
        if (template == null) {
            throw new ObjectNotFoundException("Notification template with id "
                    + dto.template() + " not found.");
        }
        return ruleMapper.toDto(transactionRunner.doInTransaction(() -> {
            var rule = ruleRepository.findById(dto.id()).orElse(null);

            if (rule == null) {
                throw new ObjectNotFoundException("Notification rule with id "
                        + dto.id() + " not found.");
            }
            ruleMapper.updateRuleFromDto(dto, rule);
            rule.setTemplate(template);
            return ruleRepository.save(rule);
        })
        );
    }

    @Override
    public RuleDto get(@NonNull Long id) {
        var rule = ruleRepository.findById(id).orElse(null);

        if (rule == null) {
            throw new ObjectNotFoundException("Notification rule with id "
                    + id + " not found.");
        }
        return ruleMapper.toDto(rule);
    }

    @Override
    public List<RuleDto> list() {
        return ruleRepository.findAll().stream()
                .map(ruleMapper::toDto)
                .toList();
    }

    @Override
    public void delete(@NonNull Long id) {
        transactionRunner.doInTransaction(() -> {
            if (!ruleRepository.existsById(id)) {
                throw new ObjectNotFoundException("Notification rule with id "
                        + id + " not found.");
            }
            ruleRepository.deleteById(id);
            return null;
        });
    }

    @Override
    public Rule fetch(@NonNull Long id) {
        return ruleRepository.findById(id).orElse(null);
    }

    @Override
    public boolean hasEnabledRules(@NonNull Long filterId) {
        return ruleRepository.existsByFiltersIdAndEnabledTrue(filterId);
    }

    @Override
    public List<Rule> findRulesByEvent(@NonNull Long filterId, boolean enabled,
                                       @NonNull Event event) {
        return ruleRepository.findByFiltersIdAndEnabledAndEvent(filterId, enabled, event);
    }

    public List<Rule> findRulesByField(@NonNull Long filterId, @NonNull String field,
                                       boolean enabled, @NonNull Event event) {
        return ruleRepository.findByFiltersIdAndFieldAndEnabledAndEvent(
                filterId, field, enabled, event);
    }
}
