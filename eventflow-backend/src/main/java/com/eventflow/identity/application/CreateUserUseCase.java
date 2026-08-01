package com.eventflow.identity.application;

import com.eventflow.identity.domain.EmailAddress;
import com.eventflow.identity.domain.Role;
import com.eventflow.identity.domain.User;
import com.eventflow.common.domain.DomainValidationException;
import java.util.Set;
import java.util.UUID;

/**
 * Use case for creating a new user.
 */
public class CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String email, String displayName, Set<Role> roles) {
        EmailAddress emailAddress = new EmailAddress(email);

        if (userRepository.existsByEmail(emailAddress.value())) {
            throw new DomainValidationException(
                "USER_ALREADY_EXISTS",
                "A user with email '" + email + "' already exists"
            );
        }

        User user = new User(emailAddress, displayName, roles);
        return userRepository.save(user);
    }
}