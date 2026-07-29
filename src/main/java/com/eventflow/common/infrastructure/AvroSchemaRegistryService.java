package com.eventflow.common.infrastructure;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Service for Avro schema management and serialization.
 * Handles schema registration, compatibility checks, and binary serialization
 * for Kafka messages using the Confluent Schema Registry.
 *
 * As specified in the PRD Section 31-32 - Event Contracts / Avro Schema Registry.
 * Enforces BACKWARD compatibility: producers can add new optional fields
 * but cannot remove or rename existing fields without a major version bump.
 */
@Service
public class AvroSchemaRegistryService {

    private static final Logger log = LoggerFactory.getLogger(AvroSchemaRegistryService.class);

    private static final byte MAGIC_BYTE = 0x0;
    private static final int SCHEMA_ID_SIZE = 4;

    @Value("${spring.kafka.properties.schema.registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    /**
     * Serializes a GenericRecord to Avro binary format with Schema Registry header.
     * Format: [Magic Byte (1)] [Schema ID (4)] [Avro Binary Payload]
     *
     * @param record the Avro GenericRecord to serialize
     * @param schemaId the Schema Registry schema ID
     * @return byte array ready for Kafka message value
     */
    public byte[] serializeWithSchemaId(GenericRecord record, int schemaId) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Write magic byte and schema ID
            outputStream.write(MAGIC_BYTE);
            outputStream.write(ByteBuffer.allocate(SCHEMA_ID_SIZE).putInt(schemaId).array());

            // Write Avro binary payload
            DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(record.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            datumWriter.write(record, encoder);
            encoder.flush();

            byte[] bytes = outputStream.toByteArray();
            log.debug("Serialized Avro record: schemaId={}, recordType={}, size={} bytes",
                schemaId, record.getSchema().getName(), bytes.length);
            return bytes;
        } catch (IOException e) {
            log.error("Failed to serialize Avro record: {}", e.getMessage(), e);
            throw new RuntimeException("Avro serialization failed", e);
        }
    }

    /**
     * Extracts the schema ID from the first 5 bytes of a Kafka message.
     *
     * @param bytes the raw Kafka message bytes
     * @return the schema ID, or -1 if the format is invalid
     */
    public int extractSchemaId(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
            log.warn("Invalid Avro message: too short ({} bytes)", bytes == null ? 0 : bytes.length);
            return -1;
        }

        if (bytes[0] != MAGIC_BYTE) {
            log.warn("Invalid Avro message: missing magic byte, expected 0x00, got 0x{}",
                Integer.toHexString(bytes[0] & 0xFF));
            return -1;
        }

        return ByteBuffer.wrap(bytes, 1, SCHEMA_ID_SIZE).getInt();
    }

    /**
     * Validates a schema evolution for backward compatibility.
     * Checks that no existing required fields are removed.
     *
     * @param existingSchema the currently registered schema
     * @param newSchema the proposed new schema
     * @return true if the new schema is backward compatible
     */
    public boolean isBackwardCompatible(Schema existingSchema, Schema newSchema) {
        if (existingSchema == null || newSchema == null) {
            return false;
        }

        // Check that all existing fields exist in the new schema
        for (Schema.Field existingField : existingSchema.getFields()) {
            Schema.Field newField = newSchema.getField(existingField.name());
            if (newField == null) {
                log.warn("Backward compatibility failed: field '{}' removed in new schema",
                    existingField.name());
                return false;
            }

            // Check that the type is compatible (allowing evolution to union with null)
            if (!isTypeCompatible(existingField.schema(), newField.schema())) {
                log.warn("Backward compatibility failed: field '{}' type changed from {} to {}",
                    existingField.name(), existingField.schema(), newField.schema());
                return false;
            }
        }

        log.info("Schema backward compatibility check passed: {} -> {}",
            existingSchema.getName(), newSchema.getName());
        return true;
    }

    /**
     * Checks if an existing field type is compatible with a new field type.
     * Allows: same type, adding null to union, promoting types.
     */
    private boolean isTypeCompatible(Schema existingType, Schema newType) {
        if (existingType.equals(newType)) {
            return true;
        }

        // If existing is a named type, just check equality
        if (existingType.getType() == Schema.Type.RECORD ||
            existingType.getType() == Schema.Type.ENUM ||
            existingType.getType() == Schema.Type.FIXED) {
            return existingType.getFullName().equals(newType.getFullName());
        }

        // Allow promoting int to long
        if (existingType.getType() == Schema.Type.INT &&
            newType.getType() == Schema.Type.LONG) {
            return true;
        }

        // Allow promoting float to double
        if (existingType.getType() == Schema.Type.FLOAT &&
            newType.getType() == Schema.Type.DOUBLE) {
            return true;
        }

        // Allow adding null to union (making a field optional)
        if (newType.getType() == Schema.Type.UNION) {
            for (Schema unionType : newType.getTypes()) {
                if (unionType.getType() == Schema.Type.NULL) {
                    // Check if the other type matches the existing type
                    for (Schema otherType : newType.getTypes()) {
                        if (otherType.getType() != Schema.Type.NULL &&
                            isTypeCompatible(existingType, otherType)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the Schema Registry URL for health checks.
     */
    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }
}