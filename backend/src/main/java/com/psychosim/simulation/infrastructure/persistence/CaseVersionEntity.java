package com.psychosim.simulation.infrastructure.persistence;

import com.psychosim.domain.user.User;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_versions")
@Getter
@Setter
@NoArgsConstructor
public class CaseVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Bloqueo optimista — Decisión #2: protege contra ediciones concurrentes. */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    /** Versión del esquema del mundo (actualmente 2 = V6+V7). */
    @Column(name = "world_schema_version", nullable = false)
    private int worldSchemaVersion = 2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_case_id", nullable = false)
    private SimulationCaseEntity simulationCase;

    @Column(name = "semantic_version", nullable = false, length = 32)
    private String semanticVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CasePublicationStatus status;

    @Column(name = "narrative_context", columnDefinition = "TEXT")
    private String narrativeContext;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_from_id")
    private CaseVersionEntity clonedFrom;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
