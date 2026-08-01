package com.eventflow.analytics.infrastructure;

import com.eventflow.analytics.application.AuditLogRepository;
import com.eventflow.analytics.domain.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.*;
import java.util.Optional;
import java.util.UUID;

/**
 * AOP aspect that intercepts auditable operations and logs them to the audit_logs table.
 * Asynchronous persistence ensures audit logging does not slow down the main transaction.
 */
@Aspect
@Component
public class AuditingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditingAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AuditingAspect(AuditLogRepository auditLogRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.auditLogRepository = auditLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Pointcut("@annotation(auditable)")
    public void auditableOperation(Auditable auditable) {}

    /**
     * After returning from an auditable operation, create an audit log entry.
     */
    @AfterReturning(pointcut = "auditableOperation(auditable)", argNames = "joinPoint, auditable")
    public void auditAfterReturning(JoinPoint joinPoint, Auditable auditable) {
        try {
            String action = auditable.action();
            String entityType = auditable.entityType();
            String entityId = extractEntityId(joinPoint);

            UUID userId = getCurrentUserId();
            UUID workspaceId = getCurrentWorkspaceId();
            String ipAddress = getClientIp();
            String userAgent = getUserAgent();

            AuditLog auditLog = new AuditLog(
                userId, workspaceId, action, entityType, entityId,
                null, ipAddress, userAgent
            );

            // Persist asynchronously to avoid impacting the main transaction
            eventPublisher.publishEvent(new AuditEvent(auditLog));

            log.debug("Audit log created: action={}, entityType={}, entityId={}, userId={}",
                action, entityType, entityId, userId);

        } catch (Exception e) {
            log.warn("Failed to create audit log (non-fatal): {}", e.getMessage());
        }
    }

    @Async
    public void persistAuditLog(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return UUID.fromString(auth.getName());
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private UUID getCurrentWorkspaceId() {
        String workspaceId = MDC.get("workspaceId");
        if (workspaceId != null) {
            return UUID.fromString(workspaceId);
        }
        return null;
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    private String getUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getHeader("User-Agent");
        }
        return null;
    }

    private String extractEntityId(JoinPoint joinPoint) {
        // Try to extract entity ID from method arguments
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof String s && s.matches("^[a-fA-F0-9-]{36}$")) {
                return s;
            }
            if (arg instanceof UUID uuid) {
                return uuid.toString();
            }
        }
        return null;
    }

    /**
     * Spring application event for async audit log persistence.
     */
    public record AuditEvent(AuditLog auditLog) {}

    /**
     * Annotation to mark methods for auditing.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Auditable {
        String action();
        String entityType() default "";
    }
}