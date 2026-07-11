package com.itimizer.jena.repository;

import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.NotificationTarget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link JiraFilter}. The enabled-by-mode finders (with an entity graph over
 * targets/template) feed the schedulers, and {@code findEnabledTargets} resolves a filter's enabled
 * targets for a channel at dispatch time.
 */
@Repository
public interface JiraFilterRepository extends JpaRepository<JiraFilter, Long> {

    @EntityGraph(attributePaths = {"targets", "template"})
    List<JiraFilter> findByModeAndEnabledTrue(FilterMode mode);

    @EntityGraph(attributePaths = {"targets", "template"})
    Optional<JiraFilter> findByIdAndModeAndEnabledTrue(Long id, FilterMode mode);

    @Query(
            """
            select t from JiraFilter f join f.targets t
            where f.id = :filterId and t.channel = :channel and t.enabled = true
            """
    )
    List<NotificationTarget> findEnabledTargets(@Param("filterId") Long filterId,
                                                @Param("channel") Channel channel);
}