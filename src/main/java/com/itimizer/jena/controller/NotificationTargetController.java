package com.itimizer.jena.controller;

import com.itimizer.jena.dto.NotificationTargetCreateDto;
import com.itimizer.jena.dto.NotificationTargetDto;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.NotificationTargetService;
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
 * REST CRUD API for reusable notification targets under {@code /targets}. Thin adapter over
 * {@link NotificationTargetService}.
 */
@RestController
@RequestMapping("/targets")
@RequiredArgsConstructor
public class NotificationTargetController {

    private final NotificationTargetService notificationTargetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationTargetDto create(
            @RequestBody @Valid NotificationTargetCreateDto notificationTargetCreateDto) {
        return notificationTargetService.create(notificationTargetCreateDto);
    }

    @PutMapping("/{id}")
    public NotificationTargetDto update(@PathVariable Long id,
                                        @RequestBody @Valid NotificationTargetDto dto) {
        if (!id.equals(dto.id())) {
            throw new ValidationException("Path id %d does not match body id %d."
                    .formatted(id, dto.id()));
        }
        return notificationTargetService.update(dto);
    }

    @GetMapping("/{id}")
    public NotificationTargetDto get(@PathVariable Long id) {
        return notificationTargetService.get(id);
    }

    @GetMapping
    public List<NotificationTargetDto> list() {
        return notificationTargetService.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        notificationTargetService.delete(id);
    }
}