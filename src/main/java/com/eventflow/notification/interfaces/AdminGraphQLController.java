package com.eventflow.notification.interfaces;

import com.eventflow.notification.application.DlqReplayUseCase;
import com.eventflow.notification.application.NotificationRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL controller for the Admin Dashboard.
 * Provides query and mutation endpoints for administrative operations
 * such as viewing notifications, managing templates, and replaying DLQ.
 *
 * Maps to the GraphQL schema defined in schema.graphqls.
 * DTOs are structured to match GraphQL type definitions exactly.
 */
@Controller
public class AdminGraphQLController {

    private static final Logger log = LoggerFactory.getLogger(AdminGraphQLController.class);

    private final NotificationRepository notificationRepository;
    private final DlqReplayUseCase dlqReplayUseCase;

    public AdminGraphQLController(NotificationRepository notificationRepository,
                                  DlqReplayUseCase dlqReplayUseCase) {
        this.notificationRepository = notificationRepository;
        this.dlqReplayUseCase = dlqReplayUseCase;
    }

    @QueryMapping
    public NotificationPayload notification(@Argument String id) {
        log.info("Querying notification: id={}", id);
        UUID notificationId = UUID.fromString(id);
        return notificationRepository.findById(notificationId)
            .map(this::toNotificationPayload)
            .orElse(null);
    }

    @QueryMapping
    public NotificationConnectionPayload notifications(@Argument String filter,
                                                 @Argument @Min(1) @Max(100) int first,
                                                 @Argument String after) {
        log.info("Querying notifications: filter={}, first={}, after={}", filter, first, after);

        UUID workspaceId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        int offset = 0;
        if (after != null && !after.isBlank()) {
            try {
                offset = Integer.parseInt(new String(Base64.getDecoder().decode(after)));
            } catch (Exception e) {
                log.warn("Invalid cursor: {}", after);
            }
        }

        String status = null;
        String channel = null;

        if (filter != null && !filter.isBlank()) {
            String[] parts = filter.split(",");
            for (String part : parts) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    switch (keyValue[0].trim().toLowerCase()) {
                        case "status" -> status = keyValue[1].trim().toUpperCase();
                        case "channel" -> channel = keyValue[1].trim().toUpperCase();
                    }
                }
            }
        }

        List<NotificationPayload> items = notificationRepository.findAll(
                workspaceId, status, channel, first, offset)
                .stream()
                .map(this::toNotificationPayload)
                .toList();

        long totalCount = notificationRepository.countByWorkspaceId(workspaceId);

        boolean hasNextPage = items.size() == first;
        String endCursor = items.isEmpty() ? null : Base64.getEncoder()
            .encodeToString(String.valueOf(offset + items.size()).getBytes());

        List<NotificationEdgePayload> edges = items.stream()
            .map(n -> new NotificationEdgePayload(n, n.id()))
            .toList();

        return new NotificationConnectionPayload(
            edges,
            new PageInfoPayload(hasNextPage, endCursor, totalCount),
            totalCount
        );
    }

    @MutationMapping
    public Boolean replayDlqMessage(@Argument String eventId) {
        log.info("Replaying DLQ message: eventId={}", eventId);
        try {
            UUID notificationId = UUID.fromString(eventId);
            UUID adminUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

            DlqReplayUseCase.ReplayResult result = dlqReplayUseCase.execute(notificationId, adminUserId);
            log.info("DLQ replay result: notificationId={}, success={}, message={}",
                notificationId, result.success(), result.message());
            return result.success();
        } catch (Exception e) {
            log.error("Failed to replay DLQ message: eventId={}, error={}", eventId, e.getMessage());
            return false;
        }
    }

    private NotificationPayload toNotificationPayload(
            com.eventflow.notification.domain.Notification notification) {
        return new NotificationPayload(
            notification.getId().toString(),
            notification.getWorkspaceId().toString(),
            notification.getChannel().name(),
            new RecipientInfoPayload(
                notification.getRecipient().email(),
                notification.getRecipient().phone(),
                notification.getRecipient().deviceToken(),
                notification.getRecipient().webhookUrl()
            ),
            notification.getTemplateSlug().orElse(null),
            notification.getPayload(),
            notification.getMetadata(),
            notification.getStatus().name(),
            notification.getAttemptCount(),
            notification.getNextRetryAt().map(Object::toString).orElse(null),
            notification.getSentAt().map(Object::toString).orElse(null),
            notification.getDeliveredAt().map(Object::toString).orElse(null),
            notification.getCreatedAt().toString(),
            notification.getUpdatedAt().toString()
        );
    }

    // === GraphQL DTO records matching schema.graphqls ===

    public record NotificationPayload(
        String id,
        String workspaceId,
        String channel,
        RecipientInfoPayload recipient,
        String templateSlug,
        Object payload,
        Object metadata,
        String status,
        int attemptCount,
        String nextRetryAt,
        String sentAt,
        String deliveredAt,
        String createdAt,
        String updatedAt
    ) {}

    public record RecipientInfoPayload(
        String email,
        String phone,
        String deviceToken,
        String webhookUrl
    ) {}

    public record NotificationEdgePayload(
        NotificationPayload node,
        String cursor
    ) {}

    public record NotificationConnectionPayload(
        List<NotificationEdgePayload> edges,
        PageInfoPayload pageInfo,
        long totalCount
    ) {}

    public record PageInfoPayload(
        boolean hasNextPage,
        String endCursor,
        long totalCount
    ) {}
}