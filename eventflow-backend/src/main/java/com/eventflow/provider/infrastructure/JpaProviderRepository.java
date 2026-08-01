package com.eventflow.provider.infrastructure;

import com.eventflow.common.domain.Channel;
import com.eventflow.provider.application.ProviderRepository;
import com.eventflow.provider.domain.Provider;
import com.eventflow.provider.domain.ProviderType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of the ProviderRepository port.
 */
@Repository
@Transactional
public class JpaProviderRepository implements ProviderRepository {

    private final SpringDataProviderRepository springDataRepository;
    private final ObjectMapper objectMapper;

    public JpaProviderRepository(SpringDataProviderRepository springDataRepository,
                                  ObjectMapper objectMapper) {
        this.springDataRepository = springDataRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Provider save(Provider provider) {
        ProviderJpaEntity entity = toJpaEntity(provider);
        ProviderJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Provider> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<Provider> findByIdAndWorkspaceId(UUID id, UUID workspaceId) {
        return springDataRepository.findByIdAndWorkspaceId(id, workspaceId).map(this::toDomainEntity);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public List<Provider> findByWorkspaceIdAndChannel(UUID workspaceId, Channel channel) {
        return springDataRepository.findByWorkspaceIdAndChannel(workspaceId, channel.name()).stream()
            .map(this::toDomainEntity)
            .toList();
    }

    @Override
    public List<Provider> findByWorkspaceId(UUID workspaceId) {
        return springDataRepository.findByWorkspaceId(workspaceId).stream()
            .map(this::toDomainEntity)
            .toList();
    }

    @Override
    public Optional<Provider> findPrimaryByWorkspaceIdAndChannel(UUID workspaceId, Channel channel) {
        return springDataRepository.findPrimaryByWorkspaceIdAndChannel(workspaceId, channel.name())
            .map(this::toDomainEntity);
    }

    @Override
    public Optional<Provider> findFirstByWorkspaceIdAndChannelAndEnabled(UUID workspaceId, Channel channel) {
        return springDataRepository.findFirstEnabledByWorkspaceIdAndChannel(workspaceId, channel.name())
            .map(this::toDomainEntity);
    }

    private ProviderJpaEntity toJpaEntity(Provider domain) {
        ProviderJpaEntity entity = new ProviderJpaEntity();
        entity.setId(domain.getId());
        entity.setWorkspaceId(domain.getWorkspaceId());
        entity.setName(domain.getName());
        entity.setProviderType(domain.getProviderType().name());
        entity.setChannel(domain.getChannel().name());
        entity.setPrimary(domain.isPrimary());
        entity.setEnabled(domain.isEnabled());
        entity.setRateLimit(domain.getRateLimit());
        entity.setRateLimitDurationSeconds(domain.getRateLimitDurationSeconds());
        try {
            entity.setCredentials(objectMapper.writeValueAsString(domain.getCredentials()));
            entity.setSettings(objectMapper.writeValueAsString(domain.getSettings()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize provider fields", e);
        }
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Provider toDomainEntity(ProviderJpaEntity entity) {
        Map<String, String> credentials = parseJsonMap(entity.getCredentials());
        Map<String, String> settings = parseJsonMap(entity.getSettings());

        return new Provider(
            entity.getId(),
            entity.getWorkspaceId(),
            entity.getName(),
            ProviderType.fromString(entity.getProviderType()),
            Channel.fromString(entity.getChannel()),
            entity.isPrimary(),
            entity.isEnabled(),
            entity.getRateLimit(),
            entity.getRateLimitDurationSeconds(),
            credentials,
            settings,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}