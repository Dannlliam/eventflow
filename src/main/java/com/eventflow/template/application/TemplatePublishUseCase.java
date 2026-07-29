package com.eventflow.template.application;

import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.template.domain.Template;
import com.eventflow.template.domain.TemplateVersion;
import com.eventflow.common.domain.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for publishing new template versions.
 * Handles the complete template version lifecycle:
 * - Creating new versions of existing templates
 * - Activating a specific version (deactivates all others)
 * - Creating brand new templates with their first version
 *
 * As specified in the PRD Section 49 - Template Engine / Versioning.
 */
public class TemplatePublishUseCase {

    private static final Logger log = LoggerFactory.getLogger(TemplatePublishUseCase.class);

    private final TemplateRepository templateRepository;

    public TemplatePublishUseCase(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Publishes a new version of an existing template.
     * If the template does not exist, creates it with the first version.
     *
     * @param command the publish command with template details
     * @return the published template version
     */
    public PublishResult publishNewVersion(PublishCommand command) {
        log.info("Publishing new template version: slug={}, channel={}", command.slug(), command.channel());

        Optional<Template> existingTemplate = templateRepository.findBySlug(command.slug());

        Template template;
        if (existingTemplate.isPresent()) {
            template = existingTemplate.get();

            // Validate channel matches existing template
            if (!template.getChannel().name().equals(command.channel())) {
                throw new DomainValidationException(
                    "TEMPLATE_CHANNEL_MISMATCH",
                    "Template '" + command.slug() + "' is already defined for channel "
                        + template.getChannel() + ", not " + command.channel()
                );
            }

            // Add a new version
            TemplateVersion newVersion = template.createNewVersion(
                command.subjectTemplate(),
                command.bodyTemplate(),
                command.createdBy()
            );
            templateRepository.save(template);

            log.info("Published new version {} for template '{}': versionId={}",
                newVersion.getTemplateVersionNumber(), command.slug(), newVersion.getId());

            return new PublishResult(template.getId(), newVersion.getId(), newVersion.getTemplateVersionNumber(), true);
        } else {
            // Create a new template with its first version
            template = new Template(
                command.slug(),
                Channel.fromString(command.channel()),
                command.description()
            );
            TemplateVersion firstVersion = template.createNewVersion(
                command.subjectTemplate(),
                command.bodyTemplate(),
                command.createdBy()
            );
            templateRepository.save(template);

            log.info("Created new template '{}' with version 1: templateId={}, versionId={}",
                command.slug(), template.getId(), firstVersion.getId());

            return new PublishResult(template.getId(), firstVersion.getId(), firstVersion.getTemplateVersionNumber(), true);
        }
    }

    /**
     * Activates a specific version of a template.
     * This deactivates all other versions of the same template.
     *
     * @param templateSlug the slug identifying the template
     * @param versionNumber the version number to activate
     * @return the activated template version
     */
    public TemplateVersion activateVersion(String templateSlug, int versionNumber) {
        log.info("Activating template version: slug={}, version={}", templateSlug, versionNumber);

        Template template = templateRepository.findBySlug(templateSlug)
            .orElseThrow(() -> new DomainValidationException(
                "TEMPLATE_NOT_FOUND",
                "Template not found: " + templateSlug
            ));

        template.activateVersion(versionNumber);
        templateRepository.save(template);

        TemplateVersion activeVersion = template.getActiveVersion()
            .orElseThrow(() -> new IllegalStateException(
                "Failed to activate version " + versionNumber + " for template '" + templateSlug + "'"
            ));

        log.info("Activated version {} for template '{}'", versionNumber, templateSlug);

        return activeVersion;
    }

    /**
     * Command for publishing a new template version.
     */
    public record PublishCommand(
        String slug,
        String channel,
        String description,
        String bodyTemplate,
        String subjectTemplate,
        UUID createdBy
    ) {}

    /**
     * Result of a publish operation.
     */
    public record PublishResult(
        UUID templateId,
        UUID versionId,
        int versionNumber,
        boolean success
    ) {}
}