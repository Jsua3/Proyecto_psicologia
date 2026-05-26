package com.psychosim.simulation.application.port.in;

import java.util.UUID;

public record RecordReflectionCommand(
        UUID attemptId,
        String attemptToken,
        Long studentId,
        String nodeId,
        String reflectionText
) {
}
