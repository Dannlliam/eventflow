package com.eventflow.common.infrastructure;

import com.eventflow.common.domain.DomainValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API errors.
 * Provides consistent, machine-readable error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<ErrorField> errors = fieldErrors.stream()
            .map(fe -> new ErrorField(fe.getField(), fe.getDefaultMessage()))
            .toList();

        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            400,
            "VALIDATION_FAILED",
            "The request payload failed validation.",
            errors,
            MDC.get("traceId")
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidation(DomainValidationException ex) {
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            400,
            ex.getCode(),
            ex.getMessage(),
            ex.getField() != null ? List.of(new ErrorField(ex.getField(), ex.getMessage())) : List.of(),
            MDC.get("traceId")
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            403,
            "FORBIDDEN",
            "Access denied",
            List.of(),
            MDC.get("traceId")
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            404,
            "NOT_FOUND",
            ex.getMessage(),
            List.of(),
            MDC.get("traceId")
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);

        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            500,
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            List.of(),
            MDC.get("traceId")
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    public record ErrorResponse(
        String timestamp,
        int status,
        String code,
        String message,
        List<ErrorField> fieldErrors,
        String traceId
    ) {}

    public record ErrorField(String field, String message) {}
}