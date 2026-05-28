package com.psychosim.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublicationChecklistJpaRepository extends JpaRepository<PublicationChecklistEntity, Long> {
    Optional<PublicationChecklistEntity> findFirstByCaseVersionIdOrderBySubmittedAtDesc(Long caseVersionId);
}
