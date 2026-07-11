package com.itimizer.jena.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-filter polling cursor (INCREMENTAL). {@link #lastRunTime} records every attempt, while
 * {@link #lastSuccessRunTime} is the watermark — the start of the next {@code updated >=} window —
 * and advances only on success. One row per filter ({@code filter_id} unique). SNAPSHOT filters
 * keep none.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class JiraSearchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jira_search_run_sequence")
    @SequenceGenerator(
            name = "jira_search_run_sequence",
            sequenceName = "jira_search_run_sequence",
            allocationSize = 1
    )
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filter_id", nullable = false, unique = true)
    private JiraFilter filter;

    @Column(nullable = false)
    private String baseUrl;

    @Column(nullable = false)
    private LocalDateTime lastRunTime;

    private LocalDateTime lastSuccessRunTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    int failureCount;

}
