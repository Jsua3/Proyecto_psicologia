package com.psychosim.simulation.application.port.in;

import java.util.UUID;

public record SafeExitCommand(
        UUID attemptId,
        String attemptToken,
        Long studentId,
        String reason
) {
}
