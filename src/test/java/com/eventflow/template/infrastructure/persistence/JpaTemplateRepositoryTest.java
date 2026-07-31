package com.eventflow.template.infrastructure.persistence;

import com.eventflow.template.domain.model.Template;
import com.eventflow.template.domain.model.TemplateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JpaTemplateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaTemplateRepositoryAdapter jpaTemplateRepository;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
    }

    @Test
    void save_shouldPersistTemplate_successfully() {
        // Arrange
        Template template = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Welcome Email")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.DRAFT)
                .createdAt(Instant.now())
                .build();

        // Act
        Template savedTemplate = jpaTemplateRepository.save(template);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(savedTemplate).isNotNull();
        assertThat(savedTemplate.getId()).isEqualTo(template.getId());
        
        Template foundTemplate = jpaTemplateRepository.findById(savedTemplate.getId()).orElse(null);
        assertThat(foundTemplate).isNotNull();
        assertThat(foundTemplate.getName()).isEqualTo("Welcome Email");
        assertThat(foundTemplate.getChannel()).isEqualTo(Template.Channel.EMAIL);
    }

    @Test
    void findById_shouldReturnTemplate_whenExists() {
        // Arrange
        Template template = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Test Template")
                .channel(Template.Channel.SMS)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        jpaTemplateRepository.save(template);
        entityManager.flush();

        // Act
        Optional<Template> result = jpaTemplateRepository.findById(template.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(template.getId());
        assertThat(result.get().getName()).isEqualTo("Test Template");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotExists() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<Template> result = jpaTemplateRepository.findById(nonExistentId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findByWorkspaceId_shouldReturnAllTemplates() {
        // Arrange
        Template template1 = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Template 1")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        
        Template template2 = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Template 2")
                .channel(Template.Channel.SMS)
                .status(Template.Status.DRAFT)
                .createdAt(Instant.now())
                .build();

        jpaTemplateRepository.save(template1);
        jpaTemplateRepository.save(template2);
        entityManager.flush();

        // Act
        List<Template> results = jpaTemplateRepository.findByWorkspaceId(workspaceId);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Template::getName)
                .containsExactlyInAnyOrder("Template 1", "Template 2");
    }

    @Test
    void findByWorkspaceIdAndChannel_shouldFilterByChannel() {
        // Arrange
        Template emailTemplate = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Email Template")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        
        Template smsTemplate = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SMS Template")
                .channel(Template.Channel.SMS)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();

        jpaTemplateRepository.save(emailTemplate);
        jpaTemplateRepository.save(smsTemplate);
        entityManager.flush();

        // Act
        List<Template> emailResults = jpaTemplateRepository.findByWorkspaceIdAndChannel(
                workspaceId, Template.Channel.EMAIL);

        // Assert
        assertThat(emailResults).hasSize(1);
        assertThat(emailResults.get(0).getName()).isEqualTo("Email Template");
        assertThat(emailResults.get(0).getChannel()).isEqualTo(Template.Channel.EMAIL);
    }

    @Test
    void delete_shouldRemoveTemplate() {
        // Arrange
        Template template = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("To Delete")
                .channel(Template.Channel.PUSH)
                .status(Template.Status.DRAFT)
                .createdAt(Instant.now())
                .build();
        jpaTemplateRepository.save(template);
        entityManager.flush();

        // Act
        jpaTemplateRepository.delete(template.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Template> result = jpaTemplateRepository.findById(template.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByWorkspaceIdAndStatus_shouldFilterByStatus() {
        // Arrange
        Template publishedTemplate = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Published")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        
        Template draftTemplate = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Draft")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.DRAFT)
                .createdAt(Instant.now())
                .build();

        jpaTemplateRepository.save(publishedTemplate);
        jpaTemplateRepository.save(draftTemplate);
        entityManager.flush();

        // Act
        List<Template> publishedResults = jpaTemplateRepository.findByWorkspaceIdAndStatus(
                workspaceId, Template.Status.PUBLISHED);

        // Assert
        assertThat(publishedResults).hasSize(1);
        assertThat(publishedResults.get(0).getName()).isEqualTo("Published");
    }

    @Test
    void update_shouldModifyExistingTemplate() {
        // Arrange
        Template template = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Original Name")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.DRAFT)
                .createdAt(Instant.now())
                .build();
        Template savedTemplate = jpaTemplateRepository.save(template);
        entityManager.flush();
        entityManager.clear();

        // Act - Update
        Template updatedTemplate = Template.builder()
                .id(savedTemplate.getId())
                .workspaceId(workspaceId)
                .name("Updated Name")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.PUBLISHED)
                .createdAt(savedTemplate.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        jpaTemplateRepository.save(updatedTemplate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Template result = jpaTemplateRepository.findById(savedTemplate.getId()).orElse(null);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getStatus()).isEqualTo(Template.Status.PUBLISHED);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_shouldHandleMultipleChannels() {
        // Arrange & Act
        Template email = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Email")
                .channel(Template.Channel.EMAIL)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        
        Template sms = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("SMS")
                .channel(Template.Channel.SMS)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        
        Template push = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Push")
                .channel(Template.Channel.PUSH)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();
        
        Template webhook = Template.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Webhook")
                .channel(Template.Channel.WEBHOOK)
                .status(Template.Status.PUBLISHED)
                .createdAt(Instant.now())
                .build();

        jpaTemplateRepository.save(email);
        jpaTemplateRepository.save(sms);
        jpaTemplateRepository.save(push);
        jpaTemplateRepository.save(webhook);
        entityManager.flush();

        // Assert
        List<Template> all = jpaTemplateRepository.findByWorkspaceId(workspaceId);
        assertThat(all).hasSize(4);
        assertThat(all).extracting(Template::getChannel)
                .containsExactlyInAnyOrder(
                        Template.Channel.EMAIL,
                        Template.Channel.SMS,
                        Template.Channel.PUSH,
                        Template.Channel.WEBHOOK
                );
    }
}
