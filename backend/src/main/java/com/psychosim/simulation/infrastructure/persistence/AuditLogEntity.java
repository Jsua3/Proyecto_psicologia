package com.psychosim.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persists every auditable action in the platform to the audit_logs table (V4 migration).
 * Retention minimum 12 months; see {@link com.psychosim.simulation.infrastructure.audit.AuditLogCleanupScheduler}.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to users.id — nullable so anonymous/system actions can be logged. */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role", length = 40)
    private String actorRole;

    @Column(name = "action", length = 120, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 80)
    private String resourceType;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    /** JSON object with extra context (e.g. method name, decision classification). */
    @Column(name = "context_json", nullable = false, columnDefinition = "TEXT")
    private String contextJson = "{}";

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Absolute timestamp after which this row may be purged. */
    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;
}
