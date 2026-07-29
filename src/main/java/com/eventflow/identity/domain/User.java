package com.eventflow.identity.domain;

import com.eventflow.common.domain.DomainValidationException;
import com.eventflow.common.domain.BaseEntity;
import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root for User entity.
 * Manages identity, roles, and account state.
 */
public class User extends BaseEntity {

    private final EmailAddress email;
    private final String displayName;
    private final Set<Role> roles;
    private boolean enabled;
    private Instant lastLoginAt;

    public User(EmailAddress email, String displayName, Set<Role> roles) {
        super();
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.roles = Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(roles, "roles must not be null")));
        this.enabled = true;
        if (roles.isEmpty()) {
            throw new DomainValidationException(
                "USER_NO_ROLES",
                "User must have at least one role assigned"
            );
        }
    }

    public User(UUID id, EmailAddress email, String displayName, Set<Role> roles,
                boolean enabled, Instant createdAt, Instant updatedAt, long version, Instant lastLoginAt) {
        super(id, createdAt, updatedAt, version);
        this.email = email;
        this.displayName = displayName;
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
        this.enabled = enabled;
        this.lastLoginAt = lastLoginAt;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<Instant> getLastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
    }

    public void disable() {
        this.enabled = false;
        markUpdated();
    }

    public void enable() {
        this.enabled = true;
        markUpdated();
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        markUpdated();
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(Role... roles) {
        for (Role role : roles) {
            if (this.roles.contains(role)) return true;
        }
        return false;
    }
}