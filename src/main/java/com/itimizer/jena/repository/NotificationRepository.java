package com.itimizer.jena.repository;

import com.itimizer.jena.entity.Notification;
import com.itimizer.jena.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for {@link Notification}. Its finder selects retry-eligible rows: failed, under
 * the attempt limit, and with a changelog (which excludes audit-only snapshot notifications).
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatusAndFailureCountLessThanEqualAndChangelogIsNotNull(
            Status status, int failureCount);
}
