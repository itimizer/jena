package com.itimizer.jena.repository;

import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Changelog}. Provides the dispatch queue ({@code status is null}) and
 * the idempotency lookup over the unique tuple used when a duplicate insert is rejected.
 */
@Repository
public interface ChangelogRepository extends JpaRepository<Changelog, Long> {

    List<Changelog> findByStatusIsNull();

    Optional<Changelog> findByEventAndItemAndItemIndexAndIssueKeyAndRuleIdAndFilterId(
            Event event, long item, int itemIndex, String issueKey, Long ruleId, Long filterId);
}
