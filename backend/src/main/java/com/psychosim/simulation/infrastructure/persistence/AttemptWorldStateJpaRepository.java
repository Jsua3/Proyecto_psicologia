package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttemptWorldStateJpaRepository extends JpaRepository<AttemptWorldStateEntity, UUID> {
}
