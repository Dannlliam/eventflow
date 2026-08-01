package com.eventflow.analytics.application;

import com.eventflow.analytics.domain.AnalyticsQuery;
import com.eventflow.analytics.domain.AnalyticsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for querying analytics data.
 * Validates the query parameters and delegates to the repository.
 */
public class QueryAnalyticsUseCase {

    private static final Logger log = LoggerFactory.getLogger(QueryAnalyticsUseCase.class);

    private final AnalyticsRepository analyticsRepository;

    public QueryAnalyticsUseCase(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Executes an analytics query with the given parameters.
     */
    public AnalyticsResult execute(UUID workspaceId, String startDate, String endDate,
                                    String channel, String providerId) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        AnalyticsQuery query = new AnalyticsQuery(
            workspaceId,
            start,
            end,
            Optional.ofNullable(channel),
            providerId != null ? Optional.of(UUID.fromString(providerId)) : Optional.empty()
        );

        log.info("Querying analytics: workspaceId={}, startDate={}, endDate={}, channel={}",
            workspaceId, startDate, endDate, channel);

        return analyticsRepository.queryAnalytics(query);
    }
}