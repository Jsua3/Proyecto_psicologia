package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationAttemptJpaRepository extends JpaRepository<SimulationAttemptEntity, UUID> {
    Optional<SimulationAttemptEntity> findByIdAndAttemptTokenHash(UUID id, String attemptTokenHash);

    List<SimulationAttemptEntity> findTop20ByOrderByStartedAtDesc();
}
