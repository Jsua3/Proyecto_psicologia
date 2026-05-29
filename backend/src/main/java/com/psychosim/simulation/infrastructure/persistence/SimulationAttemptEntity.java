package com.psychosim.simulation.infrastructure.persistence;

import com.psychosim.domain.user.User;
import com.psychosim.simulation.domain.model.AttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "simulation_attempts_v2")
@Getter
@Setter
@NoArgsConstructor
public class SimulationAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "attempt_token_hash", nullable = false, unique = true, length = 128)
    private String attemptTokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_version_id", nullable = false)
    private CaseVersionEntity caseVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_node_id", nullable = false)
    private SimulationNodeEntity currentNode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "accumulated_score", nullable = false)
    private int accumulatedScore;

    @Column(name = "stress_index", nullable = false)
    private int stressIndex;

    @Column(name = "victim_risk", nullable = false)
    private int victimRisk = 50;

    @Column(name = "user_trust", nullable = false)
    private int userTrust = 50;

    @Column(name = "institutional_route_activated", nullable = false)
    private boolean institutionalRouteActivated;

    @Column(name = "revictimization_risk", nullable = false)
    private boolean revictimizationRisk;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
}
