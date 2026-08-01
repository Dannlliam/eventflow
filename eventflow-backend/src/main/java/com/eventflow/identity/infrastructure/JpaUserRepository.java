package com.eventflow.identity.infrastructure;

import com.eventflow.identity.application.UserRepository;
import com.eventflow.identity.domain.EmailAddress;
import com.eventflow.identity.domain.Role;
import com.eventflow.identity.domain.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of the UserRepository port.
 * Maps between domain User aggregates and JPA entities.
 */
@Repository
@Transactional
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepository;

    public JpaUserRepository(SpringDataUserRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toJpaEntity(user);
        UserJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataRepository.findByEmail(email).map(this::toDomainEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    private UserJpaEntity toJpaEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail().value());
        entity.setDisplayName(domain.getDisplayName());
        entity.setEnabled(domain.isEnabled());
        entity.setLastLoginAt(domain.getLastLoginAt().orElse(null));
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    private User toDomainEntity(UserJpaEntity entity) {
        // Note: In production, roles would be loaded from a join table
        // For now, we assign a default role set
        Set<Role> roles = new HashSet<>(Set.of(Role.DEVELOPER));
        return new User(
            entity.getId(),
            new EmailAddress(entity.getEmail()),
            entity.getDisplayName(),
            roles,
            entity.isEnabled(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion(),
            entity.getLastLoginAt()
        );
    }
}