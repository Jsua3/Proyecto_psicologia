package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttemptEventJpaRepository extends JpaRepository<AttemptEventEntity, Long> {
    List<AttemptEventEntity> findByAttemptIdOrderByOccurredAt(UUID attemptId);
}
