package com.eventflow.provider.infrastructure;

import com.eventflow.common.infrastructure.SsrfProtectionService;
import com.eventflow.common.infrastructure.WebhookSigningService;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook adapter for HTTP-based notification dispatch.
 * Uses SsrfProtectionService for URL validation and WebhookSigningService
 * for HMAC signing instead of building its own.
 */
@Component
public class WebhookDispatcher implements ProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SsrfProtectionService ssrfProtectionService;
    private final WebhookSigningService webhookSigningService;

    public WebhookDispatcher(ObjectMapper objectMapper,
                              SsrfProtectionService ssrfProtectionService,
                              WebhookSigningService webhookSigningService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.ssrfProtectionService = ssrfProtectionService;
        this.webhookSigningService = webhookSigningService;
    }

    @Override
    public DispatchResultEvent dispatch(String recipient, String subject, String body,
                                         String notificationId, Map<String, String> additionalHeaders) {
        UUID notificationIdObj = UUID.fromString(notificationId);
        UUID workspaceId = additionalHeaders.containsKey("workspaceId")
            ? UUID.fromString(additionalHeaders.get("workspaceId"))
            : UUID.randomUUID();

        try {
            String webhookUrl = recipient; // The webhook URL is the recipient
            String hmacSecret = additionalHeaders.get("hmacSecret");

            // SSRF protection: validate URL before dispatching
            try {
                ssrfProtectionService.validateUrl(webhookUrl);
            } catch (IllegalArgumentException e) {
                log.warn("SSRF validation failed for webhook URL: {}", e.getMessage());
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "WEBHOOK", 0,
                    "SSRF validation failed: " + e.getMessage()
                );
            }

            // Build JSON payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("notificationId", notificationId);
            payload.put("subject", subject);
            payload.put("body", body);
            payload.put("timestamp", System.currentTimeMillis());

            // Add custom headers
            ObjectNode headers = payload.putObject("headers");
            if (additionalHeaders != null) {
                additionalHeaders.forEach((key, value) -> {
                    if (!"hmacSecret".equals(key) && !"workspaceId".equals(key)) {
                        headers.put(key, value);
                    }
                });
            }

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("X-EventFlow-NotificationId", notificationId);
            httpHeaders.set("X-EventFlow-Timestamp", String.valueOf(System.currentTimeMillis()));

            // Use WebhookSigningService instead of manual HMAC computation
            if (hmacSecret != null && !hmacSecret.isBlank()) {
                WebhookSigningService.SigningResult signingResult =
                    webhookSigningService.sign(requestBody.getBytes(StandardCharsets.UTF_8), hmacSecret);
                httpHeaders.set(signingResult.signatureHeader(),
                    "sha256=" + signingResult.signature());
                httpHeaders.set(signingResult.timestampHeader(),
                    String.valueOf(signingResult.timestamp()));
            }

            HttpEntity<String> entity = new HttpEntity<>(requestBody, httpHeaders);

            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, entity, String.class);

            log.info("Webhook dispatched: notificationId={}, url={}, status={}",
                notificationId, webhookUrl, response.getStatusCodeValue());

            if (response.getStatusCode().is2xxSuccessful()) {
                return DispatchResultEvent.success(
                    notificationIdObj, workspaceId, "WEBHOOK",
                    response.getStatusCodeValue(),
                    "Webhook delivered successfully"
                );
            } else if (response.getStatusCode().is4xxClientError()) {
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "WEBHOOK",
                    response.getStatusCodeValue(),
                    "Webhook rejected: HTTP " + response.getStatusCodeValue()
                );
            } else {
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "WEBHOOK",
                    response.getStatusCodeValue(),
                    "Webhook server error: HTTP " + response.getStatusCodeValue()
                );
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Webhook connection timeout: notificationId={}, error={}", notificationId, e.getMessage());
            return DispatchResultEvent.transientFailure(
                notificationIdObj, workspaceId, "WEBHOOK", 0,
                "Connection timeout: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error dispatching webhook: notificationId={}", notificationId, e);
            return DispatchResultEvent.transientFailure(
                notificationIdObj, workspaceId, "WEBHOOK", 0, e.getMessage()
            );
        }
    }
}