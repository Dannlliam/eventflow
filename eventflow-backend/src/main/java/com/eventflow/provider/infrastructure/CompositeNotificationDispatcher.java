package com.eventflow.provider.infrastructure;

import com.eventflow.common.infrastructure.RateLimiterService;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.provider.application.NotificationDispatcherPort;
import com.eventflow.provider.domain.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Composite dispatcher that routes notification dispatch to the appropriate
 * provider-specific implementation based on ProviderType.
 * <p>
 * This is the core of the Provider Abstraction Layer.
 * Each provider is implemented as a separate adapter.
 * Integrates rate limiting and observability metrics.
 */
@Component
public class CompositeNotificationDispatcher implements NotificationDispatcherPort {

    private static final Logger log = LoggerFactory.getLogger(CompositeNotificationDispatcher.class);

    private final Map<ProviderType, ProviderAdapter> adapters;
    private final RateLimiterService rateLimiterService;

    public CompositeNotificationDispatcher(SendGridEmailDispatcher sendGrid,
                                           TwilioSmsDispatcher twilio,
                                           FcmPushDispatcher fcm,
                                           WebhookDispatcher webhook,
                                           RateLimiterService rateLimiterService) {
        this.adapters = Map.of(
            ProviderType.SENDGRID, sendGrid,
            ProviderType.AMAZON_SES, sendGrid,
            ProviderType.MAILGUN, sendGrid,
            ProviderType.TWILIO, twilio,
            ProviderType.AMAZON_SNS, twilio,
            ProviderType.PLIVO, twilio,
            ProviderType.FCM, fcm,
            ProviderType.APNS, fcm,
            ProviderType.WEBHOOK, webhook
        );
        this.rateLimiterService = rateLimiterService;
        log.info("CompositeNotificationDispatcher initialized with {} adapters", adapters.size());
    }

    @Override
    public DispatchResultEvent dispatch(ProviderType providerType, String recipient,
                                         String subject, String body,
                                         String notificationId,
                                         Map<String, String> additionalHeaders) {
        UUID notificationIdObj = UUID.fromString(notificationId);
        UUID workspaceId = additionalHeaders.containsKey("workspaceId")
            ? UUID.fromString(additionalHeaders.get("workspaceId"))
            : UUID.randomUUID();

        ProviderAdapter adapter = adapters.get(providerType);
        if (adapter == null) {
            log.error("No adapter found for provider type: {}", providerType);
            return DispatchResultEvent.permanentFailure(
                notificationIdObj, workspaceId, providerType.name(), 0,
                "Unsupported provider type: " + providerType
            );
        }

        // Apply rate limiting before dispatching
        String providerRateLimitKey = providerType.name() + ":" + workspaceId;
        // Default rate limit: 60 requests per minute if not configured
        int maxTokens = 60;
        int windowSeconds = 60;

        // Check for provider-specific rate limit from additionalHeaders
        if (additionalHeaders.containsKey("rateLimit")) {
            maxTokens = Integer.parseInt(additionalHeaders.get("rateLimit"));
        }
        if (additionalHeaders.containsKey("rateLimitDuration")) {
            windowSeconds = Integer.parseInt(additionalHeaders.get("rateLimitDuration"));
        }

        if (!rateLimiterService.tryAcquire(providerRateLimitKey, maxTokens, windowSeconds)) {
            log.warn("Rate limit exceeded for providerType={}, workspaceId={}", providerType, workspaceId);
            return DispatchResultEvent.transientFailure(
                notificationIdObj, workspaceId, providerType.name(), 429,
                "Rate limit exceeded for provider " + providerType
            );
        }

        log.info("Dispatching via {}: recipient={}, notificationId={}", providerType, recipient, notificationId);
        return adapter.dispatch(recipient, subject, body, notificationId, additionalHeaders);
    }
}
