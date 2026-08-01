package com.eventflow.analytics.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for the Analytics domain value objects.
 */
@DisplayName("Analytics domain value objects")
class AnalyticsResultTest {

    @Test
    @DisplayName("should create analytics result with aggregated data")
    void constructor_ValidData_CreatesInstance() {
        DailyDeliveryStats dailyStat = new DailyDeliveryStats(
            UUID.randomUUID(), LocalDate.now(), "EMAIL",
            UUID.randomUUID(), 100, 95, 5, 0);

        List<DailyDeliveryStats> dailyStats = List.of(dailyStat);
        List<ChannelBreakdown> channelBreakdown = List.of(
            new ChannelBreakdown("EMAIL", 100, 100.0));
        List<ErrorStat> topErrors = List.of(
            new ErrorStat("Rate limit exceeded", 3));

        AnalyticsResult result = new AnalyticsResult(
            100, 95, 5, 0, 95.0, dailyStats, channelBreakdown, topErrors);

        assertEquals(100, result.totalSent());
        assertEquals(95, result.totalDelivered());
        assertEquals(5, result.totalFailed());
        assertEquals(0, result.totalDlq());
        assertEquals(95.0, result.deliveryRate(), 0.01);
    }

    @Test
    @DisplayName("should throw exception for null dailyStats")
    void constructor_NullDailyStats_ThrowsException() {
        List<ChannelBreakdown> cb = List.of(new ChannelBreakdown("EMAIL", 0, 0.0));
        List<ErrorStat> errors = List.of();
        assertThrows(IllegalArgumentException.class, () ->
            new AnalyticsResult(0, 0, 0, 0, 0.0, null, cb, errors));
    }

    @Test
    @DisplayName("should throw exception for null channelBreakdown")
    void constructor_NullChannelBreakdown_ThrowsException() {
        List<DailyDeliveryStats> ds = List.of();
        List<ErrorStat> errors = List.of();
        assertThrows(IllegalArgumentException.class, () ->
            new AnalyticsResult(0, 0, 0, 0, 0.0, ds, null, errors));
    }

    @Test
    @DisplayName("should calculate delivery rate for daily stats")
    void deliveryRate_CalculatesCorrectly() {
        DailyDeliveryStats stats = new DailyDeliveryStats(
            UUID.randomUUID(), LocalDate.now(), "EMAIL",
            UUID.randomUUID(), 200, 180, 20, 0);
        assertEquals(90.0, stats.deliveryRate(), 0.01);
    }

    @Test
    @DisplayName("should return 0 delivery rate when total sent is 0")
    void deliveryRate_ZeroSent_ReturnsZero() {
        DailyDeliveryStats stats = new DailyDeliveryStats(
            UUID.randomUUID(), LocalDate.now(), "SMS",
            UUID.randomUUID(), 0, 0, 0, 0);
        assertEquals(0.0, stats.deliveryRate(), 0.01);
    }

    @Test
    @DisplayName("should create channel breakdown with valid percentage range")
    void channelBreakdown_ValidPercentage_CreatesInstance() {
        assertDoesNotThrow(() -> new ChannelBreakdown("EMAIL", 50, 50.0));
        assertDoesNotThrow(() -> new ChannelBreakdown("SMS", 50, 0.0));
        assertDoesNotThrow(() -> new ChannelBreakdown("PUSH", 0, 100.0));
    }

    @Test
    @DisplayName("should throw exception for invalid channel percentage")
    void channelBreakdown_InvalidPercentage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ChannelBreakdown("EMAIL", 50, -1.0));
        assertThrows(IllegalArgumentException.class, () ->
            new ChannelBreakdown("EMAIL", 50, 101.0));
    }

    @Test
    @DisplayName("should create error stat with non-negative count")
    void errorStat_ValidCount_CreatesInstance() {
        ErrorStat stat = new ErrorStat("Error occurred", 5);
        assertEquals("Error occurred", stat.errorMessage());
        assertEquals(5, stat.count());
    }

    @Test
    @DisplayName("should throw exception for negative error count")
    void errorStat_NegativeCount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new ErrorStat("Error", -1));
    }
}