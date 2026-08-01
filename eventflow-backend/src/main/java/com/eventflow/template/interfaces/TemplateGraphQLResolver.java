package com.eventflow.template.interfaces;

import com.eventflow.common.domain.Auditable;
import com.eventflow.common.domain.Channel;
import com.eventflow.template.application.TemplateRepository;
import com.eventflow.template.domain.Template;
import com.eventflow.template.domain.TemplateVersion;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GraphQL resolver for template management operations.
 * Provides CRUD operations for templates and template versions.
 */
@Controller
public class TemplateGraphQLResolver {

    private static final Logger log = LoggerFactory.getLogger(TemplateGraphQLResolver.class);

    private final TemplateRepository templateRepository;

    public TemplateGraphQLResolver(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @QueryMapping
    public TemplatePayload template(@Argument String slug) {
        log.info("Querying template: slug={}", slug);
        Optional<Template> templateOpt = templateRepository.findBySlug(slug);
        return templateOpt.map(this::toTemplatePayload).orElse(null);
    }

    @QueryMapping
    public List<TemplatePayload> templates() {
        // In production, this would use a findAll method on the repository
        // For now, we return an empty list as a placeholder
        log.info("Querying all templates");
        return List.of();
    }

    @MutationMapping
    public TemplatePayload upsertTemplate(@Argument UpsertTemplateInput input) {
        log.info("Upserting template: slug={}, channel={}", input.slug(), input.channel());

        Channel channel = Channel.fromString(input.channel());

        Optional<Template> existing = templateRepository.findBySlug(input.slug());

        if (existing.isPresent()) {
            Template template = existing.get();
            // Template already exists, return it (versioning is handled by publishTemplateVersion)
            return toTemplatePayload(template);
        }

        Template template = new Template(input.slug(), channel, input.description());
        Template saved = templateRepository.save(template);

        return toTemplatePayload(saved);
    }

    @MutationMapping
    public TemplateVersionPayload publishTemplateVersion(@Argument @NotBlank String slug,
                                                          @Argument @NotBlank String body,
                                                          @Argument String subject) {
        log.info("Publishing template version: slug={}", slug);

        Optional<Template> templateOpt = templateRepository.findBySlug(slug);
        if (templateOpt.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + slug);
        }

        Template template = templateOpt.get();
        UUID userId = UUID.randomUUID(); // In production, extracted from security context

        TemplateVersion version = template.createNewVersion(subject, body, userId);
        templateRepository.save(template);

        return new TemplateVersionPayload(
            version.getId().toString(),
            version.getTemplateSlug(),
            version.getTemplateVersionNumber(),
            version.getSubjectTemplate(),
            version.getBodyTemplate(),
            version.isActive(),
            version.getCreatedBy().toString(),
            version.getCreatedAt().toString()
        );
    }

    private TemplatePayload toTemplatePayload(Template template) {
        return new TemplatePayload(
            template.getId().toString(),
            template.getSlug(),
            template.getChannel().name(),
            template.getDescription(),
            template.getVersions().stream()
                .<TemplateVersionPayload>map(v -> new TemplateVersionPayload(
                    v.getId().toString(),
                    v.getTemplateSlug(),
                    v.getTemplateVersionNumber(),
                    v.getSubjectTemplate(),
                    v.getBodyTemplate(),
                    v.isActive(),
                    v.getCreatedBy().toString(),
                    v.getCreatedAt().toString()
                ))
                .toList(),
            template.getCreatedAt().toString(),
            template.getUpdatedAt().toString()
        );
    }

    // GraphQL DTOs

    public record TemplatePayload(
        String id,
        String slug,
        String channel,
        String description,
        List<TemplateVersionPayload> versions,
        String createdAt,
        String updatedAt
    ) {}

    public record TemplateVersionPayload(
        String id,
        String templateSlug,
        int version,
        String subjectTemplate,
        String bodyTemplate,
        boolean isActive,
        String createdBy,
        String createdAt
    ) {}

    public record UpsertTemplateInput(
        String slug,
        String channel,
        String description
    ) {}
}