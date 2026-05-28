package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RubricEvaluationJpaRepository extends JpaRepository<RubricEvaluationEntity, Long> {
    Optional<RubricEvaluationEntity> findByAttemptIdAndRubricIdAndInstructorId(UUID attemptId, Long rubricId, Long instructorId);

    List<RubricEvaluationEntity> findByAttemptIdOrderByEvaluatedAtDesc(UUID attemptId);
}
