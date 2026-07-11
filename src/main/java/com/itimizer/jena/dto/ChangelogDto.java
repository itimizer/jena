package com.itimizer.jena.dto;

import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Status;

import java.util.Map;

/** API representation of a {@link Changelog}. */
public record ChangelogDto(
        Long id,
        Event event,
        Long item,
        int itemIndex,
        String issueKey,
        Long rule,
        Map<String, Object> context,
        Status status) {
}
