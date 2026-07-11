package com.itimizer.jena.mapper;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.dto.TemplateContentDto;
import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
import com.itimizer.jena.entity.Template;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link Template} and its DTOs. Also
 * converts the per-channel {@code content} between its JSON string column and the typed
 * {@code TemplateContentDto}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface TemplateMapper {

    TemplateDto toDto(Template entity);

    Template fromDto(TemplateCreateDto dto);

    void updateTemplateFromDto(TemplateDto dto, @MappingTarget Template entity);

    default String convertContentToEntity(TemplateContentDto dto) throws JacksonException {
        var objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(dto);
    }

    default TemplateContentDto convertContentToDto(String content) throws JacksonException {
        var objectMapper = new ObjectMapper();

        return objectMapper.readValue(content, TemplateContentDto.class);
    }
}
