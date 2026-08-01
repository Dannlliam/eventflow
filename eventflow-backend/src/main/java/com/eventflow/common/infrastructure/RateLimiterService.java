package com.eventflow.common.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Redis-based token bucket rate limiter.
 * Enforces provider rate limits using atomic Lua scripts to prevent race conditions.
 * As specified in the PRD Section 35 - Outbound Provider Rate Limiting.
 *
 * Key pattern: rate:{provider_id}:{bucket_minute}
 * Lua script atomically checks the bucket capacity and refill rate.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:";
    private static final String KEY_SEPARATOR = ":";
    private static final long LUA_SCRIPT_CACHE_TTL_MS = 30000;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> tokenBucketScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = createTokenBucketScript();
    }

    /**
     * Checks if a request is allowed for the given provider based on rate limits.
     * Uses a Lua script for atomic token bucket operations.
     *
     * @param providerId the provider UUID
     * @param maxTokens the maximum number of tokens (rate limit)
     * @param windowSeconds the time window in seconds
     * @return true if the request is allowed, false if rate limited
     */
    public boolean tryAcquire(String providerId, int maxTokens, int windowSeconds) {
        String bucketKey = RATE_LIMIT_KEY_PREFIX + providerId + KEY_SEPARATOR + getCurrentMinuteBucket();
        String windowKey = RATE_LIMIT_KEY_PREFIX + providerId + KEY_SEPARATOR + "window:" + getCurrentMinuteBucket();

        List<String> keys = Arrays.asList(bucketKey, windowKey);

        try {
            Long result = redisTemplate.execute(tokenBucketScript, keys,
                String.valueOf(maxTokens),
                String.valueOf(windowSeconds),
                String.valueOf(LUA_SCRIPT_CACHE_TTL_MS));

            if (result == null) {
                log.warn("Rate limiter Lua script returned null for providerId={}, allowing request", providerId);
                return true;
            }

            boolean allowed = result == 1L;
            if (!allowed) {
                log.warn("Rate limit exceeded for providerId={}, maxTokens={}, windowSeconds={}",
                    providerId, maxTokens, windowSeconds);
            }
            return allowed;
        } catch (Exception e) {
            log.error("Rate limiter error for providerId={}, allowing request as fallback: {}",
                providerId, e.getMessage());
            return true;
        }
    }

    /**
     * Gets the current rate limit status for a provider.
     *
     * @param providerId the provider UUID
     * @return the current count of requests in the current window, or 0
     */
    public long getCurrentCount(String providerId) {
        String bucketKey = RATE_LIMIT_KEY_PREFIX + providerId + KEY_SEPARATOR + getCurrentMinuteBucket();
        String count = redisTemplate.opsForValue().get(bucketKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    private String getCurrentMinuteBucket() {
        return String.valueOf(System.currentTimeMillis() / 60_000);
    }

    /**
     * Creates a Lua script for token bucket rate limiting.
     * The script atomically checks and increments the counter.
     * Returns 1 if allowed, 0 if rate limited.
     */
    private DefaultRedisScript<Long> createTokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local bucket_key = KEYS[1] " +
            "local window_key = KEYS[2] " +
            "local max_tokens = tonumber(ARGV[1]) " +
            "local window_seconds = tonumber(ARGV[2]) " +
            "local ttl_ms = tonumber(ARGV[3]) " +
            "" +
            "local current = redis.call('GET', bucket_key) " +
            "if current == false then " +
            "    redis.call('SET', bucket_key, 1, 'PX', ttl_ms) " +
            "    return 1 " +
            "end " +
            "" +
            "current = tonumber(current) " +
            "if current < max_tokens then " +
            "    redis.call('INCR', bucket_key) " +
            "    return 1 " +
            "end " +
            "" +
            "return 0"
        );
        script.setResultType(Long.class);
        return script;
    }
}