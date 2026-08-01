package com.eventflow.identity.application;

import com.eventflow.identity.domain.Role;
import com.eventflow.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreateUserUseCase useCase;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        useCase = new CreateUserUseCase(userRepository, passwordEncoder);
        workspaceId = UUID.randomUUID();
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        String email = "user@example.com";
        String password = "SecurePassword123!";
        String name = "Test User";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User result = useCase.execute(workspaceId, email, password, name, Role.DEVELOPER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getRole()).isEqualTo(Role.DEVELOPER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldEncodePassword() {
        // Given
        String rawPassword = "MyPassword123!";
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedHash");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        useCase.execute(workspaceId, "test@example.com", rawPassword, "Test", Role.DEVELOPER);

        // Then
        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        String existingEmail = "existing@example.com";
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(mock(User.class)));

        // When/Then
        assertThatThrownBy(() -> useCase.execute(workspaceId, existingEmail, "password", "Name", Role.DEVELOPER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldValidateEmailFormat() {
        // Given
        String invalidEmail = "not-an-email";

        // When/Then
        assertThatThrownBy(() -> useCase.execute(workspaceId, invalidEmail, "password", "Name", Role.DEVELOPER))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSetWorkspaceId() {
        // Given
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(userCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        useCase.execute(workspaceId, "test@example.com", "password", "Name", Role.DEVELOPER);

        // Then
        User captured = userCaptor.getValue();
        assertThat(captured.getWorkspaceId()).isEqualTo(workspaceId);
    }
}
