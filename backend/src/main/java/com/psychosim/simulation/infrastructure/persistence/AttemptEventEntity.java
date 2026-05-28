package com.psychosim.simulation.infrastructure.persistence;

import com.psychosim.simulation.domain.model.AttemptEventType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempt_events")
@Getter
@Setter
@NoArgsConstructor
public class AttemptEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private SimulationAttemptEntity attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private AttemptEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private SimulationNodeEntity node;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_option_id")
    private DecisionOptionEntity decisionOption;

    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    @Column(name = "stress_delta", nullable = false)
    private int stressDelta;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}
