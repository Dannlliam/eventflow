package com.eventflow.common.infrastructure;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Phone number normalization service using Google's libphonenumber.
 * Normalizes phone numbers into E.164 format before dispatch.
 * Invalid numbers are flagged as PERMANENT_FAILURE without hitting the provider API.
 *
 * As specified in the PRD Section 51 - SMS Provider / Phone Number Normalization.
 */
@Service
public class PhoneNumberNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneNumberNormalizationService.class);

    private static final String DEFAULT_REGION = "US";
    private static final int MAX_SMS_LENGTH = 1600;

    private final PhoneNumberUtil phoneNumberUtil;

    public PhoneNumberNormalizationService() {
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
    }

    /**
     * Normalizes a phone number to E.164 format.
     * Attempts to parse the number with the default region first,
     * then tries to parse without a region if that fails.
     *
     * @param phoneNumber the raw phone number string
     * @return Optional containing the E.164 formatted number, or empty if invalid
     */
    public Optional<String> normalizeToE164(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.empty();
        }

        try {
            // Try parsing with default region
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, DEFAULT_REGION);

            if (!phoneNumberUtil.isValidNumber(number)) {
                // Try parsing without region (number may include country code)
                number = phoneNumberUtil.parse(phoneNumber, null);
                if (!phoneNumberUtil.isValidNumber(number)) {
                    log.warn("Invalid phone number: {}", phoneNumber);
                    return Optional.empty();
                }
            }

            String e164 = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            log.debug("Normalized phone number: {} -> {}", phoneNumber, e164);
            return Optional.of(e164);
        } catch (NumberParseException e) {
            log.warn("Failed to parse phone number '{}': {}", phoneNumber, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validates a phone number without normalizing.
     *
     * @param phoneNumber the phone number to validate
     * @return true if the number is valid
     */
    public boolean isValidNumber(String phoneNumber) {
        return normalizeToE164(phoneNumber).isPresent();
    }

    /**
     * Truncates an SMS message to the maximum allowed length (1600 characters).
     *
     * @param message the SMS message body
     * @return truncated message if needed, original otherwise
     */
    public String truncateSmsMessage(String message) {
        if (message != null && message.length() > MAX_SMS_LENGTH) {
            String truncated = message.substring(0, MAX_SMS_LENGTH);
            log.warn("SMS message truncated from {} to {} characters", message.length(), MAX_SMS_LENGTH);
            return truncated;
        }
        return message;
    }

    /**
     * Extracts the country code from a normalized phone number.
     *
     * @param e164Number the E.164 formatted phone number
     * @return Optional containing the country code, or empty if invalid
     */
    public Optional<Integer> extractCountryCode(String e164Number) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(e164Number, null);
            return Optional.of(number.getCountryCode());
        } catch (NumberParseException e) {
            log.warn("Failed to extract country code from '{}': {}", e164Number, e.getMessage());
            return Optional.empty();
        }
    }
}