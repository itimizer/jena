package com.itimizer.jena.mapper;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.service.TemplateService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link Rule} and its DTOs, flattening
 * the linked template to its id.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TemplateService.class},
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RuleMapper {

    @Mapping(target = "template", source = "template.id")
    RuleDto toDto(Rule entity);

    @Mapping(target = "template.id", source = "template")
    Rule fromDto(RuleCreateDto dto);

    @Mapping(target = "template", ignore = true)
    void updateRuleFromDto(RuleDto dto, @MappingTarget Rule entity);
}
