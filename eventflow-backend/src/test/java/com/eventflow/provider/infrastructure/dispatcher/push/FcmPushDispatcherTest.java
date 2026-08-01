package com.eventflow.provider.infrastructure.dispatcher.push;

import com.eventflow.provider.domain.model.DispatchResult;
import com.eventflow.provider.domain.model.Provider;
import com.eventflow.provider.domain.model.ProviderCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushDispatcherTest {

    @Mock
    private FcmClient fcmClient;

    private FcmPushDispatcher dispatcher;
    private Provider provider;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        dispatcher = new FcmPushDispatcher(fcmClient);
        notificationId = UUID.randomUUID();
        
        provider = Provider.builder()
                .id(UUID.randomUUID())
                .type(Provider.ProviderType.FCM)
                .channel(Provider.Channel.PUSH)
                .credentials(ProviderCredentials.builder()
                        .serverKey("fcm-server-key")
                        .build())
                .status(Provider.Status.ACTIVE)
                .build();
    }

    @Test
    void dispatch_shouldSendPushSuccessfully() {
        Map<String, Object> content = Map.of(
                "title", "New Message",
                "body", "You have a new message"
        );

        when(fcmClient.sendPush(any(), any(), any(), any()))
                .thenReturn("fcm_msg_123");

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "device_token_xyz",
                content
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExternalMessageId()).isEqualTo("fcm_msg_123");
        verify(fcmClient).sendPush(any(), eq("device_token_xyz"), eq("New Message"), eq("You have a new message"));
    }

    @Test
    void dispatch_shouldHandleFailure() {
        Map<String, Object> content = Map.of("title", "Test", "body", "Body");

        when(fcmClient.sendPush(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("FCM API error"));

        DispatchResult result = dispatcher.dispatch(
                notificationId,
                provider,
                "device_token_xyz",
                content
        );

        assertThat(result.isSuccess()).isFalse();
    }
}
