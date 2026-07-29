package com.eventflow.template.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the template_versions table.
 */
@Repository
public interface SpringDataTemplateVersionRepository extends JpaRepository<TemplateVersionJpaEntity, UUID> {

    List<TemplateVersionJpaEntity> findByTemplateIdOrderByVersionDesc(UUID templateId);

    Optional<TemplateVersionJpaEntity> findByTemplateIdAndVersion(UUID templateId, int version);

    Optional<TemplateVersionJpaEntity> findByTemplateSlugAndIsActiveTrue(String templateSlug);
}