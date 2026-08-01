package com.eventflow.template.application;

import com.eventflow.common.domain.Channel;
import com.eventflow.template.domain.Template;
import com.eventflow.template.domain.TemplateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for Template Publishing Use Case
 * Verifies template creation and version management
 */
@ExtendWith(MockitoExtension.class)
class TemplatePublishUseCaseTest {

    @Mock
    private TemplateRepository templateRepository;

    private UUID userId;
    private String templateSlug;
    private String subjectTemplate;
    private String bodyTemplate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        templateSlug = "welcome-email";
        subjectTemplate = "Welcome {{name}}!";
        bodyTemplate = "<h1>Welcome {{name}}</h1><p>Thanks for joining us!</p>";
    }

    @Test
    void shouldCreateNewTemplateVersion() {
        // Given
        Template template = new Template(templateSlug, Channel.EMAIL, "Welcome email template");
        when(templateRepository.findBySlug(templateSlug)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(Template.class))).thenAnswer(i -> i.getArgument(0));

        // When
        TemplateVersion version = template.createNewVersion(subjectTemplate, bodyTemplate, userId);

        // Then
        assertThat(version).isNotNull();
        assertThat(version.getSubjectTemplate()).isEqualTo(subjectTemplate);
        assertThat(version.getBodyTemplate()).isEqualTo(bodyTemplate);
        assertThat(version.isActive()).isTrue();
        assertThat(version.getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void shouldIncrementVersionNumber() {
        // Given
        Template template = new Template(templateSlug, Channel.EMAIL, "Welcome email template");
        template.createNewVersion("Subject v1", "Body v1", userId);
        when(templateRepository.findBySlug(templateSlug)).thenReturn(Optional.of(template));

        // When
        TemplateVersion version2 = template.createNewVersion(subjectTemplate, bodyTemplate, userId);

        // Then
        assertThat(version2.getTemplateVersionNumber()).isEqualTo(2);
    }

    @Test
    void shouldDeactivatePreviousVersionWhenPublishingNew() {
        // Given
        Template template = new Template(templateSlug, Channel.EMAIL, "Welcome email template");
        TemplateVersion version1 = template.createNewVersion("Subject v1", "Body v1", userId);

        // When
        TemplateVersion version2 = template.createNewVersion(subjectTemplate, bodyTemplate, userId);

        // Then
        assertThat(version1.isActive()).isFalse();
        assertThat(version2.isActive()).isTrue();
    }

    @Test
    void shouldValidateTemplateSlugFormat() {
        // Given
        String invalidSlug = "Invalid Template!";

        // When/Then
        assertThatThrownBy(() -> new Template(invalidSlug, Channel.EMAIL, "Description"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldStoreTemplateMetadata() {
        // Given
        String description = "Welcome email sent to new users";
        Template template = new Template(templateSlug, Channel.EMAIL, description);

        // When/Then
        assertThat(template.getSlug()).isEqualTo(templateSlug);
        assertThat(template.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(template.getDescription()).isEqualTo(description);
    }

    @Test
    void shouldMaintainVersionHistory() {
        // Given
        Template template = new Template(templateSlug, Channel.EMAIL, "Welcome email");
        
        // When
        template.createNewVersion("Subject v1", "Body v1", userId);
        template.createNewVersion("Subject v2", "Body v2", userId);
        template.createNewVersion("Subject v3", "Body v3", userId);

        // Then
        assertThat(template.getVersions()).hasSize(3);
        assertThat(template.getVersions().get(0).getTemplateVersionNumber()).isEqualTo(1);
        assertThat(template.getVersions().get(1).getTemplateVersionNumber()).isEqualTo(2);
        assertThat(template.getVersions().get(2).getTemplateVersionNumber()).isEqualTo(3);
    }

    @Test
    void shouldOnlyHaveOneActiveVersion() {
        // Given
        Template template = new Template(templateSlug, Channel.EMAIL, "Welcome email");
        template.createNewVersion("Subject v1", "Body v1", userId);
        template.createNewVersion("Subject v2", "Body v2", userId);
        template.createNewVersion("Subject v3", "Body v3", userId);

        // When
        long activeCount = template.getVersions().stream()
            .filter(TemplateVersion::isActive)
            .count();

        // Then
        assertThat(activeCount).isEqualTo(1);
    }
}
