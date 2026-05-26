package com.psychosim.simulation.application.port.in;

import java.util.UUID;

public record SelectDecisionCommand(
        UUID attemptId,
        String attemptToken,
        Long studentId,
        String decisionOptionId
) {
}
