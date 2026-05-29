package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReflectionJournalJpaRepository extends JpaRepository<ReflectionJournalEntity, Long> {
    Optional<ReflectionJournalEntity> findByAttemptIdAndNodeId(UUID attemptId, Long nodeId);

    List<ReflectionJournalEntity> findByAttemptId(UUID attemptId);

    long countByAttemptId(UUID attemptId);

    @Query("SELECT COUNT(r) FROM ReflectionJournalEntity r WHERE r.attempt.id IN :attemptIds")
    long countByAttemptIdIn(List<UUID> attemptIds);
}
