package com.eventflow.identity.domain;

import com.eventflow.common.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

/**
 * Unit tests for the User aggregate root.
 */
@DisplayName("User aggregate root")
class UserTest {

    private final EmailAddress email = new EmailAddress("user@example.com");

    @Test
    @DisplayName("should create user with valid email and display name")
    void constructor_ValidInputs_CreatesInstance() {
        User user = new User(email, "John Doe", Set.of(Role.WORKSPACE_ADMIN));
        assertNotNull(user.getId());
        assertEquals("user@example.com", user.getEmail().value());
        assertEquals("John Doe", user.getDisplayName());
        assertTrue(user.isEnabled());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("should throw exception when roles set is empty")
    void constructor_EmptyRoles_ThrowsException() {
        assertThrows(DomainValidationException.class, () -> new User(email, "John Doe", Set.of()));
    }

    @Test
    @DisplayName("should disable user account")
    void disable_SetsEnabledToFalse() {
        User user = new User(email, "John Doe", Set.of(Role.WORKSPACE_ADMIN));
        assertTrue(user.isEnabled());

        user.disable();
        assertFalse(user.isEnabled());
    }

    @Test
    @DisplayName("should enable user account")
    void enable_SetsEnabledToTrue() {
        User user = new User(email, "John Doe", Set.of(Role.WORKSPACE_ADMIN));
        user.disable();
        assertFalse(user.isEnabled());

        user.enable();
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("should update last login timestamp")
    void recordLogin_UpdatesTimestamp() {
        User user = new User(email, "John Doe", Set.of(Role.WORKSPACE_ADMIN));
        assertTrue(user.getLastLoginAt().isEmpty());

        user.recordLogin();
        assertTrue(user.getLastLoginAt().isPresent());
    }

    @Test
    @DisplayName("should throw exception when email is null")
    void constructor_NullEmail_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new User(null, "John Doe", Set.of(Role.WORKSPACE_ADMIN)));
    }

    @Test
    @DisplayName("should throw exception when display name is null")
    void constructor_NullDisplayName_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new User(email, null, Set.of(Role.WORKSPACE_ADMIN)));
    }

    @Test
    @DisplayName("should throw exception when display name is empty")
    void constructor_EmptyDisplayName_ThrowsException() {
        assertThrows(DomainValidationException.class, () -> new User(email, "", Set.of(Role.WORKSPACE_ADMIN)));
    }

    @Test
    @DisplayName("should check role membership")
    void hasRole_ReturnsCorrectRole() {
        User user = new User(email, "John Doe", Set.of(Role.WORKSPACE_ADMIN, Role.DEVELOPER));
        assertTrue(user.hasRole(Role.WORKSPACE_ADMIN));
        assertTrue(user.hasRole(Role.DEVELOPER));
        assertFalse(user.hasRole(Role.ANALYST));
    }
}