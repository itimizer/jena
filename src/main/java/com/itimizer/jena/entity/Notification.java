package com.itimizer.jena.entity;

import com.itimizer.jena.dto.NotificationRecipientDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A queued or sent message. {@link #changelog} is set for INCREMENTAL notifications and
 * {@code null} for SNAPSHOT ones (audit-only, excluded from retry). {@link #target} is the channel
 * address payload; {@link #status} and {@link #failureCount} drive dispatch and retry.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_sequence")
    @SequenceGenerator(
            name = "notification_sequence",
            sequenceName = "notification_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changelog_id")
    private Changelog changelog;

    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private NotificationRecipientDto target;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    int failureCount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Notification(String issueKey, Changelog changelog, Channel channel,
                        NotificationRecipientDto target, String content, Status status,
                        int failureCount) {
        this.issueKey = issueKey;
        this.changelog = changelog;
        this.channel = channel;
        this.target = target;
        this.content = content;
        this.status = status;
        this.failureCount = failureCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(((Notification) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
