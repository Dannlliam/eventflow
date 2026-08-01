package com.eventflow.provider.infrastructure;

import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Firebase Cloud Messaging (FCM) adapter for push notification dispatch.
 * Sends push notifications via the FCM HTTP v1 API.
 */
@Component
public class FcmPushDispatcher implements ProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(FcmPushDispatcher.class);
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FcmPushDispatcher(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public DispatchResultEvent dispatch(String recipient, String subject, String body,
                                         String notificationId, Map<String, String> additionalHeaders) {
        UUID notificationIdObj = UUID.fromString(notificationId);
        UUID workspaceId = additionalHeaders.containsKey("workspaceId")
            ? UUID.fromString(additionalHeaders.get("workspaceId"))
            : UUID.randomUUID();

        try {
            String projectId = additionalHeaders.get("projectId");
            String accessToken = additionalHeaders.get("accessToken");

            if (projectId == null || accessToken == null) {
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "FCM", 0,
                    "FCM credentials not configured (projectId and accessToken required)"
                );
            }

            String fcmUrl = String.format(FCM_API_URL, projectId);

            // Build FCM v1 API request body
            String requestBody = buildFcmRequest(recipient, subject, body, additionalHeaders);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                fcmUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Push notification dispatched successfully via FCM: notificationId={}, recipient={}",
                    notificationId, recipient);
                return DispatchResultEvent.success(
                    notificationIdObj, workspaceId, "FCM",
                    response.getStatusCodeValue(),
                    "Push notification accepted by FCM"
                );
            } else {
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "FCM",
                    response.getStatusCodeValue(),
                    "FCM returned HTTP " + response.getStatusCodeValue()
                );
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            String errorMsg = e.getResponseBodyAsString();

            if (statusCode == 429 || statusCode >= 500) {
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "FCM", statusCode, errorMsg
                );
            } else {
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "FCM", statusCode, errorMsg
                );
            }
        } catch (Exception e) {
            log.error("Unexpected error sending push via FCM: notificationId={}", notificationId, e);
            return DispatchResultEvent.transientFailure(
                notificationIdObj, workspaceId, "FCM", 0, e.getMessage()
            );
        }
    }

    private String buildFcmRequest(String deviceToken, String title, String body,
                                    Map<String, String> additionalHeaders) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode message = root.putObject("message");
            message.put("token", deviceToken);

            ObjectNode notification = message.putObject("notification");
            notification.put("title", title != null ? title : "");
            notification.put("body", body);

            // Add data payload if present
            String dataPayload = additionalHeaders.get("dataPayload");
            if (dataPayload != null && !dataPayload.isBlank()) {
                message.set("data", objectMapper.readTree(dataPayload));
            }

            // Add Android configuration
            ObjectNode android = message.putObject("android");
            android.put("priority", "high");

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build FCM request", e);
        }
    }
}
