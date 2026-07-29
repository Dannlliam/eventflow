package com.eventflow.provider.infrastructure;

import com.eventflow.notification.domain.events.DispatchResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Twilio adapter for SMS dispatch.
 * Sends SMS messages via the Twilio REST API.
 */
@Component
public class TwilioSmsDispatcher implements ProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsDispatcher.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TwilioSmsDispatcher(ObjectMapper objectMapper) {
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
            String accountSid = additionalHeaders.get("accountSid");
            String authToken = additionalHeaders.get("authToken");
            String fromNumber = additionalHeaders.get("fromNumber");

            if (accountSid == null || authToken == null || fromNumber == null) {
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "TWILIO", 0,
                    "Twilio credentials not configured (accountSid, authToken, fromNumber required)"
                );
            }

            String twilioApiUrl = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid
            );

            String requestBody = UriComponentsBuilder.newInstance()
                .queryParam("To", recipient)
                .queryParam("From", fromNumber)
                .queryParam("Body", body)
                .build().encode().toUriString()
                .substring(1); // Remove leading '?'

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                twilioApiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS dispatched successfully via Twilio: notificationId={}, recipient={}",
                    notificationId, recipient);
                return DispatchResultEvent.success(
                    notificationIdObj, workspaceId, "TWILIO",
                    response.getStatusCodeValue(),
                    "SMS accepted by Twilio"
                );
            } else {
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "TWILIO",
                    response.getStatusCodeValue(),
                    "Twilio returned HTTP " + response.getStatusCodeValue()
                );
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            String errorMsg = e.getResponseBodyAsString();

            if (statusCode == 429 || statusCode >= 500) {
                log.warn("Transient failure sending SMS via Twilio: notificationId={}, status={}",
                    notificationId, statusCode);
                return DispatchResultEvent.transientFailure(
                    notificationIdObj, workspaceId, "TWILIO", statusCode, errorMsg
                );
            } else {
                log.error("Permanent failure sending SMS via Twilio: notificationId={}, status={}",
                    notificationId, statusCode);
                return DispatchResultEvent.permanentFailure(
                    notificationIdObj, workspaceId, "TWILIO", statusCode, errorMsg
                );
            }
        } catch (Exception e) {
            log.error("Unexpected error sending SMS via Twilio: notificationId={}", notificationId, e);
            return DispatchResultEvent.transientFailure(
                notificationIdObj, workspaceId, "TWILIO", 0, e.getMessage()
            );
        }
    }
}
