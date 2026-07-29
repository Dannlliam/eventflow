package com.eventflow.identity.domain;

import com.eventflow.common.domain.DomainValidationException;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing an email address.
 * Validates format upon construction.
 */
public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public EmailAddress {
        Objects.requireNonNull(value, "Email must not be null");
        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new DomainValidationException(
                "INVALID_EMAIL",
                "Invalid email address format: '" + value + "'",
                "email"
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}