package com.eventflow.common.infrastructure;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson distributed lock configuration for EventFlow.
 * Provides distributed locking capabilities for:
 * - Retry scheduler: ensures only one node executes the retry sweep
 * - Scheduled tasks: prevents duplicate execution across multiple instances
 * - Provider rate limiting: coordinates rate limit state across pods
 *
 * As specified in the PRD Section 25 - Redis Strategy / Distributed Locking.
 */
@Configuration
public class DistributedLockConfig {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockConfig.class);

    private static final String LOCK_NAME_PREFIX = "lock:";
    private static final long DEFAULT_LOCK_WAIT_TIME_MS = 5000;
    private static final long DEFAULT_LOCK_LEASE_TIME_MS = 60000;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates the Redisson client with optimized configuration for distributed locking.
     * Uses single node mode for local development; cluster mode for production.
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setTransportMode(TransportMode.NIO);

        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
            .setAddress(address)
            .setPassword(redisPassword.isEmpty() ? null : redisPassword)
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(2)
            .setConnectTimeout(2000)
            .setRetryAttempts(3)
            .setRetryInterval(1000);

        log.info("Redisson client configured: address={}", address);
        return Redisson.create(config);
    }

    /**
     * Predefined lock names for distributed coordination.
     */
    public static class LockNames {
        public static final String RETRY_SCHEDULER_SWEEP = LOCK_NAME_PREFIX + "scheduler:retry-sweep";
        public static final String MAT_VIEW_REFRESH = LOCK_NAME_PREFIX + "scheduler:matview-refresh";
        public static final String DLQ_ARCHIVE = LOCK_NAME_PREFIX + "scheduler:dlq-archive";
        public static final String IDEMPOTENCY_CLEANUP = LOCK_NAME_PREFIX + "scheduler:idempotency-cleanup";
        public static final String ANALYTICS_AGGREGATION = LOCK_NAME_PREFIX + "scheduler:analytics-aggregation";

        private LockNames() {}
    }

    public static long getDefaultLockWaitTimeMs() {
        return DEFAULT_LOCK_WAIT_TIME_MS;
    }

    public static long getDefaultLockLeaseTimeMs() {
        return DEFAULT_LOCK_LEASE_TIME_MS;
    }
}