package com.itimizer.jena.entity;

import com.itimizer.jena.notification.NotificationSender;

/**
 * Supported notification channels. A template carries content per channel, and each maps to one
 * {@link NotificationSender}.
 */
public enum Channel {
    /** Telegram chat message. */
    TELEGRAM,
    /**
     * Email sent through Jira's {@code issue/{key}/notify} endpoint (no chat target — notifies
     * watchers).
     */
    JIRAEMAIL,
    /** Express chat message. */
    EXPRESS
}
