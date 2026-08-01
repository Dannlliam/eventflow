package com.eventflow.notification.domain;

import com.eventflow.common.domain.Channel;
import com.eventflow.common.domain.DomainValidationException;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing recipient information for a notification.
 * Supports email, phone, deviceToken, and webhookUrl based on channel.
 */
public record Recipient(String email, String phone, String deviceToken, String webhookUrl) {

    public Recipient {
        // At least one contact method must be specified
        if (email == null && phone == null && deviceToken == null && webhookUrl == null) {
            throw new DomainValidationException(
                "INVALID_RECIPIENT",
                "At least one of email, phone, deviceToken, or webhookUrl must be specified",
                "recipient"
            );
        }
    }

    /**
     * Validates that the recipient has the required fields for the given channel.
     */
    public void validateForChannel(Channel channel) {
        switch (channel) {
            case EMAIL -> {
                if (email == null || email.isBlank()) {
                    throw new DomainValidationException(
                        "MISSING_RECIPIENT_EMAIL",
                        "Email must be specified for EMAIL channel",
                        "recipient.email"
                    );
                }
            }
            case SMS -> {
                if (phone == null || phone.isBlank()) {
                    throw new DomainValidationException(
                        "MISSING_RECIPIENT_PHONE",
                        "Phone must be specified for SMS channel",
                        "recipient.phone"
                    );
                }
            }
            case PUSH -> {
                if (deviceToken == null || deviceToken.isBlank()) {
                    throw new DomainValidationException(
                        "MISSING_DEVICE_TOKEN",
                        "Device token must be specified for PUSH channel",
                        "recipient.deviceToken"
                    );
                }
            }
            case WEBHOOK -> {
                if (webhookUrl == null || webhookUrl.isBlank()) {
                    throw new DomainValidationException(
                        "MISSING_WEBHOOK_URL",
                        "Webhook URL must be specified for WEBHOOK channel",
                        "recipient.webhookUrl"
                    );
                }
            }
        }
    }

    public Map<String, Object> toMap() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (email != null) builder.put("email", email);
        if (phone != null) builder.put("phone", phone);
        if (deviceToken != null) builder.put("deviceToken", deviceToken);
        if (webhookUrl != null) builder.put("webhookUrl", webhookUrl);
        return builder.build();
    }
}