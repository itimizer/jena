package com.itimizer.jena.repository;

import com.itimizer.jena.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link Template}.
 */
@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
}
