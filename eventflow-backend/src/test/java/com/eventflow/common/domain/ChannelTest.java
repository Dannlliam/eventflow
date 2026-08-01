package com.eventflow.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Channel enum.
 */
@DisplayName("Channel enum")
class ChannelTest {

    @Test
    @DisplayName("should return correct enum for valid channel names")
    void fromString_ValidNames_ReturnsCorrectEnum() {
        assertEquals(Channel.EMAIL, Channel.fromString("EMAIL"));
        assertEquals(Channel.SMS, Channel.fromString("SMS"));
        assertEquals(Channel.PUSH, Channel.fromString("PUSH"));
        assertEquals(Channel.WEBHOOK, Channel.fromString("WEBHOOK"));
    }

    @Test
    @DisplayName("should be case-insensitive when parsing channel names")
    void fromString_CaseInsensitive_ReturnsCorrectEnum() {
        assertEquals(Channel.EMAIL, Channel.fromString("email"));
        assertEquals(Channel.SMS, Channel.fromString("sms"));
        assertEquals(Channel.PUSH, Channel.fromString("push"));
        assertEquals(Channel.WEBHOOK, Channel.fromString("webhook"));
    }

    @Test
    @DisplayName("should throw exception for invalid channel names")
    void fromString_InvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Channel.fromString("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> Channel.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Channel.fromString(null));
    }

    @Test
    @DisplayName("should return all channel values")
    void values_ReturnsAllChannels() {
        Channel[] channels = Channel.values();
        assertEquals(4, channels.length);
        assertArrayEquals(new Channel[]{Channel.EMAIL, Channel.SMS, Channel.PUSH, Channel.WEBHOOK}, channels);
    }
}