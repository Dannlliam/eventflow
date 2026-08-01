package com.eventflow.identity.application;

import com.eventflow.identity.domain.User;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for User persistence operations.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}