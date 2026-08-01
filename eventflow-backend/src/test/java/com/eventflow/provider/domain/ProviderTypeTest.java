package com.eventflow.provider.domain;

import com.eventflow.common.domain.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ProviderType enum.
 */
@DisplayName("ProviderType enum")
class ProviderTypeTest {

    @Test
    @DisplayName("should return correct enum for valid provider type names")
    void fromString_ValidNames_ReturnsCorrectEnum() {
        assertEquals(ProviderType.SENDGRID, ProviderType.fromString("SENDGRID"));
        assertEquals(ProviderType.TWILIO, ProviderType.fromString("TWILIO"));
        assertEquals(ProviderType.FCM, ProviderType.fromString("FCM"));
        assertEquals(ProviderType.WEBHOOK, ProviderType.fromString("WEBHOOK"));
    }

    @Test
    @DisplayName("should be case-insensitive when parsing provider type names")
    void fromString_CaseInsensitive_ReturnsCorrectEnum() {
        assertEquals(ProviderType.SENDGRID, ProviderType.fromString("sendgrid"));
        assertEquals(ProviderType.TWILIO, ProviderType.fromString("twilio"));
    }

    @Test
    @DisplayName("should throw exception for invalid provider type names")
    void fromString_InvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ProviderType.fromString("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> ProviderType.fromString(""));
    }

    @Test
    @DisplayName("should return supported channels for each provider type")
    void supportedChannel_ReturnsCorrectChannel() {
        assertEquals(Channel.EMAIL, ProviderType.SENDGRID.supportedChannel());
        assertEquals(Channel.SMS, ProviderType.TWILIO.supportedChannel());
        assertEquals(Channel.PUSH, ProviderType.FCM.supportedChannel());
        assertEquals(Channel.WEBHOOK, ProviderType.WEBHOOK.supportedChannel());
    }
}