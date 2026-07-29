package com.eventflow.analytics.domain;

/**
 * Value object representing a top error statistic.
 */
public record ErrorStat(
    String errorMessage,
    long count
) {
    public ErrorStat {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative");
        }
    }
}