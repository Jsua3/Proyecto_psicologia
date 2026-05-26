package com.psychosim.simulation.domain.model;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SimulationCaseVersion(
        CaseVersionId id,
        String caseCode,
        String title,
        String semanticVersion,
        CasePublicationStatus status,
        List<SimulationNode> nodes,
        List<DecisionOption> decisions
) {
    public SimulationCaseVersion {
        if (id == null) {
            throw new IllegalArgumentException("La version del caso requiere identificador");
        }
        if (caseCode == null || caseCode.isBlank()) {
            throw new IllegalArgumentException("La version del caso requiere codigo");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("La version del caso requiere titulo");
        }
        if (semanticVersion == null || semanticVersion.isBlank()) {
            throw new IllegalArgumentException("La version del caso requiere version semantica");
        }
        status = status == null ? CasePublicationStatus.DRAFT : status;
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        validateGraph(nodes, decisions);
    }

    public boolean published() {
        return status == CasePublicationStatus.PUBLISHED;
    }

    public SimulationNode startNode() {
        return nodes.stream()
                .filter(SimulationNode::start)
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("El grafo no tiene nodo inicial"));
    }

    public SimulationNode node(NodeId nodeId) {
        return nodes.stream()
                .filter(node -> node.nodeId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Nodo inexistente en el grafo: " + nodeId.value()));
    }

    public DecisionOption decision(DecisionOptionId decisionOptionId) {
        return decisions.stream()
                .filter(decision -> decision.decisionOptionId().equals(decisionOptionId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolation("Decision inexistente: " + decisionOptionId.value()));
    }

    public List<DecisionOption> outgoingFrom(NodeId sourceNodeId) {
        return decisions.stream()
                .filter(decision -> decision.sourceNodeId().equals(sourceNodeId))
                .toList();
    }

    private static void validateGraph(List<SimulationNode> nodes, List<DecisionOption> decisions) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Un caso requiere al menos un nodo");
        }

        long startCount = nodes.stream().filter(SimulationNode::start).count();
        if (startCount != 1) {
            throw new IllegalArgumentException("El grafo debe tener exactamente un nodo inicial");
        }

        Set<NodeId> nodeIds = new HashSet<>();
        for (SimulationNode node : nodes) {
            if (!nodeIds.add(node.nodeId())) {
                throw new IllegalArgumentException("Nodo duplicado: " + node.nodeId().value());
            }
        }

        Set<DecisionOptionId> decisionIds = new HashSet<>();
        for (DecisionOption decision : decisions) {
            if (!decisionIds.add(decision.decisionOptionId())) {
                throw new IllegalArgumentException("Decision duplicada: " + decision.decisionOptionId().value());
            }
            if (!nodeIds.contains(decision.sourceNodeId()) || !nodeIds.contains(decision.targetNodeId())) {
                throw new IllegalArgumentException("Toda decision debe conectar nodos existentes");
            }
        }

        assertAcyclic(nodes, decisions);
    }

    private static void assertAcyclic(List<SimulationNode> nodes, List<DecisionOption> decisions) {
        Map<NodeId, List<NodeId>> adjacency = new HashMap<>();
        for (SimulationNode node : nodes) {
            adjacency.put(node.nodeId(), List.of());
        }
        Map<NodeId, java.util.ArrayList<NodeId>> mutable = new HashMap<>();
        for (SimulationNode node : nodes) {
            mutable.put(node.nodeId(), new java.util.ArrayList<>());
        }
        for (DecisionOption decision : decisions) {
            mutable.get(decision.sourceNodeId()).add(decision.targetNodeId());
        }
        mutable.forEach((key, value) -> adjacency.put(key, List.copyOf(value)));

        Set<NodeId> visiting = new HashSet<>();
        Set<NodeId> visited = new HashSet<>();
        ArrayDeque<NodeId> path = new ArrayDeque<>();
        for (SimulationNode node : nodes) {
            dfs(node.nodeId(), adjacency, visiting, visited, path);
        }
    }

    private static void dfs(
            NodeId nodeId,
            Map<NodeId, List<NodeId>> adjacency,
            Set<NodeId> visiting,
            Set<NodeId> visited,
            ArrayDeque<NodeId> path
    ) {
        if (visited.contains(nodeId)) {
            return;
        }
        if (!visiting.add(nodeId)) {
            path.addLast(nodeId);
            throw new IllegalArgumentException("El grafo de simulacion no puede contener ciclos");
        }
        path.addLast(nodeId);
        for (NodeId next : adjacency.getOrDefault(nodeId, List.of())) {
            dfs(next, adjacency, visiting, visited, path);
        }
        path.removeLast();
        visiting.remove(nodeId);
        visited.add(nodeId);
    }
}
