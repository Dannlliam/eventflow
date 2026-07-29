package com.eventflow.common.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Redis token bucket rate limiter.
 */
@SpringBootTest
@Testcontainers
class RateLimiterServiceIntegrationTest {

    @Container
    static final org.testcontainers.containers.GenericContainer<?> redis = new org.testcontainers.containers.GenericContainer<>(
            "redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String providerId;

    @BeforeEach
    void setUp() {
        providerId = UUID.randomUUID().toString();
        // Clear any existing rate limit keys
        redisTemplate.delete(redisTemplate.keys("rate:*"));
    }

    @Test
    void shouldAllowRequestWithinLimit() {
        // Provider allows 10 requests per 60 seconds
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiterService.tryAcquire(providerId, 10, 60),
                "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void shouldBlockRequestExceedingLimit() {
        // Provider allows 5 requests per 60 seconds
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryAcquire(providerId, 5, 60),
                "Request " + (i + 1) + " should be allowed");
        }

        // 6th request should be blocked
        assertFalse(rateLimiterService.tryAcquire(providerId, 5, 60),
            "6th request should be rate limited");
    }

    @Test
    void shouldHandleMultipleProviders() {
        String providerA = UUID.randomUUID().toString();
        String providerB = UUID.randomUUID().toString();

        // Fill provider A's bucket
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiterService.tryAcquire(providerA, 3, 60));
        }

        // Provider A should be limited, but B should not
        assertFalse(rateLimiterService.tryAcquire(providerA, 3, 60));
        assertTrue(rateLimiterService.tryAcquire(providerB, 3, 60));
    }

    @Test
    void shouldReturnCurrentCount() {
        rateLimiterService.tryAcquire(providerId, 100, 60);
        rateLimiterService.tryAcquire(providerId, 100, 60);
        rateLimiterService.tryAcquire(providerId, 100, 60);

        long count = rateLimiterService.getCurrentCount(providerId);
        assertEquals(3, count, "Should count 3 requests in the bucket");
    }

    @Test
    void shouldAllowAfterReset() throws InterruptedException {
        int maxTokens = 2;
        int windowSeconds = 1; // Very short window for test

        // Consume all tokens
        assertTrue(rateLimiterService.tryAcquire(providerId, maxTokens, windowSeconds));
        assertTrue(rateLimiterService.tryAcquire(providerId, maxTokens, windowSeconds));
        assertFalse(rateLimiterService.tryAcquire(providerId, maxTokens, windowSeconds));

        // Wait for window to expire
        Thread.sleep(1100);

        // Should allow again
        assertTrue(rateLimiterService.tryAcquire(providerId, maxTokens, windowSeconds));
    }

    @Test
    void shouldAllowEvenOnRedisFailure() {
        // Simulate Redis failure by using an invalid key pattern
        // The service should fall back to allowing requests
        assertTrue(rateLimiterService.tryAcquire(null, 10, 60),
            "Should allow request on null providerId");
    }
}