package com.eventflow.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Unit tests for the BaseEntity abstract class.
 */
@DisplayName("BaseEntity abstract class")
class BaseEntityTest {

    @Test
    @DisplayName("should create new entity with UUID and timestamps")
    void constructor_NewEntity_SetsIdAndTimestamps() {
        TestEntity entity = new TestEntity();
        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0, entity.getVersion());
    }

    @Test
    @DisplayName("should reconstitute entity with all fields")
    void constructor_Reconstitute_SetsAllFields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2023-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2023-01-02T00:00:00Z");

        TestEntity entity = new TestEntity(id, createdAt, updatedAt, 5);

        assertEquals(id, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
        assertEquals(5, entity.getVersion());
    }

    @Test
    @DisplayName("should update updatedAt and increment version on markUpdated")
    void markUpdated_UpdatesTimestampAndVersion() throws InterruptedException {
        TestEntity entity = new TestEntity();
        Instant originalUpdatedAt = entity.getUpdatedAt();
        long originalVersion = entity.getVersion();

        // Small delay to ensure different timestamp
        Thread.sleep(1);
        entity.markUpdated();

        assertNotEquals(originalUpdatedAt, entity.getUpdatedAt());
        assertEquals(originalVersion + 1, entity.getVersion());
    }

    private static class TestEntity extends BaseEntity {
        public TestEntity() { super(); }
        public TestEntity(UUID id, Instant createdAt, Instant updatedAt, long version) {
            super(id, createdAt, updatedAt, version);
        }
    }
}