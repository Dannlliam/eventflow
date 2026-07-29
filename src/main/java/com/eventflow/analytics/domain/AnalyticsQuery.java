package com.eventflow.analytics.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Value object representing an analytics query with filters.
 */
public record AnalyticsQuery(
    UUID workspaceId,
    LocalDate startDate,
    LocalDate endDate,
    Optional<String> channel,
    Optional<UUID> providerId
) {

    public AnalyticsQuery {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate must not be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
    }
}