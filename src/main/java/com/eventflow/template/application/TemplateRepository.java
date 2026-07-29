package com.eventflow.template.application;

import com.eventflow.template.domain.Template;
import com.eventflow.template.domain.TemplateVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for Template persistence operations.
 */
public interface TemplateRepository {
    Template save(Template template);
    Optional<Template> findById(UUID id);
    Optional<Template> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Template> findAll(int limit, int offset);
}