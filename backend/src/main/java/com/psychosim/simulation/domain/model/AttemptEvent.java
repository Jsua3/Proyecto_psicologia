package com.psychosim.simulation.domain.model;

import java.time.Instant;

public record AttemptEvent(
        AttemptEventType type,
        Instant occurredAt,
        NodeId nodeId,
        DecisionOptionId decisionOptionId,
        int scoreDelta,
        int stressDelta,
        String detail
) {
    public AttemptEvent {
        if (type == null || occurredAt == null) {
            throw new IllegalArgumentException("El evento requiere tipo y fecha");
        }
    }

    public static AttemptEvent started(Instant occurredAt, NodeId nodeId) {
        return new AttemptEvent(AttemptEventType.ATTEMPT_STARTED, occurredAt, nodeId, null, 0, 0, "Intento iniciado");
    }

    public static AttemptEvent nodeEntered(Instant occurredAt, NodeId nodeId) {
        return new AttemptEvent(AttemptEventType.NODE_ENTERED, occurredAt, nodeId, null, 0, 0, "Nodo visitado");
    }

    public static AttemptEvent decisionSelected(
            AttemptEventType type,
            Instant occurredAt,
            NodeId nodeId,
            DecisionOptionId decisionOptionId,
            DecisionConsequence consequence,
            String detail
    ) {
        return new AttemptEvent(
                type,
                occurredAt,
                nodeId,
                decisionOptionId,
                consequence.scoreDelta(),
                consequence.stressDelta(),
                detail
        );
    }

    public static AttemptEvent completed(Instant occurredAt, NodeId nodeId) {
        return new AttemptEvent(AttemptEventType.ATTEMPT_COMPLETED, occurredAt, nodeId, null, 0, 0, "Intento finalizado");
    }

    public static AttemptEvent safeExit(Instant occurredAt, NodeId nodeId, String reason) {
        return new AttemptEvent(AttemptEventType.SAFE_EXIT_REQUESTED, occurredAt, nodeId, null, 0, 0, reason);
    }
}
