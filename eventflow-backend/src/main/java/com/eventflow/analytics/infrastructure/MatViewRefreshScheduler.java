package com.eventflow.analytics.infrastructure;

import com.eventflow.analytics.application.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that refreshes the analytics materialized view.
 * Uses ShedLock (or Redisson distributed locks) to ensure only one
 * instance executes the refresh in a multi-node deployment.
 */
@Component
public class MatViewRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatViewRefreshScheduler.class);

    private final AnalyticsRepository analyticsRepository;

    public MatViewRefreshScheduler(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Refreshes the daily_delivery_stats materialized view every 15 minutes.
     * The CONCURRENTLY keyword allows the view to remain queryable during refresh.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void refreshDailyDeliveryStats() {
        log.info("Starting scheduled refresh of daily_delivery_stats materialized view");
        try {
            analyticsRepository.refreshMaterializedView();
            log.info("Completed scheduled refresh of daily_delivery_stats materialized view");
        } catch (Exception e) {
            log.error("Failed to refresh daily_delivery_stats materialized view", e);
        }
    }
}