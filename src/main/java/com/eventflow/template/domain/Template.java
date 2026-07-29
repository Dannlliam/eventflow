package com.eventflow.template.domain;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.common.domain.BaseEntity;
import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root for a Template entity.
 * A template is identified by a unique slug and can have multiple versions.
 */
public class Template extends BaseEntity {

    private final String slug;
    private final Channel channel;
    private final String description;
    private final List<TemplateVersion> versions;
    private TemplateVersion activeVersion;

    public Template(String slug, Channel channel, String description) {
        super();
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.description = description;
        this.versions = new ArrayList<>();
    }

    public Template(UUID id, String slug, Channel channel, String description,
                    List<TemplateVersion> versions, TemplateVersion activeVersion,
                    Instant createdAt, Instant updatedAt, long version) {
        super(id, createdAt, updatedAt, version);
        this.slug = slug;
        this.channel = channel;
        this.description = description;
        this.versions = new ArrayList<>(versions);
        this.activeVersion = activeVersion;
    }

    public TemplateVersion createNewVersion(String subjectTemplate, String bodyTemplate, UUID createdBy) {
        int nextVersion = versions.size() + 1;
        TemplateVersion newVersion = new TemplateVersion(
            getId(), slug, nextVersion, channel, subjectTemplate, bodyTemplate, true, createdBy
        );

        // Deactivate current active version
        if (activeVersion != null) {
            activeVersion.deactivate();
        }

        versions.add(newVersion);
        this.activeVersion = newVersion;
        markUpdated();
        return newVersion;
    }

    public void activateVersion(int versionNumber) {
        TemplateVersion target = versions.stream()
            .filter(v -> v.getTemplateVersionNumber() == versionNumber)
            .findFirst()
            .orElseThrow(() -> new DomainValidationException(
                "VERSION_NOT_FOUND",
                "Template version " + versionNumber + " not found for slug: " + slug
            ));

        // Deactivate current active version
        if (activeVersion != null) {
            activeVersion.deactivate();
        }

        // Activate the target version
        target.activate();
        this.activeVersion = target;
        markUpdated();
    }

    public String getSlug() { return slug; }
    public Channel getChannel() { return channel; }
    public String getDescription() { return description; }
    public List<TemplateVersion> getVersions() { return Collections.unmodifiableList(versions); }
    public Optional<TemplateVersion> getActiveVersion() { return Optional.ofNullable(activeVersion); }
}