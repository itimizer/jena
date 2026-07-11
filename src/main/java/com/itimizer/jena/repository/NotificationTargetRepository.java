package com.itimizer.jena.repository;

import com.itimizer.jena.entity.NotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link NotificationTarget}.
 */
@Repository
public interface NotificationTargetRepository extends JpaRepository<NotificationTarget, Long> {
}