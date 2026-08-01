package com.eventflow;

import com.eventflow.common.infrastructure.EventFlowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EventFlow - Enterprise-grade event-driven notification orchestration platform.
 * <p>
 * Main application entry point. This modular monolith follows Clean Architecture
 * and Domain-Driven Design principles, organized into strictly bounded contexts.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(EventFlowProperties.class)
public class EventFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventFlowApplication.class, args);
    }
}