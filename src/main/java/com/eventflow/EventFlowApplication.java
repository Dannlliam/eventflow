package com.eventflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class EventFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventFlowApplication.class, args);
    }
}