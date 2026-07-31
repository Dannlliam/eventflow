package com.eventflow.common.infrastructure.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    void handleIllegalArgumentException_shouldReturn400() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void handleResourceNotFoundException_shouldReturn404() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("Template not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Template not found");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    void handleUnauthorizedException_shouldReturn401() {
        // Arrange
        UnauthorizedException exception = new UnauthorizedException("Invalid API key");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid API key");
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    void handleForbiddenException_shouldReturn403() {
        // Arrange
        ForbiddenException exception = new ForbiddenException("Access denied");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleForbiddenException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    void handleConflictException_shouldReturn409() {
        // Arrange
        ConflictException exception = new ConflictException("Resource already exists");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflictException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Resource already exists");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void handleRateLimitException_shouldReturn429() {
        // Arrange
        RateLimitExceededException exception = new RateLimitExceededException("Rate limit exceeded");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRateLimitException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Rate limit exceeded");
        assertThat(response.getBody().getStatus()).isEqualTo(429);
    }

    @Test
    void handleGenericException_shouldReturn500() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Internal server error");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }

    @Test
    void errorResponse_shouldIncludeTimestamp() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Test error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(
                exception, webRequest);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void errorResponse_shouldIncludePath() {
        // Arrange
        when(webRequest.getDescription(false)).thenReturn("uri=/api/notifications");
        IllegalArgumentException exception = new IllegalArgumentException("Test error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(
                exception, webRequest);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/notifications");
    }

    @Test
    void handleValidationException_shouldReturn400_withFieldErrors() {
        // Arrange
        ValidationException exception = new ValidationException("Validation failed");
        exception.addFieldError("email", "Invalid email format");
        exception.addFieldError("name", "Name is required");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getFieldErrors()).hasSize(2);
        assertThat(response.getBody().getFieldErrors()).containsKey("email");
        assertThat(response.getBody().getFieldErrors()).containsKey("name");
    }

    @Test
    void handleNullPointerException_shouldReturn500() {
        // Arrange
        NullPointerException exception = new NullPointerException("Null value encountered");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void errorResponse_shouldNotExposeInternalDetails_inProduction() {
        // Arrange
        RuntimeException exception = new RuntimeException("Database connection failed at server-123");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(
                exception, webRequest);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("server-123");
        assertThat(response.getBody().getMessage()).contains("Internal server error");
    }

    @Test
    void handleBusinessException_shouldReturn422() {
        // Arrange
        BusinessException exception = new BusinessException("Business rule violation");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Business rule violation");
        assertThat(response.getBody().getStatus()).isEqualTo(422);
    }

    @Test
    void handleExternalServiceException_shouldReturn502() {
        // Arrange
        ExternalServiceException exception = new ExternalServiceException("SendGrid API unavailable");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleExternalServiceException(
                exception, webRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("SendGrid API unavailable");
        assertThat(response.getBody().getStatus()).isEqualTo(502);
    }

    @Test
    void errorResponse_shouldHaveConsistentStructure() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Test");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(
                exception, webRequest);

        // Assert
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isNotNull();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getMessage()).isNotNull();
        assertThat(body.getPath()).isNotNull();
    }
}
