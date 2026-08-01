package com.eventflow.common.domain;

import java.lang.annotation.*;

/**
 * Annotation to mark domain entities that should be audited.
 * When applied to an entity, the auditing infrastructure will track
 * creation, modification, and deletion events for compliance and traceability.
 * <p>
 * As specified in the PRD Section 73 - OWASP Compliance / Audit Logging.
 * <p>
 * Usage: {@code @Auditable("Notification")) on domain entities}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * A human-readable name for the audited entity type.
     * Used in audit log entries.
     */
    String value() default "";

    /**
     * Whether to track read operations in addition to writes.
     * Should be used sparingly due to volume.
     */
    boolean trackReads() default false;

    /**
     * Specific fields to exclude from audit tracking (e.g., sensitive data).
     */
    String[] excludeFields() default {};
}