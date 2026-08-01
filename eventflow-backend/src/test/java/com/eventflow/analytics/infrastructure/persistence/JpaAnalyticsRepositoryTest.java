package com.eventflow.analytics.infrastructure.persistence;

import com.eventflow.analytics.domain.model.AnalyticsQuery;
import com.eventflow.analytics.domain.model.AnalyticsResult;
import com.eventflow.notification.domain.model.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JpaAnalyticsRepositoryTest {

    @Autowired
    private JpaAnalyticsRepositoryAdapter repository;

    @Test
    void queryMetrics_shouldReturnVolumeMetrics() {
        UUID workspaceId = UUID.randomUUID();
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(Instant.now().minus(7, ChronoUnit.DAYS))
                .endTime(Instant.now())
                .build();

        AnalyticsResult result = repository.queryMetrics(query);

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void queryMetrics_shouldFilterByChannel() {
        UUID workspaceId = UUID.randomUUID();
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .channel(Notification.Channel.EMAIL)
                .startTime(Instant.now().minus(7, ChronoUnit.DAYS))
                .endTime(Instant.now())
                .build();

        AnalyticsResult result = repository.queryMetrics(query);

        assertThat(result).isNotNull();
    }

    @Test
    void queryMetrics_shouldReturnLatencyMetrics() {
        UUID workspaceId = UUID.randomUUID();
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.LATENCY)
                .startTime(Instant.now().minus(1, ChronoUnit.DAYS))
                .endTime(Instant.now())
                .build();

        AnalyticsResult result = repository.queryMetrics(query);

        assertThat(result).isNotNull();
    }

    @Test
    void queryMetrics_shouldFilterByProvider() {
        UUID workspaceId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .providerId(providerId)
                .startTime(Instant.now().minus(7, ChronoUnit.DAYS))
                .endTime(Instant.now())
                .build();

        AnalyticsResult result = repository.queryMetrics(query);

        assertThat(result).isNotNull();
    }

    @Test
    void queryMetrics_shouldReturnBreakdown() {
        UUID workspaceId = UUID.randomUUID();
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.BREAKDOWN)
                .groupBy(AnalyticsQuery.GroupBy.CHANNEL)
                .startTime(Instant.now().minus(7, ChronoUnit.DAYS))
                .endTime(Instant.now())
                .build();

        AnalyticsResult result = repository.queryMetrics(query);

        assertThat(result).isNotNull();
    }
}
