package com.itimizer.jena.service;

import com.itimizer.jena.dto.NotificationTargetCreateDto;
import com.itimizer.jena.dto.NotificationTargetDto;
import com.itimizer.jena.entity.NotificationTarget;
import lombok.NonNull;

import java.util.List;

/**
 * CRUD for {@link NotificationTarget}s — the reusable chat
 * definitions ({@code name}, {@code channel}, {@code chatId}) that filters route to.
 */
public interface NotificationTargetService {

    NotificationTargetDto create(@NonNull NotificationTargetCreateDto dto);

    NotificationTargetDto update(@NonNull NotificationTargetDto dto);

    NotificationTargetDto get(@NonNull Long id);

    List<NotificationTargetDto> list();

    void delete(@NonNull Long id);
}