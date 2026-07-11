package com.itimizer.jena.service.impl;

import com.itimizer.jena.dto.NotificationTargetCreateDto;
import com.itimizer.jena.dto.NotificationTargetDto;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.mapper.NotificationTargetMapper;
import com.itimizer.jena.repository.NotificationTargetRepository;
import com.itimizer.jena.service.NotificationTargetService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link NotificationTargetService} backed by JPA — straightforward CRUD over reusable chat
 * targets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTargetServiceImpl implements NotificationTargetService {

    private final NotificationTargetRepository notificationTargetRepository;
    private final NotificationTargetMapper notificationTargetMapper;
    private final TransactionRunner transactionRunner;

    @Override
    public NotificationTargetDto create(@NonNull NotificationTargetCreateDto dto) {
        var target = notificationTargetMapper.fromDto(dto);
        return notificationTargetMapper.toDto(transactionRunner.doInTransaction(() ->
                notificationTargetRepository.save(target)));
    }

    @Override
    public NotificationTargetDto update(@NonNull NotificationTargetDto dto) {
        return notificationTargetMapper.toDto(transactionRunner.doInTransaction(() -> {
            var target = notificationTargetRepository.findById(dto.id()).orElse(null);

            if (target == null) {
                throw new ObjectNotFoundException("Notification target with id "
                        + dto.id() + " not found.");
            }
            notificationTargetMapper.updateFromDto(dto, target);
            return notificationTargetRepository.save(target);
        }));
    }

    @Override
    public NotificationTargetDto get(@NonNull Long id) {
        var target = notificationTargetRepository.findById(id).orElse(null);

        if (target == null) {
            throw new ObjectNotFoundException("Notification target with id "
                    + id + " not found.");
        }
        return notificationTargetMapper.toDto(target);
    }

    @Override
    public List<NotificationTargetDto> list() {
        return notificationTargetRepository.findAll().stream()
                .map(notificationTargetMapper::toDto)
                .toList();
    }

    @Override
    public void delete(@NonNull Long id) {
        transactionRunner.doInTransaction(() -> {
            if (!notificationTargetRepository.existsById(id)) {
                throw new ObjectNotFoundException("Notification target with id "
                        + id + " not found.");
            }
            notificationTargetRepository.deleteById(id);
            return null;
        });
    }
}
