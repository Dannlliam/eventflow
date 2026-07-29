package com.eventflow.common.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based idempotency service for the notification ingestion API.
 * Prevents duplicate notification processing by storing and checking idempotency keys.
 * Uses SET NX (set if not exists) with a 24-hour TTL as specified in the PRD.
 * 
 * Key pattern: idemp:{workspace_id}:{idempotency_key}
 * Value: JSON string containing the eventId and status
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private static final String IDEMPOTENCY_KEY_PREFIX = "idemp:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    private static final String KEY_SEPARATOR = ":";

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to register an idempotency key for a given workspace.
     * If the key does not exist, it is created and returns empty (no duplicate).
     * If the key already exists, returns the stored eventId (duplicate detected).
     *
     * @param workspaceId the workspace UUID
     * @param idempotencyKey the client-provided idempotency key
     * @param eventId the event ID to store if this is a new request
     * @return Optional containing the existing eventId if a duplicate was detected
     */
    public Optional<UUID> tryRegisterIdempotencyKey(UUID workspaceId, String idempotencyKey, UUID eventId) {
        String redisKey = buildKey(workspaceId, idempotencyKey);
        String value = eventId.toString();

        Boolean keySet = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, value, IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(keySet)) {
            log.debug("Registered new idempotency key: workspaceId={}, key={}, eventId={}",
                workspaceId, idempotencyKey, eventId);
            return Optional.empty();
        }

        // Key already exists - retrieve the original eventId
        String existingValue = redisTemplate.opsForValue().get(redisKey);
        if (existingValue != null) {
            log.info("Duplicate idempotency key detected: workspaceId={}, key={}, originalEventId={}",
                workspaceId, idempotencyKey, existingValue);
            return Optional.of(UUID.fromString(existingValue));
        }

        // Race condition: key expired between setIfAbsent failure and get().
        // Attempt to re-register within a retry loop.
        for (int i = 0; i < 3; i++) {
            keySet = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, value, IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
            if (Boolean.TRUE.equals(keySet)) {
                log.warn("Idempotency key re-registered after race condition: workspaceId={}, key={}",
                    workspaceId, idempotencyKey);
                return Optional.empty();
            }
            // Another thread set it - retrieve the value
            existingValue = redisTemplate.opsForValue().get(redisKey);
            if (existingValue != null) {
                return Optional.of(UUID.fromString(existingValue));
            }
        }

        // Final fallback: still ambiguous, return empty to allow database-level dedup
        log.error("Idempotency key race condition unresolved after retries: workspaceId={}, key={}",
            workspaceId, idempotencyKey);
        return Optional.empty();
    }

    /**
     * Checks if an idempotency key exists without registering a new one.
     * Used for read-only validation.
     *
     * @param workspaceId the workspace UUID
     * @param idempotencyKey the client-provided idempotency key
     * @return Optional containing the stored eventId if the key exists
     */
    public Optional<UUID> checkIdempotencyKey(UUID workspaceId, String idempotencyKey) {
        String redisKey = buildKey(workspaceId, idempotencyKey);
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value != null) {
            return Optional.of(UUID.fromString(value));
        }
        return Optional.empty();
    }

    /**
     * Removes an idempotency key from Redis. Used for testing and manual overrides.
     *
     * @param workspaceId the workspace UUID
     * @param idempotencyKey the idempotency key to remove
     */
    public void removeIdempotencyKey(UUID workspaceId, String idempotencyKey) {
        String redisKey = buildKey(workspaceId, idempotencyKey);
        redisTemplate.delete(redisKey);
        log.debug("Removed idempotency key: workspaceId={}, key={}", workspaceId, idempotencyKey);
    }

    private String buildKey(UUID workspaceId, String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + workspaceId.toString() + KEY_SEPARATOR + idempotencyKey;
    }
}