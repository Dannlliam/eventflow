package com.eventflow.common.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability configuration for EventFlow.
 * Sets up Micrometer metrics, distributed tracing,
 * and structured logging for all bounded contexts.
 *
 * As specified in the PRD Section 62 - Observability.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public NotificationMetrics notificationMetrics(MeterRegistry meterRegistry) {
        return new NotificationMetrics(meterRegistry);
    }

    @Bean
    public ProviderMetrics providerMetrics(MeterRegistry meterRegistry) {
        return new ProviderMetrics(meterRegistry);
    }

    @Bean
    public ApiMetrics apiMetrics(MeterRegistry meterRegistry) {
        return new ApiMetrics(meterRegistry);
    }

    /**
     * Metrics for notification lifecycle tracking.
     */
    public static class NotificationMetrics {
        private final Counter notificationsIngested;
        private final Counter notificationsDelivered;
        private final Counter notificationsFailed;
        private final Counter notificationsDlq;
        private final Timer notificationProcessingTime;
        private final Timer notificationEndToEndLatency;
        private final DistributionSummary notificationPayloadSize;

        public NotificationMetrics(MeterRegistry registry) {
            this.notificationsIngested = Counter.builder("eventflow.notifications.ingested")
                .description("Total notifications ingested")
                .register(registry);

            this.notificationsDelivered = Counter.builder("eventflow.notifications.delivered")
                .description("Total notifications successfully delivered")
                .register(registry);

            this.notificationsFailed = Counter.builder("eventflow.notifications.failed")
                .description("Total notifications failed")
                .tag("cause", "permanent")
                .register(registry);

            this.notificationsDlq = Counter.builder("eventflow.notifications.dlq")
                .description("Total notifications routed to DLQ")
                .register(registry);

            this.notificationProcessingTime = Timer.builder("eventflow.notifications.processing.time")
                .description("Time to process a notification")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

            this.notificationEndToEndLatency = Timer.builder("eventflow.notifications.e2e.latency")
                .description("End-to-end latency from ingestion to delivery")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

            this.notificationPayloadSize = DistributionSummary.builder("eventflow.notifications.payload.size")
                .description("Notification payload size in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        }

        public void recordIngestion() { notificationsIngested.increment(); }
        public void recordDelivery() { notificationsDelivered.increment(); }
        public void recordFailure() { notificationsFailed.increment(); }
        public void recordDlq() { notificationsDlq.increment(); }
        public Timer.Sample startProcessingTimer() { return Timer.start(); }
        public void stopProcessingTimer(Timer.Sample sample) {
            sample.stop(notificationProcessingTime);
        }
        public void recordPayloadSize(int bytes) { notificationPayloadSize.record(bytes); }
    }

    /**
     * Metrics for provider dispatch tracking.
     */
    public static class ProviderMetrics {
        private final Counter dispatchesAttempted;
        private final Counter dispatchesSucceeded;
        private final Counter dispatchesFailed;
        private final Timer dispatchLatency;

        public ProviderMetrics(MeterRegistry registry) {
            this.dispatchesAttempted = Counter.builder("eventflow.providers.dispatches.attempted")
                .description("Total dispatch attempts")
                .register(registry);

            this.dispatchesSucceeded = Counter.builder("eventflow.providers.dispatches.succeeded")
                .description("Total successful dispatches")
                .register(registry);

            this.dispatchesFailed = Counter.builder("eventflow.providers.dispatches.failed")
                .description("Total failed dispatches")
                .register(registry);

            this.dispatchLatency = Timer.builder("eventflow.providers.dispatch.latency")
                .description("Provider dispatch latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);
        }

        public void recordAttempt() { dispatchesAttempted.increment(); }
        public void recordSuccess() { dispatchesSucceeded.increment(); }
        public void recordFailure() { dispatchesFailed.increment(); }
        public Timer.Sample startDispatchTimer() { return Timer.start(); }
        public void stopDispatchTimer(Timer.Sample sample) {
            sample.stop(dispatchLatency);
        }
    }

    /**
     * Metrics for API rate limiting and throttling.
     */
    public static class ApiMetrics {
        private final Counter apiRequests;
        private final Counter apiErrors;
        private final Counter rateLimitHits;

        public ApiMetrics(MeterRegistry registry) {
            this.apiRequests = Counter.builder("eventflow.api.requests")
                .description("Total API requests")
                .register(registry);

            this.apiErrors = Counter.builder("eventflow.api.errors")
                .description("Total API errors")
                .register(registry);

            this.rateLimitHits = Counter.builder("eventflow.api.ratelimit.hits")
                .description("Total rate limit hits")
                .register(registry);
        }

        public void recordRequest() { apiRequests.increment(); }
        public void recordError() { apiErrors.increment(); }
        public void recordRateLimitHit() { rateLimitHits.increment(); }
    }
}