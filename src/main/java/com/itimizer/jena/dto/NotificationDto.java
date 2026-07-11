package com.itimizer.jena.dto;

import com.itimizer.jena.entity.Channel;

/** A rendered notification preview returned by the dry-run endpoint. */
public record NotificationDto(
        Long rule,
        Long template,
        Channel channel,
        String issueKey,
        Long item,
        int itemIndex,
        String message) {
}
