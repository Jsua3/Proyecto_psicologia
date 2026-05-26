package com.psychosim.simulation.application.port.out;

import com.psychosim.simulation.domain.model.AttemptId;
import com.psychosim.simulation.domain.model.SimulationAttempt;

import java.util.Optional;

public interface SimulationAttemptRepositoryPort {
    SimulationAttempt save(SimulationAttempt attempt);

    Optional<SimulationAttempt> findById(AttemptId attemptId);
}
