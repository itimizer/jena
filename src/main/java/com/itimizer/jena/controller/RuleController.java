package com.itimizer.jena.controller;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.RuleService;
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
 * REST CRUD API for rules under {@code /rules}. On update the path id must match the body id. Thin
 * adapter over {@link RuleService}.
 */
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleDto create(@RequestBody @Valid RuleCreateDto ruleCreateDto) {
        return ruleService.create(ruleCreateDto);
    }

    @PutMapping("/{id}")
    public RuleDto update(@PathVariable Long id, @RequestBody @Valid RuleDto ruleDto) {
        if (!id.equals(ruleDto.id())) {
            throw new ValidationException("Path id %d does not match body id %d."
                    .formatted(id, ruleDto.id()));
        }
        return ruleService.update(ruleDto);
    }

    @GetMapping("/{id}")
    public RuleDto get(@PathVariable Long id) {
        return ruleService.get(id);
    }

    @GetMapping
    public List<RuleDto> list() {
        return ruleService.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }
}