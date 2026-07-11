package com.itimizer.jena.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
import java.util.List;
import java.util.Map;

/**
 * A matched changelog item captured by polling (INCREMENTAL): the {@link #event}, the issue, the
 * matched {@link #rule}/{@link #filter}, and the rendered {@link #context} snapshot. A unique
 * constraint over (event, item, index, key, rule, filter) makes capture idempotent. {@link #status}
 * tracks whether it has been dispatched.
 */
@Getter
@Setter
@ToString
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uq_changelog_event_item_item_index_key_rule_id_filter_id",
        columnNames = {"event", "item", "item_index", "key", "rule_id", "filter_id"}))
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Changelog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "changelog_sequence")
    @SequenceGenerator(
            name = "changelog_sequence",
            sequenceName = "changelog_sequence",
            allocationSize = 1
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Event event;

    @Column(nullable = false)
    private long item;

    @Column(nullable = false)
    private int itemIndex;

    @Column(name = "key", nullable = false)
    private String issueKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rule_id")
    private Rule rule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "filter_id", nullable = false)
    private JiraFilter filter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> context;

    @Enumerated(EnumType.STRING)
    private Status status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @ToString.Exclude
    @OneToMany(mappedBy = "changelog", cascade = CascadeType.ALL, orphanRemoval = true)
    private transient List<Notification> notification;

    public Changelog(Map<String, Object> context, Rule rule, JiraFilter filter, String issueKey,
                     int itemIndex, long item) {
        this.context = context;
        this.rule = rule;
        this.filter = filter;
        this.issueKey = issueKey;
        this.itemIndex = itemIndex;
        this.item = item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(((Changelog) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
