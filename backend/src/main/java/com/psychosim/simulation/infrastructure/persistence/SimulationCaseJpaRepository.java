package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationCaseJpaRepository extends JpaRepository<SimulationCaseEntity, Long> {
    Optional<SimulationCaseEntity> findByCode(String code);
}
