package com.eventflow.provider.domain;

import com.eventflow.common.domain.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for the Provider aggregate root.
 */
@DisplayName("Provider aggregate root")
class ProviderTest {

    private final UUID workspaceId = UUID.randomUUID();
    private final Map<String, String> credentials = Map.of("apiKey", "sk-test123");
    private final Map<String, String> settings = Map.of("fromEmail", "noreply@example.com");

    @Test
    @DisplayName("should create provider with all fields")
    void constructor_ValidInputs_CreatesInstance() {
        Provider provider = new Provider(
            workspaceId, "SendGrid", ProviderType.SENDGRID, Channel.EMAIL,
            true, 100, 60, credentials, settings);

        assertNotNull(provider.getId());
        assertEquals(workspaceId, provider.getWorkspaceId());
        assertEquals("SendGrid", provider.getName());
        assertEquals(ProviderType.SENDGRID, provider.getProviderType());
        assertEquals(Channel.EMAIL, provider.getChannel());
        assertTrue(provider.isPrimary());
        assertTrue(provider.isEnabled());
        assertEquals(100, provider.getRateLimit());
        assertEquals(60, provider.getRateLimitDurationSeconds());
        assertEquals(credentials, provider.getCredentials());
        assertEquals(settings, provider.getSettings());
    }

    @Test
    @DisplayName("should enable provider")
    void enable_SetsEnabledToTrue() {
        Provider provider = createTestProvider();
        provider.disable();
        assertFalse(provider.isEnabled());

        provider.enable();
        assertTrue(provider.isEnabled());
    }

    @Test
    @DisplayName("should disable provider")
    void disable_SetsEnabledToFalse() {
        Provider provider = createTestProvider();
        assertTrue(provider.isEnabled());

        provider.disable();
        assertFalse(provider.isEnabled());
    }

    @Test
    @DisplayName("should update rate limit")
    void updateRateLimit_UpdatesValues() {
        Provider provider = createTestProvider();
        provider.updateRateLimit(200, 30);
        assertEquals(200, provider.getRateLimit());
        assertEquals(30, provider.getRateLimitDurationSeconds());
    }

    @Test
    @DisplayName("should throw exception for null workspaceId")
    void constructor_NullWorkspaceId_ThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new Provider(null, "Test", ProviderType.SENDGRID, Channel.EMAIL,
                true, 100, 60, credentials, settings));
    }

    @Test
    @DisplayName("should throw exception for null name")
    void constructor_NullName_ThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new Provider(workspaceId, null, ProviderType.SENDGRID, Channel.EMAIL,
                true, 100, 60, credentials, settings));
    }

    @Test
    @DisplayName("should handle null credentials")
    void constructor_NullCredentials_DefaultsToEmpty() {
        Provider provider = new Provider(workspaceId, "Test", ProviderType.SENDGRID, Channel.EMAIL,
            false, 50, 30, null, null);
        assertTrue(provider.getCredentials().isEmpty());
        assertTrue(provider.getSettings().isEmpty());
    }

    private Provider createTestProvider() {
        return new Provider(workspaceId, "SendGrid", ProviderType.SENDGRID, Channel.EMAIL,
            true, 100, 60, credentials, settings);
    }
}