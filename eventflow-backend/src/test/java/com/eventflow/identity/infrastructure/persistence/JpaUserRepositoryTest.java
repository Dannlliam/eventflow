package com.eventflow.identity.infrastructure.persistence;

import com.eventflow.identity.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JpaUserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaUserRepositoryAdapter repository;

    @Test
    void save_shouldPersistUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .email("admin@example.com")
                .passwordHash("hashed-password")
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();

        User saved = repository.save(user);
        entityManager.flush();

        assertThat(saved.getId()).isEqualTo(user.getId());
    }

    @Test
    void findByEmail_shouldReturnUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed")
                .role(User.Role.DEVELOPER)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        repository.save(user);
        entityManager.flush();

        Optional<User> result = repository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByWorkspaceId_shouldReturnAllUsers() {
        UUID workspaceId = UUID.randomUUID();
        
        repository.save(User.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email("user1@example.com")
                .passwordHash("hash1")
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        repository.save(User.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email("user2@example.com")
                .passwordHash("hash2")
                .role(User.Role.ANALYST)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<User> results = repository.findByWorkspaceId(workspaceId);

        assertThat(results).hasSize(2);
    }

    @Test
    void findByWorkspaceIdAndRole_shouldFilterByRole() {
        UUID workspaceId = UUID.randomUUID();
        
        repository.save(User.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email("admin@example.com")
                .passwordHash("hash")
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        repository.save(User.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email("dev@example.com")
                .passwordHash("hash")
                .role(User.Role.DEVELOPER)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<User> admins = repository.findByWorkspaceIdAndRole(workspaceId, User.Role.ADMIN);

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .role(User.Role.DEVELOPER)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build();
        repository.save(user);
        entityManager.flush();
        entityManager.clear();

        User updated = User.builder()
                .id(user.getId())
                .workspaceId(user.getWorkspaceId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .status(User.Status.SUSPENDED)
                .createdAt(user.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        repository.save(updated);
        entityManager.flush();

        User result = repository.findById(user.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(User.Status.SUSPENDED);
    }

    @Test
    void findByStatus_shouldFilterByStatus() {
        UUID workspaceId = UUID.randomUUID();
        
        repository.save(User.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email("active@example.com")
                .passwordHash("hash")
                .role(User.Role.DEVELOPER)
                .status(User.Status.ACTIVE)
                .createdAt(Instant.now())
                .build());
        
        repository.save(User.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email("invited@example.com")
                .passwordHash("hash")
                .role(User.Role.ANALYST)
                .status(User.Status.INVITED)
                .createdAt(Instant.now())
                .build());
        
        entityManager.flush();

        List<User> active = repository.findByStatus(User.Status.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(User.Status.ACTIVE);
    }
}
