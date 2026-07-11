package com.itimizer.jena.controller;

import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.TemplateService;
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
 * REST CRUD API for templates under {@code /templates}. On update the path id must match the body
 * id. Thin adapter over {@link TemplateService}.
 */
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDto create(@RequestBody @Valid TemplateCreateDto templateCreateDto) {
        return templateService.create(templateCreateDto);
    }

    @PutMapping("/{id}")
    public TemplateDto update(@PathVariable Long id, @RequestBody @Valid TemplateDto templateDto) {
        if (!id.equals(templateDto.id())) {
            throw new ValidationException("Path id %d does not match body id %d."
                    .formatted(id, templateDto.id()));
        }
        return templateService.update(templateDto);
    }

    @GetMapping("/{id}")
    public TemplateDto get(@PathVariable Long id) {
        return templateService.get(id);
    }

    @GetMapping
    public List<TemplateDto> list() {
        return templateService.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        templateService.delete(id);
    }
}
