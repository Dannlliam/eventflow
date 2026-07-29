package com.eventflow.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DomainValidationException.
 */
@DisplayName("DomainValidationException")
class DomainValidationExceptionTest {

    @Test
    @DisplayName("should create exception with code and message")
    void constructor_WithCodeAndMessage_SetsFields() {
        String code = "INVALID_EMAIL";
        String message = "Email address is not valid";

        DomainValidationException exception = new DomainValidationException(code, message);

        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void type_IsRuntimeException() {
        DomainValidationException exception = new DomainValidationException("CODE", "msg");
        assertInstanceOf(RuntimeException.class, exception);
    }
}