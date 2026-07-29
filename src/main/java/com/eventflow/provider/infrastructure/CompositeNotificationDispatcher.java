package com.eventflow.provider.infrastructure;

import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.provider.application.NotificationDispatcherPort;
import com.eventflow.provider.domain.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Composite dispatcher that routes notification dispatch to the appropriate
 * provider-specific implementation based on ProviderType.
 * <p>
 * This is the core of the Provider Abstraction Layer.
 * Each provider is implemented as a separate adapter.
 */
@Component
public class CompositeNotificationDispatcher implements NotificationDispatcherPort {

    private static final Logger log = LoggerFactory.getLogger(CompositeNotificationDispatcher.class);

    private final Map<ProviderType, ProviderAdapter> adapters;

    public CompositeNotificationDispatcher(SendGridEmailDispatcher sendGrid,
                                           TwilioSmsDispatcher twilio,
                                           FcmPushDispatcher fcm,
                                           WebhookDispatcher webhook) {
        this.adapters = Map.of(
            ProviderType.SENDGRID, sendGrid,
            ProviderType.AMAZON_SES, sendGrid, // SES uses SendGrid adapter pattern
            ProviderType.MAILGUN, sendGrid,    // Mailgun uses similar API pattern
            ProviderType.TWILIO, twilio,
            ProviderType.AMAZON_SNS, twilio,   // SNS uses similar pattern
            ProviderType.PLIVO, twilio,        // Plivo uses similar pattern
            ProviderType.FCM, fcm,
            ProviderType.APNS, fcm,            // APNS uses similar pattern
            ProviderType.WEBHOOK, webhook
        );
        log.info("CompositeNotificationDispatcher initialized with {} adapters", adapters.size());
    }

    @Override
    public DispatchResultEvent dispatch(ProviderType providerType, String recipient,
                                         String subject, String body,
                                         String notificationId,
                                         Map<String, String> additionalHeaders) {
        ProviderAdapter adapter = adapters.get(providerType);
        if (adapter == null) {
            log.error("No adapter found for provider type: {}", providerType);
            return DispatchResultEvent.permanentFailure(
                java.util.UUID.fromString(notificationId),
                java.util.UUID.randomUUID(), // workspaceId unknown at this point
                providerType.name(),
                0,
                "Unsupported provider type: " + providerType
            );
        }

        log.info("Dispatching via {}: recipient={}, notificationId={}", providerType, recipient, notificationId);
        return adapter.dispatch(recipient, subject, body, notificationId, additionalHeaders);
    }
}
