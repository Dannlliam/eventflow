package com.eventflow.analytics.interfaces;

import com.eventflow.analytics.application.QueryAnalyticsUseCase;
import com.eventflow.analytics.domain.AnalyticsResult;
import com.eventflow.analytics.domain.ChannelBreakdown;
import com.eventflow.analytics.domain.DailyDeliveryStats;
import com.eventflow.analytics.domain.ErrorStat;
import com.eventflow.common.infrastructure.WorkspaceContextProvider;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for analytics queries.
 * Provides aggregated delivery metrics for the Admin Dashboard.
 */
@Controller
public class AnalyticsGraphQLResolver {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsGraphQLResolver.class);

    private final QueryAnalyticsUseCase queryAnalyticsUseCase;
    private final WorkspaceContextProvider workspaceContextProvider;

    public AnalyticsGraphQLResolver(QueryAnalyticsUseCase queryAnalyticsUseCase,
                                     WorkspaceContextProvider workspaceContextProvider) {
        this.queryAnalyticsUseCase = queryAnalyticsUseCase;
        this.workspaceContextProvider = workspaceContextProvider;
    }

    @QueryMapping
    public AnalyticsPayload analytics(@Argument @NotBlank String startDate,
                                      @Argument @NotBlank String endDate,
                                      @Argument String channel,
                                      @Argument String providerId) {
        UUID workspaceId = workspaceContextProvider.getCurrentWorkspaceId();

        log.info("Analytics query: startDate={}, endDate={}, channel={}", startDate, endDate, channel);

        AnalyticsResult result = queryAnalyticsUseCase.execute(
            workspaceId, startDate, endDate, channel, providerId);

        return new AnalyticsPayload(
            result.totalSent(),
            result.totalDelivered(),
            result.totalFailed(),
            result.totalDlq(),
            result.deliveryRate(),
            result.dailyStats().stream()
                .map(d -> new DailyStatsPayload(
                    d.workspaceId().toString(),
                    d.date().toString(),
                    d.channel(),
                    d.providerId().toString(),
                    d.totalSent(),
                    d.totalDelivered(),
                    d.totalFailed(),
                    d.totalDlq(),
                    d.deliveryRate()
                )).toList(),
            result.channelBreakdown().stream()
                .map(c -> new ChannelBreakdownPayload(c.channel(), c.count(), c.percentage()))
                .toList(),
            result.topErrors().stream()
                .map(e -> new ErrorStatPayload(e.errorMessage(), e.count()))
                .toList()
        );
    }

    // GraphQL-compatible DTOs

    public record AnalyticsPayload(
        long totalSent,
        long totalDelivered,
        long totalFailed,
        long totalDlq,
        double deliveryRate,
        List<DailyStatsPayload> dailyStats,
        List<ChannelBreakdownPayload> channelBreakdown,
        List<ErrorStatPayload> topErrors
    ) {}

    public record DailyStatsPayload(
        String workspaceId,
        String date,
        String channel,
        String providerId,
        long totalSent,
        long totalDelivered,
        long totalFailed,
        long totalDlq,
        double deliveryRate
    ) {}

    public record ChannelBreakdownPayload(
        String channel,
        long count,
        double percentage
    ) {}

    public record ErrorStatPayload(
        String errorMessage,
        long count
    ) {}
}