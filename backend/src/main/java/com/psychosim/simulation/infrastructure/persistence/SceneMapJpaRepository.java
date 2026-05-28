package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SceneMapJpaRepository extends JpaRepository<SceneMapEntity, Long> {
    Optional<SceneMapEntity> findByNodeId(Long nodeId);

    List<SceneMapEntity> findByCaseVersionIdOrderById(Long caseVersionId);
}
