package com.psychosim.simulation.application.port.out;

import com.psychosim.simulation.domain.model.CaseVersionId;
import com.psychosim.simulation.domain.model.SimulationCaseVersion;

import java.util.Optional;

public interface CaseGraphRepositoryPort {
    Optional<SimulationCaseVersion> findPublishedById(CaseVersionId caseVersionId);
}
