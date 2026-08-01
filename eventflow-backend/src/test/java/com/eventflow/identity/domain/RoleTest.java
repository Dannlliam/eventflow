package com.eventflow.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Role enum.
 */
@DisplayName("Role enum")
class RoleTest {

    @Test
    @DisplayName("should return correct enum for valid role names")
    void fromString_ValidNames_ReturnsCorrectEnum() {
        assertEquals(Role.WORKSPACE_ADMIN, Role.fromString("WORKSPACE_ADMIN"));
        assertEquals(Role.DEVELOPER, Role.fromString("DEVELOPER"));
        assertEquals(Role.ANALYST, Role.fromString("ANALYST"));
    }

    @Test
    @DisplayName("should throw exception for invalid role names")
    void fromString_InvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString(""));
    }

    @Test
    @DisplayName("should return all role values")
    void values_ReturnsAllRoles() {
        Role[] roles = Role.values();
        assertTrue(roles.length >= 3);
    }
}