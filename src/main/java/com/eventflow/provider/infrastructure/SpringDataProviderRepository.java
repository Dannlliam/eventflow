package com.eventflow.provider.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the providers table.
 */
@Repository
public interface SpringDataProviderRepository extends JpaRepository<ProviderJpaEntity, UUID> {
    List<ProviderJpaEntity> findByWorkspaceIdAndChannel(UUID workspaceId, String channel);
    List<ProviderJpaEntity> findByWorkspaceId(UUID workspaceId);
    Optional<ProviderJpaEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("SELECT p FROM ProviderJpaEntity p WHERE p.workspaceId = :workspaceId AND p.channel = :channel AND p.isPrimary = true")
    Optional<ProviderJpaEntity> findPrimaryByWorkspaceIdAndChannel(@Param("workspaceId") UUID workspaceId,
                                                                   @Param("channel") String channel);

    @Query("SELECT p FROM ProviderJpaEntity p WHERE p.workspaceId = :workspaceId AND p.channel = :channel AND p.enabled = true ORDER BY p.isPrimary DESC")
    Optional<ProviderJpaEntity> findFirstEnabledByWorkspaceIdAndChannel(@Param("workspaceId") UUID workspaceId,
                                                                        @Param("channel") String channel);
}