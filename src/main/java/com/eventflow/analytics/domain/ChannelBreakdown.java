package com.eventflow.analytics.domain;

/**
 * Value object representing a channel breakdown in analytics.
 */
public record ChannelBreakdown(
    String channel,
    long count,
    double percentage
) {
    public ChannelBreakdown {
        if (percentage < 0.0 || percentage > 100.0) {
            throw new IllegalArgumentException("Percentage must be between 0.0 and 100.0");
        }
    }
}