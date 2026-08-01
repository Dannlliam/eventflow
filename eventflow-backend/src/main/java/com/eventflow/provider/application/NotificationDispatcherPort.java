package com.eventflow.provider.application;

import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.eventflow.provider.domain.ProviderType;
import java.util.Map;

/**
 * Port for dispatching notifications via external providers.
 * This is the core abstraction of the Provider Abstraction Layer.
 */
public interface NotificationDispatcherPort {

    /**
     * Dispatches a notification through the external provider.
     *
     * @param providerType the type of provider to use
     * @param recipient the recipient address (email, phone, URL, etc.)
     * @param subject the rendered subject (if applicable)
     * @param body the rendered body content
     * @param notificationId the unique notification ID for idempotency
     * @param additionalHeaders provider-specific additional headers/metadata
     * @return the result of the dispatch operation
     */
    DispatchResultEvent dispatch(ProviderType providerType, String recipient,
                                  String subject, String body,
                                  String notificationId,
                                  Map<String, String> additionalHeaders);
}