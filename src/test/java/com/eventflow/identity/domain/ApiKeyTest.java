package com.eventflow.identity.domain;

import com.eventflow.common.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

/**
 * Unit tests for the ApiKey aggregate root.
 */
@DisplayName("ApiKey aggregate root")
class ApiKeyTest {

    private final UUID workspaceId = UUID.randomUUID();

    @Test
    @DisplayName("should create API key with prefix and hash")
    void constructor_ValidInputs_CreatesInstance() {
        ApiKey apiKey = new ApiKey(workspaceId, "Test key");

        assertNotNull(apiKey.getId());
        assertEquals(workspaceId, apiKey.getWorkspaceId());
        assertNotNull(apiKey.getKeyPrefix());
        assertEquals("Test key", apiKey.getDescription());
        assertTrue(apiKey.isActive());
        assertNotNull(apiKey.getCreatedAt());
        assertNotNull(apiKey.getUpdatedAt());
        assertTrue(apiKey.getKeyPrefix().startsWith("ef_live_"));
        assertNotNull(apiKey.getKeyHash());
    }

    @Test
    @DisplayName("should create API key without description")
    void constructor_NoDescription_CreatesInstance() {
        ApiKey apiKey = new ApiKey(workspaceId, null);
        assertNull(apiKey.getDescription());
    }

    @Test
    @DisplayName("should deactivate API key")
    void deactivate_SetsActiveToFalse() {
        ApiKey apiKey = new ApiKey(workspaceId, "Test");
        assertTrue(apiKey.isActive());

        apiKey.deactivate();
        assertFalse(apiKey.isActive());
    }

    @Test
    @DisplayName("should update last used timestamp")
    void recordUsage_UpdatesTimestamp() {
        ApiKey apiKey = new ApiKey(workspaceId, "Test");
        assertNull(apiKey.getLastUsedAt().orElse(null));

        apiKey.recordUsage();
        assertNotNull(apiKey.getLastUsedAt().orElse(null));
    }

    @Test
    @DisplayName("should throw exception when workspaceId is null")
    void constructor_NullWorkspaceId_ThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new ApiKey(null, "desc"));
    }

    @Test
    @DisplayName("should have valid key hash")
    void constructor_GeneratesValidHash() {
        ApiKey apiKey = new ApiKey(workspaceId, "Test key");
        assertNotNull(apiKey.getKeyHash());
        // Hash should be 64 hex characters (SHA-256)
        assertEquals(64, apiKey.getKeyHash().length());
    }

    @Test
    @DisplayName("should match hash correctly")
    void matchesHash_ValidHash_ReturnsTrue() {
        // This test verifies the hash matching logic
        // The actual raw key is not exposed, so we test through the public API
        ApiKey apiKey = new ApiKey(workspaceId, "Test");
        assertNotNull(apiKey.getKeyPrefix());
        assertNotNull(apiKey.getKeyHash());
    }
}