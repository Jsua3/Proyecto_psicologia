package com.psychosim.simulation.infrastructure.persistence;

import com.psychosim.simulation.domain.model.DecisionClassification;
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

@Entity
@Table(name = "decision_options")
@Getter
@Setter
@NoArgsConstructor
public class DecisionOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_version_id", nullable = false)
    private CaseVersionEntity caseVersion;

    @Column(name = "option_key", nullable = false, length = 120)
    private String optionKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    private SimulationNodeEntity sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private SimulationNodeEntity targetNode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DecisionClassification classification;

    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    @Column(name = "stress_delta", nullable = false)
    private int stressDelta;

    @Column(name = "prohibited_penalty", nullable = false)
    private int prohibitedPenalty;

    @Column(name = "immediate_feedback", nullable = false, columnDefinition = "TEXT")
    private String immediateFeedback;

    @Column(name = "prohibited_conduct", nullable = false)
    private boolean prohibitedConduct;

    @Column(name = "prohibition_reason", columnDefinition = "TEXT")
    private String prohibitionReason;
}
