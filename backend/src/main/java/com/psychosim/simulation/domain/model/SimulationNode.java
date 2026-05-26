package com.psychosim.simulation.domain.model;

import java.util.List;
import java.util.Set;

public record SimulationNode(
        NodeId nodeId,
        String title,
        String narrative,
        List<String> supportResources,
        Set<ToolRequirement> requiredTools,
        ContentSafety contentSafety,
        boolean start,
        boolean terminal
) {
    public SimulationNode {
        if (nodeId == null) {
            throw new IllegalArgumentException("El nodo requiere identificador");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El nodo requiere titulo");
        }
        if (narrative == null || narrative.isBlank()) {
            throw new IllegalArgumentException("El nodo requiere narrativa");
        }
        supportResources = supportResources == null ? List.of() : List.copyOf(supportResources);
        requiredTools = requiredTools == null ? Set.of() : Set.copyOf(requiredTools);
        contentSafety = contentSafety == null ? ContentSafety.standard() : contentSafety;
    }
}
