package com.eventflow.analytics.domain;

import java.util.List;

/**
 * Value object representing the aggregated analytics result.
 */
public record AnalyticsResult(
    long totalSent,
    long totalDelivered,
    long totalFailed,
    long totalDlq,
    double deliveryRate,
    List<DailyDeliveryStats> dailyStats,
    List<ChannelBreakdown> channelBreakdown,
    List<ErrorStat> topErrors
) {

    public AnalyticsResult {
        if (dailyStats == null) {
            throw new IllegalArgumentException("dailyStats must not be null");
        }
        if (channelBreakdown == null) {
            throw new IllegalArgumentException("channelBreakdown must not be null");
        }
        if (topErrors == null) {
            throw new IllegalArgumentException("topErrors must not be null");
        }
    }
}