package com.eventflow.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for the Recipient value object.
 */
@DisplayName("Recipient value object")
class RecipientTest {

    @Test
    @DisplayName("should create recipient with email")
    void constructor_ValidEmail_CreatesInstance() {
        Recipient recipient = new Recipient("user@example.com", null, null, null);
        assertEquals("user@example.com", recipient.email());
        assertNull(recipient.phone());
        assertNull(recipient.deviceToken());
        assertNull(recipient.webhookUrl());
    }

    @Test
    @DisplayName("should create recipient with phone")
    void constructor_ValidPhone_CreatesInstance() {
        Recipient recipient = new Recipient(null, "+1234567890", null, null);
        assertEquals("+1234567890", recipient.phone());
        assertNull(recipient.email());
    }

    @Test
    @DisplayName("should create recipient with device token")
    void constructor_ValidDeviceToken_CreatesInstance() {
        Recipient recipient = new Recipient(null, null, "device-token-123", null);
        assertEquals("device-token-123", recipient.deviceToken());
    }

    @Test
    @DisplayName("should create recipient with webhook URL")
    void constructor_ValidWebhookUrl_CreatesInstance() {
        Recipient recipient = new Recipient(null, null, null, "https://hooks.example.com/notify");
        assertEquals("https://hooks.example.com/notify", recipient.webhookUrl());
    }

    @Test
    @DisplayName("should convert to map")
    void toMap_ReturnsCorrectMap() {
        Recipient recipient = new Recipient("user@example.com", "+1234567890", "token", "https://hook.com");
        Map<String, Object> map = recipient.toMap();

        assertEquals("user@example.com", map.get("email"));
        assertEquals("+1234567890", map.get("phone"));
        assertEquals("token", map.get("deviceToken"));
        assertEquals("https://hook.com", map.get("webhookUrl"));
    }

    @Test
    @DisplayName("should have equals based on all fields")
    void equals_SameFields_ReturnsTrue() {
        Recipient r1 = new Recipient("user@example.com", "+123", null, null);
        Recipient r2 = new Recipient("user@example.com", "+123", null, null);
        assertEquals(r1, r2);
    }

    @Test
    @DisplayName("should have different hashcodes for different recipients")
    void equals_DifferentFields_ReturnsFalse() {
        Recipient r1 = new Recipient("user@example.com", null, null, null);
        Recipient r2 = new Recipient("other@example.com", null, null, null);
        assertNotEquals(r1, r2);
    }
}