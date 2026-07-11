package com.itimizer.jena.mapper;

import com.itimizer.jena.dto.NotificationTargetCreateDto;
import com.itimizer.jena.dto.NotificationTargetDto;
import com.itimizer.jena.entity.NotificationTarget;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper between {@link NotificationTarget} and its
 * DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationTargetMapper {

    NotificationTargetDto toDto(NotificationTarget entity);

    NotificationTarget fromDto(NotificationTargetCreateDto dto);

    void updateFromDto(NotificationTargetDto dto, @MappingTarget NotificationTarget entity);
}
