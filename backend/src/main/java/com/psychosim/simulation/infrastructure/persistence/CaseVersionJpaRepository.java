package com.psychosim.simulation.infrastructure.persistence;

import com.psychosim.simulation.domain.model.CasePublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseVersionJpaRepository extends JpaRepository<CaseVersionEntity, Long> {
    List<CaseVersionEntity> findByStatusAndSimulationCaseActiveTrueOrderByPublishedAtDesc(CasePublicationStatus status);

    List<CaseVersionEntity> findBySimulationCaseIdOrderByCreatedAtDesc(Long simulationCaseId);

    Optional<CaseVersionEntity> findTopBySimulationCaseIdOrderByCreatedAtDesc(Long simulationCaseId);
}
