package com.eventflow.analytics.infrastructure;

import com.eventflow.analytics.application.AnalyticsRepository;
import com.eventflow.analytics.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JPA/JDBC implementation of the AnalyticsRepository port.
 * Queries the daily_delivery_stats materialized view for fast aggregation.
 */
@Repository
public class JpaAnalyticsRepository implements AnalyticsRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaAnalyticsRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public JpaAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AnalyticsResult queryAnalytics(AnalyticsQuery query) {
        List<DailyDeliveryStats> dailyStats = findDailyStats(
            query.workspaceId(),
            query.startDate().toString(),
            query.endDate().toString(),
            query.channel().orElse(null)
        );

        // Aggregate totals
        long totalSent = dailyStats.stream().mapToLong(DailyDeliveryStats::totalSent).sum();
        long totalDelivered = dailyStats.stream().mapToLong(DailyDeliveryStats::totalDelivered).sum();
        long totalFailed = dailyStats.stream().mapToLong(DailyDeliveryStats::totalFailed).sum();
        long totalDlq = dailyStats.stream().mapToLong(DailyDeliveryStats::totalDlq).sum();
        double deliveryRate = totalSent > 0 ? (double) totalDelivered / totalSent * 100.0 : 0.0;

        // Channel breakdown
        List<ChannelBreakdown> channelBreakdown = computeChannelBreakdown(query);

        // Top errors
        List<ErrorStat> topErrors = findTopErrors(query.workspaceId(), query.startDate().toString(),
            query.endDate().toString(), 10);

        return new AnalyticsResult(
            totalSent, totalDelivered, totalFailed, totalDlq,
            deliveryRate, dailyStats, channelBreakdown, topErrors
        );
    }

    @Override
    public List<DailyDeliveryStats> findDailyStats(UUID workspaceId, String startDate,
                                                     String endDate, String channel) {
        String sql = """
            SELECT workspace_id, date, channel, provider_id, total_sent, total_delivered, total_failed, total_dlq
            FROM eventflow.daily_delivery_stats
            WHERE workspace_id = ?
              AND date >= ?::date
              AND date <= ?::date
              AND (CAST(? AS TEXT) IS NULL OR channel = CAST(? AS TEXT))
            ORDER BY date ASC, channel ASC
            """;

        return jdbcTemplate.query(sql,
            new Object[]{workspaceId, startDate, endDate, channel, channel},
            (rs, rowNum) -> new DailyDeliveryStats(
                UUID.fromString(rs.getString("workspace_id")),
                rs.getDate("date").toLocalDate(),
                rs.getString("channel"),
                rs.getObject("provider_id", UUID.class),
                rs.getLong("total_sent"),
                rs.getLong("total_delivered"),
                rs.getLong("total_failed"),
                rs.getLong("total_dlq")
            ));
    }

    @Override
    public List<ErrorStat> findTopErrors(UUID workspaceId, String startDate,
                                          String endDate, int limit) {
        String sql = """
            SELECT ne.error_message, COUNT(*) AS error_count
            FROM eventflow.notification_events ne
            JOIN eventflow.notifications n ON n.id = ne.notification_id
            WHERE n.workspace_id = ?
              AND ne.event_type = 'FAILED'
              AND ne.error_message IS NOT NULL
              AND ne.created_at >= ?::timestamptz
              AND ne.created_at <= ?::timestamptz
            GROUP BY ne.error_message
            ORDER BY error_count DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql,
            new Object[]{workspaceId, startDate + " 00:00:00", endDate + " 23:59:59", limit},
            (rs, rowNum) -> new ErrorStat(
                rs.getString("error_message"),
                rs.getLong("error_count")
            ));
    }

    @Override
    @Transactional
    public void refreshMaterializedView() {
        log.info("Refreshing materialized view: daily_delivery_stats");
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY eventflow.daily_delivery_stats");
    }

    private List<ChannelBreakdown> computeChannelBreakdown(AnalyticsQuery query) {
        String sql = """
            SELECT channel, COUNT(*) AS cnt
            FROM eventflow.notifications
            WHERE workspace_id = ?
              AND created_at >= ?::timestamptz
              AND created_at <= ?::timestamptz
              AND (CAST(? AS TEXT) IS NULL OR channel = CAST(? AS TEXT))
            GROUP BY channel
            ORDER BY cnt DESC
            """;

        List<ChannelBreakdown> breakdowns = jdbcTemplate.query(sql,
            new Object[]{
                query.workspaceId(),
                query.startDate().toString() + " 00:00:00",
                query.endDate().toString() + " 23:59:59",
                query.channel().orElse(null),
                query.channel().orElse(null)
            },
            (rs, rowNum) -> {
                long count = rs.getLong("cnt");
                return new ChannelBreakdown(rs.getString("channel"), count, 0.0);
            });

        long total = breakdowns.stream().mapToLong(ChannelBreakdown::count).sum();
        if (total > 0) {
            return breakdowns.stream()
                .map(b -> new ChannelBreakdown(b.channel(), b.count(),
                    (double) b.count() / total * 100.0))
                .toList();
        }
        return breakdowns;
    }
}