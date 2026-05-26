package com.psychosim.simulation.application.port.out;

import java.time.Instant;
import java.util.Map;

public interface AuditTrailPort {
    void append(String actorId, String actorRole, String action, Map<String, String> context, Instant occurredAt);
}
