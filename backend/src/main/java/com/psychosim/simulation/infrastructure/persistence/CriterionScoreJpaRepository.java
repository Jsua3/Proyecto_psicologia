package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterionScoreJpaRepository extends JpaRepository<CriterionScoreEntity, Long> {
    List<CriterionScoreEntity> findByRubricEvaluationId(Long rubricEvaluationId);

    void deleteByRubricEvaluationId(Long rubricEvaluationId);
}
