package com.psychosim.simulation.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record SimulationAttempt(
        AttemptId id,
        AttemptToken token,
        CaseVersionId caseVersionId,
        Long studentId,
        NodeId currentNodeId,
        AttemptStatus status,
        int accumulatedScore,
        int stressIndex,
        Instant startedAt,
        Instant endedAt,
        List<NodeId> visitedNodeIds,
        List<AttemptEvent> events
) {
    public SimulationAttempt {
        if (id == null || token == null || caseVersionId == null || currentNodeId == null) {
            throw new IllegalArgumentException("El intento requiere identificadores, token y nodo actual");
        }
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("El intento requiere estudiante valido");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("El intento requiere fecha de inicio");
        }
        status = status == null ? AttemptStatus.IN_PROGRESS : status;
        stressIndex = clampStress(stressIndex);
        visitedNodeIds = visitedNodeIds == null ? List.of() : List.copyOf(visitedNodeIds);
        events = events == null ? List.of() : List.copyOf(events);
    }

    public static SimulationAttempt start(CaseVersionId caseVersionId, Long studentId, NodeId startNodeId, Instant now) {
        AttemptId attemptId = AttemptId.newId();
        AttemptToken attemptToken = AttemptToken.create();
        List<AttemptEvent> initialEvents = List.of(
                AttemptEvent.started(now, startNodeId),
                AttemptEvent.nodeEntered(now, startNodeId)
        );
        return new SimulationAttempt(
                attemptId,
                attemptToken,
                caseVersionId,
                studentId,
                startNodeId,
                AttemptStatus.IN_PROGRESS,
                0,
                0,
                now,
                null,
                List.of(startNodeId),
                initialEvents
        );
    }

    public SimulationAttempt transitionTo(
            NodeId nextNodeId,
            DecisionOption decision,
            DecisionConsequence consequence,
            AttemptEventType eventType,
            Instant occurredAt
    ) {
        ensureInProgress();

        List<NodeId> visited = new ArrayList<>(visitedNodeIds);
        visited.add(nextNodeId);

        List<AttemptEvent> updatedEvents = new ArrayList<>(events);
        updatedEvents.add(AttemptEvent.decisionSelected(
                eventType,
                occurredAt,
                currentNodeId,
                decision.decisionOptionId(),
                consequence,
                decision.immediateFeedback()
        ));
        updatedEvents.add(AttemptEvent.nodeEntered(occurredAt, nextNodeId));

        return new SimulationAttempt(
                id,
                token,
                caseVersionId,
                studentId,
                nextNodeId,
                status,
                accumulatedScore + consequence.scoreDelta(),
                stressIndex + consequence.stressDelta(),
                startedAt,
                endedAt,
                visited,
                updatedEvents
        );
    }

    public SimulationAttempt complete(Instant occurredAt) {
        ensureInProgress();
        List<AttemptEvent> updatedEvents = new ArrayList<>(events);
        updatedEvents.add(AttemptEvent.completed(occurredAt, currentNodeId));
        return new SimulationAttempt(
                id,
                token,
                caseVersionId,
                studentId,
                currentNodeId,
                AttemptStatus.COMPLETED,
                accumulatedScore,
                stressIndex,
                startedAt,
                occurredAt,
                visitedNodeIds,
                updatedEvents
        );
    }

    public SimulationAttempt safeExit(Instant occurredAt, String reason) {
        ensureInProgress();
        List<AttemptEvent> updatedEvents = new ArrayList<>(events);
        updatedEvents.add(AttemptEvent.safeExit(occurredAt, currentNodeId, reason));
        return new SimulationAttempt(
                id,
                token,
                caseVersionId,
                studentId,
                currentNodeId,
                AttemptStatus.SAFE_EXITED,
                accumulatedScore,
                stressIndex,
                startedAt,
                occurredAt,
                visitedNodeIds,
                updatedEvents
        );
    }

    private void ensureInProgress() {
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new DomainRuleViolation("El intento ya no acepta cambios");
        }
    }

    private static int clampStress(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
