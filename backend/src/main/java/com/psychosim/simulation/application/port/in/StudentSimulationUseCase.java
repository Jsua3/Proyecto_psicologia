package com.psychosim.simulation.application.port.in;

import com.psychosim.simulation.domain.model.SimulationAttempt;
import com.psychosim.simulation.domain.service.SimulationEngine.TransitionResult;

public interface StudentSimulationUseCase {
    SimulationAttempt start(StartSimulationCommand command);

    TransitionResult choose(SelectDecisionCommand command);

    void recordReflection(RecordReflectionCommand command);

    SimulationAttempt safeExit(SafeExitCommand command);
}
