package com.eventflow.notification.interfaces;

import com.eventflow.notification.application.IngestNotificationUseCase;
import com.eventflow.notification.application.IngestNotificationUseCase.IngestCommand;
import com.eventflow.notification.application.IngestNotificationUseCase.IngestResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the notification ingestion API.
 * POST /api/v1/notifications - Ingest a new notification request.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationIngestionController {

    private static final Logger log = LoggerFactory.getLogger(NotificationIngestionController.class);

    private final IngestNotificationUseCase ingestNotificationUseCase;

    public NotificationIngestionController(IngestNotificationUseCase ingestNotificationUseCase) {
        this.ingestNotificationUseCase = ingestNotificationUseCase;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> ingestNotification(
            @Valid @RequestBody NotificationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {

        String workspaceIdStr = (String) servletRequest.getAttribute("workspaceId");
        UUID workspaceId = workspaceIdStr != null ? UUID.fromString(workspaceIdStr) : UUID.randomUUID();

        log.info("Ingesting notification: channel={}, template={}, workspaceId={}",
            request.channel(), request.templateSlug(), workspaceId);

        IngestCommand command = new IngestCommand(
            workspaceId,
            request.channel(),
            request.recipient() != null ? request.recipient().get("email") : null,
            request.recipient() != null ? request.recipient().get("phone") : null,
            request.recipient() != null ? request.recipient().get("deviceToken") : null,
            request.recipient() != null ? request.recipient().get("webhookUrl") : null,
            request.templateSlug(),
            request.payload(),
            request.metadata(),
            idempotencyKey
        );

        IngestResult result = ingestNotificationUseCase.execute(command);

        NotificationResponse response = new NotificationResponse(
            result.eventId().toString(),
            result.status(),
            result.deduplicated(),
            result.deduplicated() ? "Idempotent request - original eventId returned" : "Notification queued for processing"
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // Request/Response DTOs
    public record NotificationRequest(
        @NotBlank String channel,
        Map<String, String> recipient,
        String templateSlug,
        Map<String, String> payload,
        Map<String, String> metadata
    ) {}

    public record NotificationResponse(
        String eventId,
        String status,
        boolean deduplicated,
        String message
    ) {}
}