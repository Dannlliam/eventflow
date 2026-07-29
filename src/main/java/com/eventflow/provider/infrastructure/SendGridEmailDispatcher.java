package com.eventflow.provider.infrastructure;

import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * SendGrid adapter for email dispatch.
 * Sends emails via the SendGrid v3 API.
 */
@Component
public class SendGridEmailDispatcher implements ProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailDispatcher.class);
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SendGridEmailDispatcher(ObjectMapper objectMapper) {
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
            String apiKey = additionalHeaders.get("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "SENDGRID", 0,
                    "SendGrid API key not configured"
                );
            }

            String fromEmail = additionalHeaders.getOrDefault("fromEmail", "noreply@eventflow.com");
            String fromName = additionalHeaders.getOrDefault("fromName", "EventFlow");
            String replyTo = additionalHeaders.getOrDefault("replyTo", null);

            // Build SendGrid v3 API request body
            String requestBody = buildSendGridRequest(fromEmail, fromName, replyTo, recipient, subject, body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                SENDGRID_API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email dispatched successfully via SendGrid: notificationId={}, recipient={}",
                    notificationId, recipient);
                return DispatchResultEvent.success(
                    notificationIdObj, workspaceId, "SENDGRID",
                    response.getStatusCodeValue(),
                    "Email accepted by SendGrid"
                );
            } else {
                String errorMsg = "SendGrid returned HTTP " + response.getStatusCodeValue();
                log.warn("Email dispatch failed: notificationId={}, status={}", notificationId, response.getStatusCode());
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "SENDGRID",
                    response.getStatusCodeValue(), errorMsg
                );
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            String errorMsg = e.getResponseBodyAsString();

            // 4xx errors (except 429) are permanent failures
            if (statusCode == 429 || statusCode >= 500) {
                log.warn("Transient failure sending email via SendGrid: notificationId={}, status={}, error={}",
                    notificationId, statusCode, errorMsg);
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "SENDGRID", statusCode, errorMsg
                );
            } else {
                log.error("Permanent failure sending email via SendGrid: notificationId={}, status={}, error={}",
                    notificationId, statusCode, errorMsg);
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "SENDGRID", statusCode, errorMsg
                );
            }
        } catch (Exception e) {
            log.error("Unexpected error sending email via SendGrid: notificationId={}", notificationId, e);
            return DispatchResultEvent.transientFailure(
                notificationIdObj, workspaceId, "SENDGRID", 0, e.getMessage()
            );
        }
    }

    private String buildSendGridRequest(String fromEmail, String fromName, String replyTo,
                                         String toEmail, String subject, String htmlBody) {
        try {
            // Use ObjectMapper for safe JSON construction
            var root = objectMapper.createObjectNode();
            var personalizations = root.putArray("personalizations");
            var personalizationObj = personalizations.addObject();
            var to = personalizationObj.putArray("to");
            to.addObject().put("email", toEmail);
            personalizationObj.put("subject", subject);

            var from = root.putObject("from");
            from.put("email", fromEmail);
            from.put("name", fromName);

            if (replyTo != null && !replyTo.isBlank()) {
                root.putObject("reply_to").put("email", replyTo);
            }

            var content = root.putArray("content");
            content.addObject()
                .put("type", "text/html")
                .put("value", htmlBody);

            root.put("subject", subject);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SendGrid request", e);
        }
    }
}
