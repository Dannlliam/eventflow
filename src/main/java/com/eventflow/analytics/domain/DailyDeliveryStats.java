package com.eventflow.analytics.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Value object representing daily delivery statistics.
 * Maps to the daily_delivery_stats materialized view.
 */
public record DailyDeliveryStats(
    UUID workspaceId,
    LocalDate date,
    String channel,
    UUID providerId,
    long totalSent,
    long totalDelivered,
    long totalFailed,
    long totalDlq
) {

    /**
     * Calculates the delivery rate as a percentage.
     */
    public double deliveryRate() {
        if (totalSent == 0) return 0.0;
        return (double) totalDelivered / totalSent * 100.0;
    }
}