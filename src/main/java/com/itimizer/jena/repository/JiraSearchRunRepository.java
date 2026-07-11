package com.itimizer.jena.repository;

import com.itimizer.jena.entity.JiraSearchRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link JiraSearchRun}, the per-filter polling cursor (one row per filter).
 */
@Repository
public interface JiraSearchRunRepository extends JpaRepository<JiraSearchRun, Long> {

    JiraSearchRun findByFilterId(Long filterId);
}
