package com.itimizer.jena.entity;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
import java.util.HashSet;
import java.util.Set;

/**
 * A named JQL filter — the unit of polling for both modes. INCREMENTAL filters own {@link #rules}
 * and leave {@link #template}/{@link #scheduleCron}/{@link #scheduleZone} null; SNAPSHOT filters
 * carry a template and cron and have no rules. {@link #targets} are the chats it routes to. A DB
 * check constraint enforces these per-mode invariants.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@Table(name = "jira_filters",
        uniqueConstraints = @UniqueConstraint(name = "uq_jira_filters_name", columnNames = "name"))
@EntityListeners(AuditingEntityListener.class)
public class JiraFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jira_filter_sequence")
    @SequenceGenerator(
            name = "jira_filter_sequence",
            sequenceName = "jira_filter_sequence",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4000)
    private String jql;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FilterMode mode = FilterMode.INCREMENTAL;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private Template template;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "schedule_zone")
    private String scheduleZone;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "filter_targets",
            joinColumns = @JoinColumn(name = "filter_id"),
            inverseJoinColumns = @JoinColumn(name = "target_id"))
    private Set<NotificationTarget> targets = new HashSet<>();

    @ToString.Exclude
    @ManyToMany(mappedBy = "filters", fetch = FetchType.EAGER)
    private Set<Rule> rules = new HashSet<>();

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
        return id != null && id.equals(((JiraFilter) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
