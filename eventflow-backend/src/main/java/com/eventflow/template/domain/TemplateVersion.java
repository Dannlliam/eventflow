package com.eventflow.template.domain;

import com.eventflow.common.domain.BaseEntity;
import com.eventflow.common.domain.Channel;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a specific version of a template.
 * Templates are versioned for auditability; historical notifications reference
 * the exact template version used at the time of sending.
 */
public class TemplateVersion extends BaseEntity {

    private final UUID templateId;
    private final String templateSlug;
    private final int version;
    private final Channel channel;
    private final String subjectTemplate;
    private final String bodyTemplate;
    private boolean isActive;
    private final UUID createdBy;

    public TemplateVersion(UUID templateId, String templateSlug, int version, Channel channel,
                           String subjectTemplate, String bodyTemplate, boolean isActive, UUID createdBy) {
        super();
        this.templateId = Objects.requireNonNull(templateId, "templateId must not be null");
        this.templateSlug = Objects.requireNonNull(templateSlug, "templateSlug must not be null");
        this.version = version;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = Objects.requireNonNull(bodyTemplate, "bodyTemplate must not be null");
        this.isActive = isActive;
        this.createdBy = createdBy;
    }

    public TemplateVersion(UUID id, UUID templateId, String templateSlug, int version,
                           Channel channel, String subjectTemplate, String bodyTemplate,
                           boolean isActive, UUID createdBy,
                           Instant createdAt, Instant updatedAt, long versionEntity) {
        super(id, createdAt, updatedAt, versionEntity);
        this.templateId = templateId;
        this.templateSlug = templateSlug;
        this.version = version;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.isActive = isActive;
        this.createdBy = createdBy;
    }

    public UUID getTemplateId() { return templateId; }
    public String getTemplateSlug() { return templateSlug; }
    public int getTemplateVersionNumber() { return version; }
    public Channel getChannel() { return channel; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public boolean isActive() { return isActive; }
    public UUID getCreatedBy() { return createdBy; }

    public void deactivate() {
        this.isActive = false;
        markUpdated();
    }

    public void activate() {
        this.isActive = true;
        markUpdated();
    }
}