package com.itimizer.jena.controller;

import com.itimizer.jena.dto.JiraFilterCreateDto;
import com.itimizer.jena.dto.JiraFilterDto;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.JiraFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for filters under {@code /filters}: CRUD plus the wiring endpoints that attach/detach
 * targets and rules. On update the path id must match the body id. Thin adapter over
 * {@link JiraFilterService}.
 */
@RestController
@RequestMapping("/filters")
@RequiredArgsConstructor
public class JiraFilterController {

    private final JiraFilterService jiraFilterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JiraFilterDto create(@RequestBody @Valid JiraFilterCreateDto jiraFilterCreateDto) {
        return jiraFilterService.create(jiraFilterCreateDto);
    }

    @PutMapping("/{id}")
    public JiraFilterDto update(@PathVariable Long id, @RequestBody @Valid JiraFilterDto dto) {
        if (!id.equals(dto.id())) {
            throw new ValidationException("Path id %d does not match body id %d."
                    .formatted(id, dto.id()));
        }
        return jiraFilterService.update(dto);
    }

    @GetMapping("/{id}")
    public JiraFilterDto get(@PathVariable Long id) {
        return jiraFilterService.get(id);
    }

    @GetMapping
    public List<JiraFilterDto> list() {
        return jiraFilterService.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        jiraFilterService.delete(id);
    }

    @PutMapping("/{id}/targets/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachTarget(@PathVariable Long id, @PathVariable Long targetId) {
        jiraFilterService.attachTarget(id, targetId);
    }

    @DeleteMapping("/{id}/targets/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachTarget(@PathVariable Long id, @PathVariable Long targetId) {
        jiraFilterService.detachTarget(id, targetId);
    }

    @PutMapping("/{id}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachRule(@PathVariable Long id, @PathVariable Long ruleId) {
        jiraFilterService.attachRule(id, ruleId);
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachRule(@PathVariable Long id, @PathVariable Long ruleId) {
        jiraFilterService.detachRule(id, ruleId);
    }
}