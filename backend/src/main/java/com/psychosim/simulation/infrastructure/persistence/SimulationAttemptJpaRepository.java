package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationAttemptJpaRepository extends JpaRepository<SimulationAttemptEntity, UUID> {
    Optional<SimulationAttemptEntity> findByIdAndAttemptTokenHash(UUID id, String attemptTokenHash);

    List<SimulationAttemptEntity> findTop20ByOrderByStartedAtDesc();

    long countByStatusIn(List<com.psychosim.simulation.domain.model.AttemptStatus> statuses);

    @Query("SELECT COUNT(a) FROM SimulationAttemptEntity a WHERE a.status IN ('COMPLETED', 'SAFE_EXITED') AND a.endedAt >= :since")
    long countCompletedSince(LocalDateTime since);

    @Query("SELECT COALESCE(AVG(a.accumulatedScore), 0) FROM SimulationAttemptEntity a WHERE a.status IN ('COMPLETED', 'SAFE_EXITED')")
    Double averageCompletedScore();

    List<SimulationAttemptEntity> findTop10ByOrderByStartedAtDesc();

    Optional<SimulationAttemptEntity> findFirstByStudent_IdAndCaseVersion_IdAndStatusOrderByStartedAtDesc(
            Long studentId, Long caseVersionId, com.psychosim.simulation.domain.model.AttemptStatus status);

    List<SimulationAttemptEntity> findByStudent_IdAndCaseVersion_IdAndStatus(
            Long studentId, Long caseVersionId, com.psychosim.simulation.domain.model.AttemptStatus status);

    @Query("""
            SELECT a FROM SimulationAttemptEntity a
            WHERE a.caseVersion.id = :caseVersionId
              AND a.student.id IN :studentIds
            ORDER BY a.startedAt DESC
            """)
    List<SimulationAttemptEntity> findByCaseVersionIdAndStudentIds(Long caseVersionId, List<Long> studentIds);
}
