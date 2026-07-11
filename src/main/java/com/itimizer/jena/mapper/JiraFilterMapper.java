package com.itimizer.jena.mapper;

import com.itimizer.jena.dto.JiraFilterCreateDto;
import com.itimizer.jena.dto.JiraFilterDto;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.NotificationTarget;
import com.itimizer.jena.entity.Rule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

/**
 * MapStruct mapper between {@link JiraFilter} and its DTOs,
 * exposing attached targets and rules as id lists.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JiraFilterMapper {

    @Mapping(target = "targetIds", source = "targets")
    @Mapping(target = "ruleIds", source = "rules")
    @Mapping(target = "templateId", source = "template.id")
    JiraFilterDto toDto(JiraFilter entity);

    @Mapping(target = "template", ignore = true)
    JiraFilter fromDto(JiraFilterCreateDto dto);

    @Mapping(target = "targets", ignore = true)
    @Mapping(target = "rules", ignore = true)
    @Mapping(target = "template", ignore = true)
    void updateFromDto(JiraFilterDto dto, @MappingTarget JiraFilter entity);

    default List<Long> targetIds(Set<NotificationTarget> targets) {
        return targets == null ? List.of()
                : targets.stream().map(NotificationTarget::getId).sorted().toList();
    }

    default List<Long> ruleIds(Set<Rule> rules) {
        return rules == null ? List.of()
                : rules.stream().map(Rule::getId).sorted().toList();
    }
}