package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricCriterionJpaRepository extends JpaRepository<RubricCriterionEntity, Long> {
    List<RubricCriterionEntity> findByRubricIdOrderByDisplayOrder(Long rubricId);
}
