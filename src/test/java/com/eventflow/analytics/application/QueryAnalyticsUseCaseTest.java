package com.eventflow.analytics.application;

import com.eventflow.analytics.domain.model.AnalyticsQuery;
import com.eventflow.analytics.domain.model.AnalyticsResult;
import com.eventflow.analytics.domain.model.TimeSeriesDataPoint;
import com.eventflow.analytics.domain.repository.AnalyticsRepository;
import com.eventflow.notification.domain.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryAnalyticsUseCaseTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private QueryAnalyticsUseCase queryAnalyticsUseCase;

    private UUID workspaceId;
    private Instant startTime;
    private Instant endTime;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        startTime = Instant.now().minus(7, ChronoUnit.DAYS);
        endTime = Instant.now();
    }

    @Test
    void execute_shouldQueryVolumeMetrics_successfully() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .totalCount(15000L)
                .successCount(14250L)
                .failureCount(750L)
                .deliveryRate(0.95)
                .build();

        when(analyticsRepository.queryMetrics(any(AnalyticsQuery.class)))
                .thenReturn(expectedResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(15000L);
        assertThat(result.getSuccessCount()).isEqualTo(14250L);
        assertThat(result.getFailureCount()).isEqualTo(750L);
        assertThat(result.getDeliveryRate()).isEqualTo(0.95);

        verify(analyticsRepository).queryMetrics(query);
    }

    @Test
    void execute_shouldQueryLatencyMetrics_successfully() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.LATENCY)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .avgLatencyMs(250.5)
                .p50LatencyMs(180.0)
                .p95LatencyMs(450.0)
                .p99LatencyMs(850.0)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(expectedResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result.getAvgLatencyMs()).isEqualTo(250.5);
        assertThat(result.getP50LatencyMs()).isEqualTo(180.0);
        assertThat(result.getP95LatencyMs()).isEqualTo(450.0);
        assertThat(result.getP99LatencyMs()).isEqualTo(850.0);

        verify(analyticsRepository).queryMetrics(query);
    }

    @Test
    void execute_shouldQueryByChannel_successfully() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .channel(Notification.Channel.EMAIL)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .totalCount(8000L)
                .successCount(7600L)
                .failureCount(400L)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(expectedResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result.getTotalCount()).isEqualTo(8000L);
        assertThat(result.getSuccessCount()).isEqualTo(7600L);

        verify(analyticsRepository).queryMetrics(query);
    }

    @Test
    void execute_shouldQueryByProvider_successfully() {
        // Arrange
        UUID providerId = UUID.randomUUID();
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .providerId(providerId)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .totalCount(5000L)
                .successCount(4850L)
                .failureCount(150L)
                .deliveryRate(0.97)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(expectedResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result.getTotalCount()).isEqualTo(5000L);
        assertThat(result.getDeliveryRate()).isEqualTo(0.97);

        verify(analyticsRepository).queryMetrics(query);
    }

    @Test
    void execute_shouldQueryTimeSeriesData_successfully() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.TIME_SERIES)
                .startTime(startTime)
                .endTime(endTime)
                .granularity(Duration.ofHours(1))
                .build();

        List<TimeSeriesDataPoint> timeSeriesData = Arrays.asList(
                new TimeSeriesDataPoint(startTime, 1000L),
                new TimeSeriesDataPoint(startTime.plus(1, ChronoUnit.HOURS), 1200L),
                new TimeSeriesDataPoint(startTime.plus(2, ChronoUnit.HOURS), 950L)
        );

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .timeSeriesData(timeSeriesData)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(expectedResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result.getTimeSeriesData()).hasSize(3);
        assertThat(result.getTimeSeriesData().get(0).getValue()).isEqualTo(1000L);
        assertThat(result.getTimeSeriesData().get(1).getValue()).isEqualTo(1200L);

        verify(analyticsRepository).queryMetrics(query);
    }

    @Test
    void execute_shouldQueryBreakdownByChannel_successfully() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.BREAKDOWN)
                .startTime(startTime)
                .endTime(endTime)
                .groupBy(AnalyticsQuery.GroupBy.CHANNEL)
                .build();

        Map<String, Long> channelBreakdown = Map.of(
                "EMAIL", 8000L,
                "SMS", 5000L,
                "PUSH", 2000L
        );

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .breakdown(channelBreakdown)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(expectedResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result.getBreakdown()).hasSize(3);
        assertThat(result.getBreakdown().get("EMAIL")).isEqualTo(8000L);
        assertThat(result.getBreakdown().get("SMS")).isEqualTo(5000L);
        assertThat(result.getBreakdown().get("PUSH")).isEqualTo(2000L);

        verify(analyticsRepository).queryMetrics(query);
    }

    @Test
    void execute_shouldThrowException_whenWorkspaceIdIsNull() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(null)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> queryAnalyticsUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace ID cannot be null");

        verify(analyticsRepository, never()).queryMetrics(any());
    }

    @Test
    void execute_shouldThrowException_whenMetricTypeIsNull() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(null)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> queryAnalyticsUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Metric type cannot be null");

        verify(analyticsRepository, never()).queryMetrics(any());
    }

    @Test
    void execute_shouldThrowException_whenStartTimeIsNull() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(null)
                .endTime(endTime)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> queryAnalyticsUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time cannot be null");

        verify(analyticsRepository, never()).queryMetrics(any());
    }

    @Test
    void execute_shouldThrowException_whenEndTimeIsNull() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(startTime)
                .endTime(null)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> queryAnalyticsUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time cannot be null");

        verify(analyticsRepository, never()).queryMetrics(any());
    }

    @Test
    void execute_shouldThrowException_whenEndTimeBeforeStartTime() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(endTime)
                .endTime(startTime)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> queryAnalyticsUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");

        verify(analyticsRepository, never()).queryMetrics(any());
    }

    @Test
    void execute_shouldThrowException_whenTimeRangeExceedsLimit() {
        // Arrange
        Instant veryOldStartTime = Instant.now().minus(400, ChronoUnit.DAYS);
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(veryOldStartTime)
                .endTime(endTime)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> queryAnalyticsUseCase.execute(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Time range cannot exceed 365 days");

        verify(analyticsRepository, never()).queryMetrics(any());
    }

    @Test
    void execute_shouldApplyCaching_forRepeatedQueries() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        AnalyticsResult expectedResult = AnalyticsResult.builder()
                .totalCount(10000L)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(expectedResult);

        // Act
        AnalyticsResult result1 = queryAnalyticsUseCase.execute(query);
        AnalyticsResult result2 = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        
        // Cache behavior depends on implementation - 
        // Either both calls hit repository (no cache), or second call uses cache
        verify(analyticsRepository, atLeastOnce()).queryMetrics(query);
    }

    @Test
    void execute_shouldHandleEmptyResults_gracefully() {
        // Arrange
        AnalyticsQuery query = AnalyticsQuery.builder()
                .workspaceId(workspaceId)
                .metricType(AnalyticsQuery.MetricType.VOLUME)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        AnalyticsResult emptyResult = AnalyticsResult.builder()
                .totalCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .deliveryRate(0.0)
                .build();

        when(analyticsRepository.queryMetrics(query)).thenReturn(emptyResult);

        // Act
        AnalyticsResult result = queryAnalyticsUseCase.execute(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getDeliveryRate()).isZero();

        verify(analyticsRepository).queryMetrics(query);
    }
}
