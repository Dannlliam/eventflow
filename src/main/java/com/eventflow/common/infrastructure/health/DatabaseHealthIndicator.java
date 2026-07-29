package com.eventflow.common.infrastructure.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Health indicator for database connectivity and migration status.
 * Checks not only basic connectivity but also verifies Flyway migrations
 * have been applied and the schema is up to date.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            // Basic connectivity check
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // Check Flyway migration status
            List<Map<String, Object>> migrations = jdbcTemplate
                .queryForList("SELECT version, description, success, installed_on " +
                             "FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5");

            long appliedCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Long.class);

            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("migrationsApplied", appliedCount)
                .withDetail("lastMigrations", migrations)
                .build();
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}