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
        if (value == null) {
            throw new IllegalArgumentException("Channel type must not be null");
        }
        for (Channel channel : values()) {
            if (channel.name().equalsIgnoreCase(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException(
            "Invalid channel type: '" + value + "'. Must be one of: EMAIL, SMS, PUSH, WEBHOOK"
        );
    }
}