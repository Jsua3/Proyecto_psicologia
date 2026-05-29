package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AttemptEventJpaRepository extends JpaRepository<AttemptEventEntity, Long> {
    List<AttemptEventEntity> findByAttemptIdOrderByOccurredAt(UUID attemptId);

    @Query("""
            SELECT COUNT(e) FROM AttemptEventEntity e
            WHERE e.attempt.id = :attemptId
              AND e.eventType IN ('DECISION_SELECTED', 'PROHIBITED_DECISION_SELECTED')
              AND e.decisionOption.classification = :classification
            """)
    long countDecisionsByClassification(UUID attemptId, com.psychosim.simulation.domain.model.DecisionClassification classification);

    @Query("""
            SELECT COUNT(e) FROM AttemptEventEntity e
            WHERE e.eventType IN ('DECISION_SELECTED', 'PROHIBITED_DECISION_SELECTED')
              AND e.decisionOption.classification = :classification
            """)
    long countAllDecisionsByClassification(com.psychosim.simulation.domain.model.DecisionClassification classification);

    @Query("""
            SELECT COUNT(e) FROM AttemptEventEntity e
            WHERE e.eventType = 'PROHIBITED_DECISION_SELECTED'
            """)
    long countProhibitedDecisions();

    @Query("""
            SELECT COUNT(e) FROM AttemptEventEntity e
            WHERE e.eventType = 'TOOL_USED'
            """)
    long countToolUsageEvents();
}
