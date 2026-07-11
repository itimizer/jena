package com.itimizer.jena.service;

import com.itimizer.jena.dto.JiraFilterCreateDto;
import com.itimizer.jena.dto.JiraFilterDto;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import lombok.NonNull;

import java.util.List;

/**
 * Manages {@link JiraFilter}s — the named JQL definitions that drive both polling modes. Create and
 * update validate the JQL against Jira and enforce the per-mode invariants (INCREMENTAL carries
 * rules; SNAPSHOT carries a template and cron). {@code get}/{@code list} return DTOs for the API,
 * while {@code fetch}/{@code findEnabled*} return entities for internal callers.
 */
public interface JiraFilterService {

    JiraFilterDto create(@NonNull JiraFilterCreateDto dto);

    JiraFilterDto update(@NonNull JiraFilterDto dto);

    JiraFilterDto get(@NonNull Long id);

    List<JiraFilterDto> list();

    void delete(@NonNull Long id);

    void attachTarget(@NonNull Long filterId, @NonNull Long targetId);

    void detachTarget(@NonNull Long filterId, @NonNull Long targetId);

    /** Links a rule to a filter; rejected for SNAPSHOT filters, which have no rules. */
    void attachRule(@NonNull Long filterId, @NonNull Long ruleId);

    void detachRule(@NonNull Long filterId, @NonNull Long ruleId);

    /** Enabled filters in the given mode — the working set the schedulers iterate over. */
    List<JiraFilter> findEnabled(@NonNull FilterMode mode);

    /**
     * The filter only if it is currently enabled and still in SNAPSHOT mode, otherwise
     * {@code null}. A digest run calls this to re-check live state before firing, since its trigger
     * may outlive an edit that disabled or converted the filter.
     */
    JiraFilter findEnabledSnapshot(long id);

    /** Loads the entity by id or throws if absent; the internal counterpart to {@link #get}. */
    JiraFilter fetch(@NonNull Long id);
}
