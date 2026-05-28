package com.psychosim.simulation.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.simulation.application.port.out.AuditTrailPort;
import com.psychosim.simulation.infrastructure.persistence.AuditLogEntity;
import com.psychosim.simulation.infrastructure.persistence.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Infrastructure adapter that implements {@link AuditTrailPort} by persisting to
 * {@code audit_logs} in an independent transaction ({@code REQUIRES_NEW}) so that
 * the audit record is committed even if the outer transaction rolls back.
 *
 * <p>Audit must <em>never</em> interrupt the business operation: all exceptions
 * inside this adapter are caught and logged at WARN level.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAdapter implements AuditTrailPort {

    private static final int RETENTION_MONTHS = 12;
    private static final int MAX_USER_AGENT_LEN = 500;

    private final AuditLogJpaRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // ─── AuditTrailPort (minimal port used by domain/application layer) ──────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(String actorId, String actorRole, String action,
                       Map<String, String> context, Instant occurredAt) {
        persist(actorId, actorRole, action, null, null, context, null, null, occurredAt);
    }

    // ─── Richer overload used by the AOP aspect (infrastructure layer) ───────────

    /**
     * Full-signature audit write.  Called by {@link SimulationAuditAspect} to include
     * HTTP-level metadata (IP, User-Agent) and domain resource coordinates.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(String actorId, String actorRole,
                       String action, String resourceType, String resourceId,
                       Map<String, String> context,
                       String ipAddress, String userAgent,
                       Instant occurredAt) {
        persist(actorId, actorRole, action, resourceType, resourceId, context, ipAddress, userAgent, occurredAt);
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    private void persist(String actorId, String actorRole,
                         String action, String resourceType, String resourceId,
                         Map<String, String> context,
                         String ipAddress, String userAgent,
                         Instant occurredAt) {
        try {
            AuditLogEntity entity = new AuditLogEntity();

            if (actorId != null) {
                try {
                    entity.setActorId(Long.parseLong(actorId));
                } catch (NumberFormatException ignored) {
                    // actorId may be a UUID (attempt token) — store as null FK, propagate via context
                }
            }

            entity.setActorRole(actorRole);
            entity.setAction(action);
            entity.setResourceType(resourceType);
            entity.setResourceId(resourceId);
            entity.setContextJson(objectMapper.writeValueAsString(
                    context != null ? context : Map.of()));
            entity.setIpAddress(ipAddress);
            entity.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LEN));
            entity.setOccurredAt(occurredAt != null ? occurredAt : Instant.now());
            entity.setRetentionUntil(entity.getOccurredAt().plus(RETENTION_MONTHS * 30L, ChronoUnit.DAYS));

            auditLogRepository.save(entity);

        } catch (Exception ex) {
            // Audit failures must never propagate to callers
            log.warn("Audit persistence failed [action={}]: {}", action, ex.getMessage(), ex);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
