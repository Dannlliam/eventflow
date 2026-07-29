package com.eventflow.template.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the templates table.
 */
@Repository
public interface SpringDataTemplateRepository extends JpaRepository<TemplateJpaEntity, UUID> {
    Optional<TemplateJpaEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
}