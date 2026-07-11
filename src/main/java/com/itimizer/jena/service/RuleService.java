package com.itimizer.jena.service;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.Rule;
import lombok.NonNull;

import java.util.List;

/**
 * Manages {@link Rule}s and the lookups the poller uses to match them against changelog items.
 * {@code get}/{@code list} return DTOs for the API; {@code fetch} and the {@code findRulesBy*}
 * queries return entities for the matching engine.
 */
public interface RuleService {

    RuleDto create(@NonNull RuleCreateDto dto);

    RuleDto update(@NonNull RuleDto dto);

    RuleDto get(@NonNull Long id);

    List<RuleDto> list();

    void delete(@NonNull Long id);

    /** Loads the entity by id or throws if absent; the internal counterpart to {@link #get}. */
    Rule fetch(@NonNull Long id);

    /** Whether the filter has any enabled rule — a cheap guard before fetching full issues. */
    boolean hasEnabledRules(@NonNull Long filterId);

    /** Filter's rules for a given event (e.g. all {@code ISSUE_CREATED} rules). */
    List<Rule> findRulesByEvent(@NonNull Long filterId, boolean enabled, @NonNull Event event);

    /** Filter's rules watching a specific field for {@code ISSUE_UPDATED} matching. */
    List<Rule> findRulesByField(@NonNull Long filterId, @NonNull String field, boolean enabled,
                                @NonNull Event event);
}
