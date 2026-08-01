package com.eventflow.common.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Redis-based IdempotencyService.
 * Uses Testcontainers Redis for isolated testing.
 */
@SpringBootTest
@Testcontainers
class IdempotencyServiceIntegrationTest {

    @Container
    static final org.testcontainers.containers.GenericContainer<?> redis = new org.testcontainers.containers.GenericContainer<>(
            "redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IdempotencyService idempotencyService;

    private UUID workspaceId;
    private UUID eventId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        idempotencyKey = "test-key-" + UUID.randomUUID();
        // Clear any existing keys
        redisTemplate.delete(redisTemplate.keys("idemp:*"));
    }

    @Test
    void shouldRegisterNewIdempotencyKey() {
        Optional<UUID> result = idempotencyService.tryRegisterIdempotencyKey(
            workspaceId, idempotencyKey, eventId);

        assertTrue(result.isEmpty(), "New key should return empty");
    }

    @Test
    void shouldDetectDuplicateIdempotencyKey() {
        // First registration
        idempotencyService.tryRegisterIdempotencyKey(workspaceId, idempotencyKey, eventId);

        // Duplicate attempt
        UUID secondEventId = UUID.randomUUID();
        Optional<UUID> result = idempotencyService.tryRegisterIdempotencyKey(
            workspaceId, idempotencyKey, secondEventId);

        assertTrue(result.isPresent(), "Duplicate key should return original eventId");
        assertEquals(eventId, result.get(), "Should return original eventId");
    }

    @Test
    void shouldCheckExistingKey() {
        idempotencyService.tryRegisterIdempotencyKey(workspaceId, idempotencyKey, eventId);

        Optional<UUID> result = idempotencyService.checkIdempotencyKey(workspaceId, idempotencyKey);

        assertTrue(result.isPresent(), "Existing key should be found");
        assertEquals(eventId, result.get());
    }

    @Test
    void shouldReturnEmptyForNonExistentKey() {
        Optional<UUID> result = idempotencyService.checkIdempotencyKey(
            workspaceId, "nonexistent-key");

        assertTrue(result.isEmpty(), "Non-existent key should return empty");
    }

    @Test
    void shouldRemoveIdempotencyKey() {
        idempotencyService.tryRegisterIdempotencyKey(workspaceId, idempotencyKey, eventId);
        idempotencyService.removeIdempotencyKey(workspaceId, idempotencyKey);

        Optional<UUID> result = idempotencyService.checkIdempotencyKey(workspaceId, idempotencyKey);
        assertTrue(result.isEmpty(), "Key should be removed");
    }

    @Test
    void shouldHandleConcurrentRegistrations() throws InterruptedException {
        // Simulate concurrent registration attempts
        Thread thread1 = new Thread(() ->
            idempotencyService.tryRegisterIdempotencyKey(workspaceId, idempotencyKey, eventId));
        Thread thread2 = new Thread(() ->
            idempotencyService.tryRegisterIdempotencyKey(workspaceId, idempotencyKey, UUID.randomUUID()));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Only one should succeed; the other should get the duplicate
        Optional<UUID> check = idempotencyService.checkIdempotencyKey(workspaceId, idempotencyKey);
        assertTrue(check.isPresent(), "Key should exist after concurrent attempts");
        assertEquals(eventId, check.get(), "Should return the first eventId");
    }

    @Test
    void shouldUseDifferentKeysForDifferentWorkspaces() {
        UUID workspaceA = UUID.randomUUID();
        UUID workspaceB = UUID.randomUUID();
        String sameKey = "shared-key";

        idempotencyService.tryRegisterIdempotencyKey(workspaceA, sameKey, eventId);

        // Same key, different workspace should not be a duplicate
        Optional<UUID> result = idempotencyService.tryRegisterIdempotencyKey(
            workspaceB, sameKey, UUID.randomUUID());
        assertTrue(result.isEmpty(), "Same key in different workspace should not be duplicate");
    }
}