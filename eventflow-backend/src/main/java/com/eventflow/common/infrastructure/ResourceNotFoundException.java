package com.eventflow.common.infrastructure;

/**
 * Exception thrown when a requested resource is not found.
 * Returns HTTP 404 to the client to prevent data enumeration attacks.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier);
    }
}