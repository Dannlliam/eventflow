package com.eventflow.analytics.infrastructure;

import com.eventflow.analytics.application.AuditLogRepository;
import com.eventflow.analytics.application.AuditLoggingUseCase;
import com.eventflow.analytics.application.AnalyticsRepository;
import com.eventflow.analytics.application.QueryAnalyticsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring configuration for the Analytics bounded context.
 */
@Configuration
public class AnalyticsConfiguration {

    @Bean
    @Primary
    public AnalyticsRepository analyticsRepository(JdbcTemplate jdbcTemplate) {
        return new JpaAnalyticsRepository(jdbcTemplate);
    }

    @Bean
    @Primary
    public AuditLogRepository auditLogRepository(SpringDataAuditLogRepository springDataRepository) {
        return new JpaAuditLogRepository(springDataRepository);
    }

    @Bean
    public QueryAnalyticsUseCase queryAnalyticsUseCase(AnalyticsRepository analyticsRepository) {
        return new QueryAnalyticsUseCase(analyticsRepository);
    }

    @Bean
    public AuditLoggingUseCase auditLoggingUseCase(AuditLogRepository auditLogRepository,
                                                      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new AuditLoggingUseCase(auditLogRepository, objectMapper);
    }
}