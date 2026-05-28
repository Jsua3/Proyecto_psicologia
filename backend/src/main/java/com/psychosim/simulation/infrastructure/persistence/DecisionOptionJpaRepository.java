package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionOptionJpaRepository extends JpaRepository<DecisionOptionEntity, Long> {
    List<DecisionOptionEntity> findBySourceNodeIdOrderById(Long sourceNodeId);

    List<DecisionOptionEntity> findByCaseVersionIdOrderById(Long caseVersionId);
}
