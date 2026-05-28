package com.psychosim.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

@Entity
@Table(name = "criterion_scores")
@Getter
@Setter
@NoArgsConstructor
public class CriterionScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_evaluation_id", nullable = false)
    private RubricEvaluationEntity rubricEvaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_criterion_id", nullable = false)
    private RubricCriterionEntity rubricCriterion;

    @Column(nullable = false)
    private BigDecimal score = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "evidence_json", nullable = false, columnDefinition = "TEXT")
    private String evidenceJson = "{}";
}
