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
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{1,}$");

    public EmailAddress {
        if (value == null) {
            throw new DomainValidationException(
                "INVALID_EMAIL",
                "Email must not be null",
                "email"
            );
        }
        String lower = value.toLowerCase();
        if (lower.isEmpty() || !EMAIL_PATTERN.matcher(lower).matches()) {
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