package com.eventflow.analytics.application;

import com.eventflow.analytics.domain.AnalyticsQuery;
import com.eventflow.analytics.domain.AnalyticsResult;
import com.eventflow.analytics.domain.DailyDeliveryStats;
import com.eventflow.analytics.domain.ErrorStat;
import java.util.List;
import java.util.UUID;

/**
 * Port for analytics data retrieval operations.
 */
public interface AnalyticsRepository {

    /**
     * Retrieves aggregated analytics for a given query.
     */
    AnalyticsResult queryAnalytics(AnalyticsQuery query);

    /**
     * Retrieves daily delivery stats for a workspace within a date range.
     */
    List<DailyDeliveryStats> findDailyStats(UUID workspaceId, String startDate, String endDate, String channel);

    /**
     * Retrieves the top N errors for a workspace within a date range.
     */
    List<ErrorStat> findTopErrors(UUID workspaceId, String startDate, String endDate, int limit);

    /**
     * Refreshes the materialized view for analytics.
     */
    void refreshMaterializedView();
}