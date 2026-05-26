package com.psychosim.simulation.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationCaseVersionTest {

    @Test
    void requiresExactlyOneStartNode() {
        SimulationNode first = node("inicio", true, false);
        SimulationNode second = node("otro-inicio", true, true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SimulationCaseVersion(
                        new CaseVersionId(1L),
                        "VBG-001",
                        "Caso sensible",
                        "1.0.0",
                        CasePublicationStatus.DRAFT,
                        List.of(first, second),
                        List.of()
                ));

        assertEquals("El grafo debe tener exactamente un nodo inicial", ex.getMessage());
    }

    @Test
    void rejectsCyclesBecauseSimulationCasesMustBeDag() {
        SimulationNode start = node("inicio", true, false);
        SimulationNode middle = node("medio", false, false);

        DecisionOption toMiddle = decision("d1", "inicio", "medio");
        DecisionOption backToStart = decision("d2", "medio", "inicio");

        assertThrows(IllegalArgumentException.class, () ->
                new SimulationCaseVersion(
                        new CaseVersionId(1L),
                        "VBG-001",
                        "Caso sensible",
                        "1.0.0",
                        CasePublicationStatus.DRAFT,
                        List.of(start, middle),
                        List.of(toMiddle, backToStart)
                ));
    }

    @Test
    void exposesOutgoingDecisionsFromSourceNode() {
        SimulationCaseVersion version = validCaseVersion();

        List<DecisionOption> outgoing = version.outgoingFrom(new NodeId("inicio"));

        assertEquals(1, outgoing.size());
        assertEquals(new DecisionOptionId("d1"), outgoing.getFirst().decisionOptionId());
    }

    static SimulationCaseVersion validCaseVersion() {
        SimulationNode start = node("inicio", true, false);
        SimulationNode end = node("cierre", false, true);
        return new SimulationCaseVersion(
                new CaseVersionId(1L),
                "VBG-001",
                "Violencia familiar y tentativa de feminicidio",
                "1.0.0",
                CasePublicationStatus.PUBLISHED,
                List.of(start, end),
                List.of(decision("d1", "inicio", "cierre"))
        );
    }

    private static SimulationNode node(String id, boolean start, boolean terminal) {
        return new SimulationNode(
                new NodeId(id),
                "Nodo " + id,
                "Narrativa academica del nodo " + id,
                List.of("Guia institucional"),
                Set.of(ToolRequirement.REFLECTION_JOURNAL),
                ContentSafety.standard(),
                start,
                terminal
        );
    }

    private static DecisionOption decision(String id, String source, String target) {
        return new DecisionOption(
                new DecisionOptionId(id),
                new NodeId(source),
                new NodeId(target),
                "Aplicar intervencion segura",
                DecisionClassification.ADEQUATE,
                ScoreRule.adequate(),
                "Decision coherente con el enfoque etico y de derechos.",
                false,
                null
        );
    }
}
