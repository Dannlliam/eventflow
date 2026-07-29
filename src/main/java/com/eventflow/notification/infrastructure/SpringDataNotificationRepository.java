package com.eventflow.notification.infrastructure;

import com.eventflow.notification.infrastructure.NotificationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the notifications table.
 */
@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.workspaceId = :workspaceId AND n.idempotencyKey = :idempotencyKey")
    Optional<NotificationJpaEntity> findByIdempotencyKey(@Param("workspaceId") UUID workspaceId,
                                                          @Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.status = :status AND n.nextRetryAt IS NOT NULL AND n.nextRetryAt <= :before ORDER BY n.nextRetryAt ASC")
    List<NotificationJpaEntity> findByStatusAndNextRetryAtBefore(
        @Param("status") String status,
        @Param("before") Instant before,
        Pageable pageable
    );

    long countByStatus(String status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.status = :status, n.updatedAt = NOW() WHERE n.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    List<NotificationJpaEntity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    List<NotificationJpaEntity> findByWorkspaceIdAndStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID workspaceId, String status, Instant startDate, Instant endDate, Pageable pageable);

    List<NotificationJpaEntity> findByWorkspaceIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID workspaceId, Instant startDate, Instant endDate, Pageable pageable);

    long countByWorkspaceId(UUID workspaceId);
}