package com.itimizer.jena.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A reusable chat/recipient: {@code name}, {@code channel} and {@code chatId} (stored verbatim,
 * including any sign/prefix). Attached to many filters; {@code (channel, chatId)} is unique.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@Table(name = "notification_targets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notification_targets_channel_chat",
                columnNames = {"channel", "chat_id"}))
@EntityListeners(AuditingEntityListener.class)
public class NotificationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_target_sequence")
    @SequenceGenerator(
            name = "notification_target_sequence",
            sequenceName = "notification_target_sequence",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Channel channel;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(nullable = false)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(((NotificationTarget) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}