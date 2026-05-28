package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalToolJpaRepository extends JpaRepository<ClinicalToolEntity, Long> {
    List<ClinicalToolEntity> findByCaseVersionIdAndActiveTrueOrderById(Long caseVersionId);

    List<ClinicalToolEntity> findByCaseVersionIdOrderById(Long caseVersionId);

    Optional<ClinicalToolEntity> findByCaseVersionIdAndToolCode(Long caseVersionId, String toolCode);
}
