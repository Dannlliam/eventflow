package com.eventflow.identity.domain;

/**
 * Roles available for Role-Based Access Control (RBAC).
 */
public enum Role {
    WORKSPACE_ADMIN,
    DEVELOPER,
    ANALYST;

    public static Role fromString(String value) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}