package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReflectionJournalJpaRepository extends JpaRepository<ReflectionJournalEntity, Long> {
    Optional<ReflectionJournalEntity> findByAttemptIdAndNodeId(UUID attemptId, Long nodeId);

    List<ReflectionJournalEntity> findByAttemptId(UUID attemptId);
}
