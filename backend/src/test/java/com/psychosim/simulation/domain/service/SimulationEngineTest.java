package com.psychosim.simulation.domain.service;

import com.psychosim.simulation.domain.model.AttemptStatus;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import com.psychosim.simulation.domain.model.CaseVersionId;
import com.psychosim.simulation.domain.model.ContentSafety;
import com.psychosim.simulation.domain.model.DecisionClassification;
import com.psychosim.simulation.domain.model.DecisionOption;
import com.psychosim.simulation.domain.model.DecisionOptionId;
import com.psychosim.simulation.domain.model.DomainRuleViolation;
import com.psychosim.simulation.domain.model.NodeId;
import com.psychosim.simulation.domain.model.ScoreRule;
import com.psychosim.simulation.domain.model.SimulationAttempt;
import com.psychosim.simulation.domain.model.SimulationCaseVersion;
import com.psychosim.simulation.domain.model.SimulationNode;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    private final SimulationEngine engine = new SimulationEngine();
    private final Instant now = Instant.parse("2026-05-26T12:00:00Z");

    @Test
    void appliesTransitionAndCompletesAttemptWhenTargetNodeIsTerminal() {
        SimulationCaseVersion version = publishedCase(false);
        SimulationAttempt attempt = SimulationAttempt.start(version.id(), 7L, version.startNode().nodeId(), now);

        var result = engine.applyDecision(version, attempt, new DecisionOptionId("decision-segura"), now.plusSeconds(8));

        assertEquals(new NodeId("cierre"), result.nextNode().nodeId());
        assertEquals(AttemptStatus.COMPLETED, result.attempt().status());
        assertEquals(10, result.attempt().accumulatedScore());
        assertEquals(0, result.attempt().stressIndex());
        assertTrue(result.attempt().endedAt() != null);
    }

    @Test
    void appliesDrasticPenaltyForProhibitedConduct() {
        SimulationCaseVersion version = publishedCase(true);
        SimulationAttempt attempt = SimulationAttempt.start(version.id(), 7L, version.startNode().nodeId(), now);

        var result = engine.applyDecision(version, attempt, new DecisionOptionId("decision-segura"), now.plusSeconds(8));

        assertTrue(result.consequence().prohibitedConduct());
        assertEquals(-40, result.attempt().accumulatedScore());
        assertEquals(35, result.attempt().stressIndex());
    }

    @Test
    void rejectsDraftCaseExecution() {
        SimulationCaseVersion draft = new SimulationCaseVersion(
                new CaseVersionId(1L),
                "VBG-001",
                "Caso en borrador",
                "1.0.0",
                CasePublicationStatus.DRAFT,
                publishedCase(false).nodes(),
                publishedCase(false).decisions()
        );
        SimulationAttempt attempt = SimulationAttempt.start(draft.id(), 7L, draft.startNode().nodeId(), now);

        assertThrows(DomainRuleViolation.class, () ->
                engine.applyDecision(draft, attempt, new DecisionOptionId("decision-segura"), now));
    }

    private static SimulationCaseVersion publishedCase(boolean prohibited) {
        SimulationNode start = new SimulationNode(
                new NodeId("inicio"),
                "Sala de urgencias",
                "La estudiante debe elegir una intervencion inicial.",
                List.of(),
                Set.of(),
                ContentSafety.standard(),
                true,
                false
        );
        SimulationNode end = new SimulationNode(
                new NodeId("cierre"),
                "Cierre",
                "El caso queda cerrado para evaluacion.",
                List.of(),
                Set.of(),
                ContentSafety.standard(),
                false,
                true
        );
        DecisionOption decision = new DecisionOption(
                new DecisionOptionId("decision-segura"),
                start.nodeId(),
                end.nodeId(),
                "Aplicar PAP antes de activar la ruta.",
                prohibited ? DecisionClassification.INADEQUATE : DecisionClassification.ADEQUATE,
                ScoreRule.adequate(),
                "Retroalimentacion inmediata",
                prohibited,
                prohibited ? "Mediacion o revictimizacion prohibida en VBG" : null
        );

        return new SimulationCaseVersion(
                new CaseVersionId(1L),
                "VBG-001",
                "Caso publicado",
                "1.0.0",
                CasePublicationStatus.PUBLISHED,
                List.of(start, end),
                List.of(decision)
        );
    }
}
