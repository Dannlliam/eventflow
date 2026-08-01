package com.eventflow.template.domain;

import com.eventflow.common.domain.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

/**
 * Unit tests for the TemplateVersion entity.
 */
@DisplayName("TemplateVersion entity")
class TemplateVersionTest {

    private final UUID templateId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("should create new template version with active flag")
    void constructor_NewVersion_SetsActiveTrue() {
        TemplateVersion version = new TemplateVersion(
            templateId, "welcome-email", 1, Channel.EMAIL,
            "Subject", "Body", true, userId);

        assertNotNull(version.getId());
        assertEquals(templateId, version.getTemplateId());
        assertEquals("welcome-email", version.getTemplateSlug());
        assertEquals(1, version.getTemplateVersionNumber());
        assertEquals(Channel.EMAIL, version.getChannel());
        assertEquals("Subject", version.getSubjectTemplate());
        assertEquals("Body", version.getBodyTemplate());
        assertTrue(version.isActive());
        assertEquals(userId, version.getCreatedBy());
    }

    @Test
    @DisplayName("should deactivate version")
    void deactivate_SetsActiveToFalse() {
        TemplateVersion version = new TemplateVersion(
            templateId, "test", 1, Channel.SMS,
            null, "Body", true, userId);

        assertTrue(version.isActive());
        version.deactivate();
        assertFalse(version.isActive());
    }

    @Test
    @DisplayName("should activate version")
    void activate_SetsActiveToTrue() {
        TemplateVersion version = new TemplateVersion(
            templateId, "test", 1, Channel.EMAIL,
            "Subject", "Body", false, userId);

        assertFalse(version.isActive());
        version.activate();
        assertTrue(version.isActive());
    }

    @Test
    @DisplayName("should allow null subject template")
    void constructor_NullSubject_CreatesInstance() {
        TemplateVersion version = new TemplateVersion(
            templateId, "test", 1, Channel.EMAIL,
            null, "Body", true, userId);

        assertNull(version.getSubjectTemplate());
    }

    @Test
    @DisplayName("should throw exception for null templateId")
    void constructor_NullTemplateId_ThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new TemplateVersion(null, "test", 1, Channel.EMAIL, "Sub", "Body", true, userId));
    }

    @Test
    @DisplayName("should throw exception for null body template")
    void constructor_NullBodyTemplate_ThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new TemplateVersion(templateId, "test", 1, Channel.EMAIL, "Sub", null, true, userId));
    }

    @Test
    @DisplayName("should throw exception for null slug")
    void constructor_NullSlug_ThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new TemplateVersion(templateId, null, 1, Channel.EMAIL, "Sub", "Body", true, userId));
    }
}