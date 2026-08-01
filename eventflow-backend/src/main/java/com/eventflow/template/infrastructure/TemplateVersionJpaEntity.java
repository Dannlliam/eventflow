package com.eventflow.template.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the template_versions table.
 * Maps the domain TemplateVersion aggregate to PostgreSQL.
 */
@Entity
@Table(name = "template_versions", schema = "eventflow")
public class TemplateVersionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_slug", nullable = false, length = 100)
    private String templateSlug;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "subject_template", columnDefinition = "text")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version_entity", nullable = false)
    private long versionEntity;

    public TemplateVersionJpaEntity() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public String getTemplateSlug() { return templateSlug; }
    public void setTemplateSlug(String templateSlug) { this.templateSlug = templateSlug; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersionEntity() { return versionEntity; }
    public void setVersionEntity(long versionEntity) { this.versionEntity = versionEntity; }
}