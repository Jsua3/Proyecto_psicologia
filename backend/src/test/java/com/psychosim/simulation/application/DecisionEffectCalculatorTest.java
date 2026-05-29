package com.psychosim.simulation.application;

import com.psychosim.simulation.domain.model.DecisionClassification;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DecisionEffectCalculatorTest {

    private final DecisionEffectCalculator calculator = new DecisionEffectCalculator();

    @Test
    void adequateDecisionIncreasesTrustAndReducesVictimRisk() {
        DecisionOptionEntity decision = mockDecision(
                DecisionClassification.ADEQUATE,
                15,
                -10,
                false,
                "node_emotional_containment",
                "Contención emocional"
        );

        var effects = calculator.resolve(decision);

        assertEquals(15, effects.scoreDelta());
        assertEquals(-10, effects.stressDelta());
        assertTrue(effects.trustDelta() > 0);
        assertTrue(effects.victimRiskDelta() < 0);
    }

    @Test
    void prohibitedDecisionFlagsRevictimizationRisk() {
        DecisionOptionEntity decision = mockDecision(
                DecisionClassification.INADEQUATE,
                0,
                5,
                true,
                "node_alert",
                "Alerta"
        );

        var effects = calculator.resolve(decision);

        assertTrue(effects.revictimizationRisk());
        assertTrue(effects.scoreDelta() < 0);
        assertTrue(effects.stressDelta() >= 40);
    }

    private DecisionOptionEntity mockDecision(
            DecisionClassification classification,
            int scoreDelta,
            int stressDelta,
            boolean prohibited,
            String targetKey,
            String targetTitle
    ) {
        DecisionOptionEntity decision = mock(DecisionOptionEntity.class);
        var targetNode = mock(com.psychosim.simulation.infrastructure.persistence.SimulationNodeEntity.class);
        when(targetNode.getNodeKey()).thenReturn(targetKey);
        when(targetNode.getTitle()).thenReturn(targetTitle);
        when(decision.getClassification()).thenReturn(classification);
        when(decision.getScoreDelta()).thenReturn(scoreDelta);
        when(decision.getStressDelta()).thenReturn(stressDelta);
        when(decision.isProhibitedConduct()).thenReturn(prohibited);
        when(decision.getProhibitedPenalty()).thenReturn(-50);
        when(decision.getImmediateFeedback()).thenReturn("Retroalimentación clínica.");
        when(decision.getTargetNode()).thenReturn(targetNode);
        return decision;
    }
}
