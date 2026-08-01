package com.eventflow.provider.domain;

import com.eventflow.common.domain.Channel;

/**
 * Types of providers supported by EventFlow.
 * Each provider type maps to a specific notification channel.
 */
public enum ProviderType {
    SENDGRID(Channel.EMAIL),
    AMAZON_SES(Channel.EMAIL),
    MAILGUN(Channel.EMAIL),
    TWILIO(Channel.SMS),
    AMAZON_SNS(Channel.SMS),
    PLIVO(Channel.SMS),
    FCM(Channel.PUSH),
    APNS(Channel.PUSH),
    WEBHOOK(Channel.WEBHOOK);

    private final Channel supportedChannel;

    ProviderType(Channel supportedChannel) {
        this.supportedChannel = supportedChannel;
    }

    public Channel supportedChannel() {
        return supportedChannel;
    }

    public static ProviderType fromString(String value) {
        for (ProviderType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown provider type: " + value);
    }
}
