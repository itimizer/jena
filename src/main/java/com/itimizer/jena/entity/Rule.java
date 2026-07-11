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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
 * A notification rule (INCREMENTAL only): the match conditions against a changelog item plus the
 * {@link #template} to render. {@link #field}, {@link #from}/{@link #fromStr}, {@link #to}/
 * {@link #toStr} and {@link #hasChanged} define the match for {@link Event#ISSUE_UPDATED};
 * from/fromStr/to/toStr are regex (null = any). Linked to filters many-to-many via
 * {@code rule_filters}.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@Table(name = "rules", indexes = @Index(name = "idx_rules_field", columnList = "field"))
@EntityListeners(AuditingEntityListener.class)
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_sequence")
    @SequenceGenerator(
            name = "rule_sequence",
            sequenceName = "rule_sequence",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private Template template;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Event event;

    private String field;

    @Column(name = "`from`")
    private String from;

    @Column(name = "from_string")
    private String fromStr;

    @Column(name = "`to`")
    private String to;

    @Column(name = "to_string")
    private String toStr;

    @Column(nullable = false)
    private boolean hasChanged;

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

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rule_filters",
            joinColumns = @JoinColumn(name = "rule_id"),
            inverseJoinColumns = @JoinColumn(name = "filter_id"))
    private Set<JiraFilter> filters = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(((Rule) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
