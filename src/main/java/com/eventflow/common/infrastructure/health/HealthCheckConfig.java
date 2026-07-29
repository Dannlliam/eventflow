package com.eventflow.common.infrastructure.health;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for composite health checks.
 * Provides a summary endpoint that aggregates all bounded context health statuses.
 */
@Configuration
public class HealthCheckConfig {

    /**
     * Returns a map of all bounded context health check endpoints.
     * Used for the readiness probe to ensure all subsystems are operational.
     */
    @Bean
    public Map<String, String> boundedContextHealthEndpoints() {
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("identity", "/actuator/health/identity");
        endpoints.put("notification", "/actuator/health/notification");
        endpoints.put("template", "/actuator/health/template");
        endpoints.put("provider", "/actuator/health/provider");
        endpoints.put("analytics", "/actuator/health/analytics");
        return endpoints;
    }
}