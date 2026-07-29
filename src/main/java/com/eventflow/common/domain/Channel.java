package com.eventflow.common.domain;

/**
 * Value object representing a channel type for notifications.
 */
public enum Channel {
    EMAIL,
    SMS,
    PUSH,
    WEBHOOK;

    public static Channel fromString(String value) {
        for (Channel channel : values()) {
            if (channel.name().equalsIgnoreCase(value)) {
                return channel;
            }
        }
        throw new DomainValidationException(
            "INVALID_CHANNEL",
            "Invalid channel type: '" + value + "'. Must be one of: EMAIL, SMS, PUSH, WEBHOOK"
        );
    }
}