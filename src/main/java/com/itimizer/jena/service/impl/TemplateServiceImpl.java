package com.itimizer.jena.service.impl;

import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.mapper.TemplateMapper;
import com.itimizer.jena.repository.TemplateRepository;
import com.itimizer.jena.service.TemplateService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

/**
 * {@link TemplateService} backed by JPA, with rendering delegated to the configured Thymeleaf
 * {@link TemplateEngine} (which carries JENA's custom Jira-field dialect).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final TransactionRunner transactionRunner;
    private final TemplateEngine templateEngine;

    @Override
    public TemplateDto create(@NonNull TemplateCreateDto dto) {
        var template = templateMapper.fromDto(dto);

        return templateMapper.toDto(transactionRunner.doInTransaction(() ->
                templateRepository.save(template)));
    }

    @Override
    public TemplateDto update(@NonNull TemplateDto dto) {
        return templateMapper.toDto(transactionRunner.doInTransaction(() -> {
            var template = templateRepository.findById(dto.id()).orElse(null);

            if (template == null) {
                throw new ObjectNotFoundException("Notification template with id "
                        + dto.id() + " not found.");
            }
            templateMapper.updateTemplateFromDto(dto, template);
            return templateRepository.save(template);
        })
        );
    }

    @Override
    public TemplateDto get(@NonNull Long id) {
        var template = templateRepository.findById(id).orElse(null);

        if (template == null) {
            throw new ObjectNotFoundException("Notification template with id "
                    + id + " not found.");
        }
        return templateMapper.toDto(template);
    }

    @Override
    public List<TemplateDto> list() {
        return templateRepository.findAll().stream()
                .map(templateMapper::toDto)
                .toList();
    }

    @Override
    public void delete(@NonNull Long id) {
        transactionRunner.doInTransaction(() -> {
            if (!templateRepository.existsById(id)) {
                throw new ObjectNotFoundException("Notification template with id "
                        + id + " not found.");
            }
            templateRepository.deleteById(id);
            return null;
        });
    }

    @Override
    public Template fetch(@NonNull Long id) {
        return templateRepository.findById(id).orElse(null);
    }

    public String getContent(@NonNull String template, @NonNull Map<String, Object> attr) {
        var context = new Context();
        context.setVariables(attr);
        return templateEngine.process(template, context);
    }
}
