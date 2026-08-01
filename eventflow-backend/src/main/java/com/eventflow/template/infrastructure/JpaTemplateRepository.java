package com.eventflow.template.infrastructure;

import com.eventflow.common.domain.Channel;
import com.eventflow.template.application.TemplateRepository;
import com.eventflow.template.domain.Template;
import com.eventflow.template.domain.TemplateVersion;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of the TemplateRepository port.
 * Manages both Template and TemplateVersion persistence.
 */
@Repository
@Transactional
public class JpaTemplateRepository implements TemplateRepository {

    private final SpringDataTemplateRepository springDataRepository;
    private final SpringDataTemplateVersionRepository springDataVersionRepository;

    public JpaTemplateRepository(SpringDataTemplateRepository springDataRepository,
                                  SpringDataTemplateVersionRepository springDataVersionRepository) {
        this.springDataRepository = springDataRepository;
        this.springDataVersionRepository = springDataVersionRepository;
    }

    @Override
    public Template save(Template template) {
        // Save the template entity
        TemplateJpaEntity entity = toJpaEntity(template);
        TemplateJpaEntity saved = springDataRepository.save(entity);

        // Save template versions
        for (TemplateVersion version : template.getVersions()) {
            springDataVersionRepository.save(toVersionJpaEntity(version));
        }

        return toDomainEntity(saved);
    }

    @Override
    public Optional<Template> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomainEntityWithVersions);
    }

    @Override
    public Optional<Template> findBySlug(String slug) {
        return springDataRepository.findBySlug(slug).map(this::toDomainEntityWithVersions);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return springDataRepository.existsBySlug(slug);
    }

    @Override
    public List<Template> findAll(int limit, int offset) {
        return springDataRepository.findAll(org.springframework.data.domain.PageRequest.of(offset / limit, limit))
            .stream()
            .map(this::toDomainEntityWithVersions)
            .collect(Collectors.toList());
    }

    private TemplateJpaEntity toJpaEntity(Template domain) {
        TemplateJpaEntity entity = new TemplateJpaEntity();
        entity.setId(domain.getId());
        entity.setSlug(domain.getSlug());
        entity.setChannel(domain.getChannel().name());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    private TemplateVersionJpaEntity toVersionJpaEntity(TemplateVersion domain) {
        TemplateVersionJpaEntity entity = new TemplateVersionJpaEntity();
        entity.setId(domain.getId());
        entity.setTemplateId(domain.getTemplateId());
        entity.setTemplateSlug(domain.getTemplateSlug());
        entity.setVersion(domain.getTemplateVersionNumber());
        entity.setChannel(domain.getChannel().name());
        entity.setSubjectTemplate(domain.getSubjectTemplate());
        entity.setBodyTemplate(domain.getBodyTemplate());
        entity.setActive(domain.isActive());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersionEntity(domain.getVersion());
        return entity;
    }

    private Template toDomainEntity(TemplateJpaEntity entity) {
        return new Template(
            entity.getId(),
            entity.getSlug(),
            Channel.fromString(entity.getChannel()),
            entity.getDescription(),
            new ArrayList<>(),
            null,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    private Template toDomainEntityWithVersions(TemplateJpaEntity entity) {
        // Load all versions from the database
        List<TemplateVersionJpaEntity> versionEntities =
            springDataVersionRepository.findByTemplateIdOrderByVersionDesc(entity.getId());

        List<TemplateVersion> versions = versionEntities.stream()
            .map(this::toVersionDomainEntity)
            .collect(Collectors.toList());

        // Find the active version
        TemplateVersion activeVersion = versions.stream()
            .filter(TemplateVersion::isActive)
            .findFirst()
            .orElse(null);

        return new Template(
            entity.getId(),
            entity.getSlug(),
            Channel.fromString(entity.getChannel()),
            entity.getDescription(),
            versions,
            activeVersion,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    private TemplateVersion toVersionDomainEntity(TemplateVersionJpaEntity entity) {
        return new TemplateVersion(
            entity.getId(),
            entity.getTemplateId(),
            entity.getTemplateSlug(),
            entity.getVersion(),
            Channel.fromString(entity.getChannel()),
            entity.getSubjectTemplate(),
            entity.getBodyTemplate(),
            entity.isActive(),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersionEntity()
        );
    }
}