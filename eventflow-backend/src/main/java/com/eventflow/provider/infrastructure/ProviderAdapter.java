package com.eventflow.provider.infrastructure;

import com.eventflow.notification.domain.events.DispatchResultEvent;

import java.util.Map;

/**
 * Interface for provider-specific adapter implementations.
 * Each provider implements this interface to handle the actual API calls.
 */
public interface ProviderAdapter {

    /**
     * Dispatches a notification via the external provider.
     *
     * @param recipient the recipient address (email, phone, URL, device token)
     * @param subject the rendered subject (if applicable)
     * @param body the rendered body content
     * @param notificationId the notification ID for idempotency
     * @param additionalHeaders provider-specific metadata
     * @return the result of the dispatch operation
     */
    DispatchResultEvent dispatch(String recipient, String subject, String body,
                                  String notificationId, Map<String, String> additionalHeaders);
}
