package com.eventflow.template.domain;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

/**
 * Unit tests for the Template aggregate root.
 */
@DisplayName("Template aggregate root")
class TemplateTest {

    @Test
    @DisplayName("should create template with slug, channel, and description")
    void constructor_ValidInputs_CreatesInstance() {
        Template template = new Template("welcome-email", Channel.EMAIL, "Welcome email template");

        assertNotNull(template.getId());
        assertEquals("welcome-email", template.getSlug());
        assertEquals(Channel.EMAIL, template.getChannel());
        assertEquals("Welcome email template", template.getDescription());
        assertTrue(template.getVersions().isEmpty());
        assertTrue(template.getActiveVersion().isEmpty());
    }

    @Test
    @DisplayName("should create template without description")
    void constructor_NullDescription_CreatesInstance() {
        Template template = new Template("test-slug", Channel.SMS, null);
        assertNull(template.getDescription());
    }

    @Test
    @DisplayName("should create new version and set it as active")
    void createNewVersion_SetsActiveAndReturnsVersion() {
        Template template = new Template("welcome-email", Channel.EMAIL, "Welcome template");
        UUID userId = UUID.randomUUID();

        TemplateVersion version = template.createNewVersion("Welcome {{name}}", "<h1>Welcome {{name}}</h1>", userId);

        assertNotNull(version.getId());
        assertEquals(1, version.getTemplateVersionNumber());
        assertEquals(template.getId(), version.getTemplateId());
        assertTrue(version.isActive());
        assertTrue(template.getActiveVersion().isPresent());
        assertEquals(version.getId(), template.getActiveVersion().get().getId());
        assertEquals(1, template.getVersions().size());
    }

    @Test
    @DisplayName("should create subsequent versions with incremented version number")
    void createNewVersion_MultipleVersions_IncrementsVersion() {
        Template template = new Template("welcome-email", Channel.EMAIL, "Welcome");
        UUID userId = UUID.randomUUID();

        TemplateVersion v1 = template.createNewVersion("Subject 1", "Body 1", userId);
        TemplateVersion v2 = template.createNewVersion("Subject 2", "Body 2", userId);

        assertEquals(1, v1.getTemplateVersionNumber());
        assertEquals(2, v2.getTemplateVersionNumber());
        assertEquals(2, template.getVersions().size());
        // V2 should be active, V1 should be deactivated
        assertTrue(v2.isActive());
        assertFalse(v1.isActive());
    }

    @Test
    @DisplayName("should activate a specific version by version number")
    void activateVersion_SpecificVersion_ActivatesIt() {
        Template template = new Template("welcome-email", Channel.EMAIL, "Welcome");
        UUID userId = UUID.randomUUID();

        TemplateVersion v1 = template.createNewVersion("Subject 1", "Body 1", userId);
        TemplateVersion v2 = template.createNewVersion("Subject 2", "Body 2", userId);

        // V2 is active, reactivate V1
        template.activateVersion(1);

        assertTrue(v1.isActive());
        assertFalse(v2.isActive());
        assertEquals(v1, template.getActiveVersion().orElse(null));
    }

    @Test
    @DisplayName("should throw exception when activating non-existent version")
    void activateVersion_NonExistentVersion_ThrowsException() {
        Template template = new Template("test", Channel.EMAIL, "Test");

        assertThrows(DomainValidationException.class, () -> template.activateVersion(99));
    }

    @Test
    @DisplayName("should throw exception for null slug")
    void constructor_NullSlug_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new Template(null, Channel.EMAIL, "desc"));
    }

    @Test
    @DisplayName("should throw exception for null channel")
    void constructor_NullChannel_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new Template("test", null, "desc"));
    }
}