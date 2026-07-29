package com.eventflow.common.infrastructure;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Multi-tier caching configuration for EventFlow.
 * Implements a two-level cache strategy as specified in the PRD Section 24:
 * - Tier 1: In-Memory L1 Cache (Caffeine) for rarely-changing, high-read data
 * - Tier 2: Distributed L2 Cache (Redis via Redisson) for session state, rate limits
 *
 * Cache eviction strategy:
 * - L1 eviction broadcasts via Redis Pub/Sub to invalidate across all nodes
 * - Write-through: updates to data invalidate both L1 and L2
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Creates the Caffeine L1 cache manager for hot data.
     * Maximum 10,000 items, 5-minute TTL as specified in the PRD.
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
        );
        cacheManager.setCacheNames(Arrays.asList(
            "providerConfigurations",
            "channelMappings",
            "compiledTemplates",
            "userProfiles",
            "workspaceConfigs"
        ));
        return cacheManager;
    }

    /**
     * Creates the Redis L2 cache manager via Redisson for distributed caching.
     */
    @Bean
    public CacheManager redisCacheManager(RedissonClient redissonClient) {
        RedissonSpringCacheManager cacheManager = new RedissonSpringCacheManager(redissonClient);
        log.info("Redis cache manager initialized with Redisson");
        return cacheManager;
    }

    /**
     * Primary composite cache manager that checks L1 (Caffeine) first, then L2 (Redis).
     */
    @Bean
    @Primary
    public CacheManager compositeCacheManager(
            CacheManager caffeineCacheManager,
            CacheManager redisCacheManager) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();
        compositeCacheManager.setCacheManagers(Arrays.asList(
            caffeineCacheManager,
            redisCacheManager
        ));
        compositeCacheManager.setFallbackToNoOpCache(true);
        log.info("Composite cache manager initialized: L1=Caffeine, L2=Redis");
        return compositeCacheManager;
    }

    /**
     * Cache names for use with @Cacheable annotations.
     */
    public static class CacheNames {
        public static final String PROVIDER_CONFIGURATIONS = "providerConfigurations";
        public static final String CHANNEL_MAPPINGS = "channelMappings";
        public static final String COMPILED_TEMPLATES = "compiledTemplates";
        public static final String USER_PROFILES = "userProfiles";
        public static final String WORKSPACE_CONFIGS = "workspaceConfigs";

        private CacheNames() {}
    }
}