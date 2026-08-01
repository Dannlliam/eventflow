package com.eventflow.identity.domain;

import com.eventflow.common.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the EmailAddress value object.
 */
@DisplayName("EmailAddress value object")
class EmailAddressTest {

    @Test
    @DisplayName("should create valid email address")
    void constructor_ValidEmail_CreatesInstance() {
        String email = "user@example.com";
        EmailAddress emailAddress = new EmailAddress(email);
        assertEquals(email, emailAddress.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test.user@domain.com",
        "test+label@domain.co.uk",
        "a@b.c",
        "user.name+tag@example.com",
        "very.common@example.com"
    })
    @DisplayName("should accept valid email formats")
    void constructor_ValidEmailFormats_CreatesInstance(String email) {
        assertDoesNotThrow(() -> new EmailAddress(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "not-an-email",
        "@domain.com",
        "user@",
        "user@.com",
        "user@domain",
        "user@@domain.com",
        " user@domain.com",
        "user@domain.com "
    })
    @DisplayName("should reject invalid email formats")
    void constructor_InvalidEmailFormats_ThrowsException(String email) {
        assertThrows(DomainValidationException.class, () -> new EmailAddress(email));
    }

    @Test
    @DisplayName("should throw exception for null email")
    void constructor_NullEmail_ThrowsException() {
        assertThrows(DomainValidationException.class, () -> new EmailAddress(null));
    }

    @Test
    @DisplayName("should be equal to another EmailAddress with same value")
    void equals_SameValue_ReturnsTrue() {
        EmailAddress email1 = new EmailAddress("user@example.com");
        EmailAddress email2 = new EmailAddress("user@example.com");
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    @DisplayName("should not be equal to a different EmailAddress")
    void equals_DifferentValue_ReturnsFalse() {
        EmailAddress email1 = new EmailAddress("user@example.com");
        EmailAddress email2 = new EmailAddress("other@example.com");
        assertNotEquals(email1, email2);
    }

    @Test
    @DisplayName("should return email as string representation")
    void toString_ReturnsEmail() {
        String email = "user@example.com";
        EmailAddress emailAddress = new EmailAddress(email);
        assertEquals(email, emailAddress.toString());
    }
}