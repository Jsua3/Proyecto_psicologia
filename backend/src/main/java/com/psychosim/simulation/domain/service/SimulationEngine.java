package com.psychosim.simulation.domain.service;

import com.psychosim.simulation.domain.model.AttemptEventType;
import com.psychosim.simulation.domain.model.DecisionConsequence;
import com.psychosim.simulation.domain.model.DecisionOption;
import com.psychosim.simulation.domain.model.DecisionOptionId;
import com.psychosim.simulation.domain.model.DomainRuleViolation;
import com.psychosim.simulation.domain.model.SimulationAttempt;
import com.psychosim.simulation.domain.model.SimulationCaseVersion;
import com.psychosim.simulation.domain.model.SimulationNode;

import java.time.Instant;

public class SimulationEngine {

    public TransitionResult applyDecision(
            SimulationCaseVersion caseVersion,
            SimulationAttempt attempt,
            DecisionOptionId decisionOptionId,
            Instant occurredAt
    ) {
        if (!caseVersion.id().equals(attempt.caseVersionId())) {
            throw new DomainRuleViolation("El intento no pertenece a la version de caso indicada");
        }
        if (!caseVersion.published()) {
            throw new DomainRuleViolation("Solo se pueden ejecutar versiones publicadas");
        }

        DecisionOption decision = caseVersion.decision(decisionOptionId);
        if (!decision.sourceNodeId().equals(attempt.currentNodeId())) {
            throw new DomainRuleViolation("La decision no esta disponible desde el nodo actual");
        }

        DecisionConsequence consequence = decision.scoreRule().evaluate(decision.prohibitedConduct());
        SimulationNode nextNode = caseVersion.node(decision.targetNodeId());
        AttemptEventType eventType = decision.prohibitedConduct()
                ? AttemptEventType.PROHIBITED_DECISION_SELECTED
                : AttemptEventType.DECISION_SELECTED;

        SimulationAttempt updatedAttempt = attempt.transitionTo(nextNode.nodeId(), decision, consequence, eventType, occurredAt);
        if (nextNode.terminal()) {
            updatedAttempt = updatedAttempt.complete(occurredAt);
        }

        return new TransitionResult(updatedAttempt, nextNode, decision, consequence);
    }

    public record TransitionResult(
            SimulationAttempt attempt,
            SimulationNode nextNode,
            DecisionOption decision,
            DecisionConsequence consequence
    ) {
    }
}
