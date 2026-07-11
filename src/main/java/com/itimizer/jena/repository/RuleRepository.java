package com.itimizer.jena.repository;

import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for {@link Rule}. Its finders scope to a filter's linked rules and drive the
 * poller's matching engine (by event, and by event + field).
 */
@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {

    boolean existsByFiltersIdAndEnabledTrue(Long filterId);

    List<Rule> findByFiltersIdAndEnabledAndEvent(Long filterId, boolean enabled, Event event);

    List<Rule> findByFiltersIdAndFieldAndEnabledAndEvent(Long filterId, String field,
                                                         boolean enabled, Event event);
}
