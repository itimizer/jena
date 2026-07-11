package com.itimizer.jena.service;

import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
import com.itimizer.jena.entity.Template;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Manages {@link Template}s (the per-channel message bodies) and renders them. {@code get}/
 * {@code list} return DTOs for the API; {@code fetch} returns the entity for internal callers.
 */
public interface TemplateService {

    TemplateDto create(@NonNull TemplateCreateDto dto);

    TemplateDto update(@NonNull TemplateDto dto);

    TemplateDto get(@NonNull Long id);

    List<TemplateDto> list();

    void delete(@NonNull Long id);

    /** Loads the entity by id or throws if absent; the internal counterpart to {@link #get}. */
    Template fetch(@NonNull Long id);

    /**
     * Renders a raw template string through Thymeleaf with the given field context, returning the
     * final message text.
     *
     * @param template raw template body for one channel
     * @param attr     field context exposed to the template (issue fields, {@code baseUrl}, …)
     */
    String getContent(@NonNull String template, @NonNull Map<String, Object> attr);
}
