package com.itimizer.jena.mapper;

import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.dto.ChangelogDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.service.ChangelogService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper between {@link Changelog} and its DTOs,
 * flattening the linked rule to its id.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ChangelogService.class},
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ChangelogMapper {

    @Mapping(target = "rule", source = "rule.id")
    ChangelogDto toDto(Changelog entity);

    @Mapping(target = "rule.id", source = "rule")
    @Mapping(target = "filter.id", source = "filter")
    Changelog fromDto(ChangelogCreateDto dto);

    @Mapping(target = "rule.id", source = "rule")
    void updateChangelogFromDto(ChangelogDto dto, @MappingTarget Changelog entity);
}
