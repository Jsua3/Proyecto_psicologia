package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RubricJpaRepository extends JpaRepository<RubricEntity, Long> {
    List<RubricEntity> findByCaseVersionIdAndActiveTrueOrderById(Long caseVersionId);

    List<RubricEntity> findByCaseVersionIdOrderById(Long caseVersionId);

    Optional<RubricEntity> findFirstByCaseVersionIdAndActiveTrueOrderById(Long caseVersionId);
}
