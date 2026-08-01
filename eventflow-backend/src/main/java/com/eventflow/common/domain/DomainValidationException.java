package com.eventflow.common.domain;

/**
 * Base exception for domain validation failures.
 * Encapsulates business rule violations.
 */
public class DomainValidationException extends RuntimeException {

    private final String code;
    private final String field;

    public DomainValidationException(String code, String message) {
        this(code, message, null);
    }

    public DomainValidationException(String code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}