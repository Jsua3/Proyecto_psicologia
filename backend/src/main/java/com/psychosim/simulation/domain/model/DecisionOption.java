package com.psychosim.simulation.domain.model;

public record DecisionOption(
        DecisionOptionId decisionOptionId,
        NodeId sourceNodeId,
        NodeId targetNodeId,
        String text,
        DecisionClassification classification,
        ScoreRule scoreRule,
        String immediateFeedback,
        boolean prohibitedConduct,
        String prohibitionReason
) {
    public DecisionOption {
        if (decisionOptionId == null || sourceNodeId == null || targetNodeId == null) {
            throw new IllegalArgumentException("La opcion requiere identificador, origen y destino");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("La opcion de decision requiere texto visible");
        }
        if (classification == null) {
            throw new IllegalArgumentException("La opcion requiere clasificacion clinica/procedimental");
        }
        scoreRule = scoreRule == null ? ScoreRule.inadequate() : scoreRule;
        if (immediateFeedback == null || immediateFeedback.isBlank()) {
            throw new IllegalArgumentException("La opcion requiere retroalimentacion inmediata");
        }
        if (prohibitedConduct && (prohibitionReason == null || prohibitionReason.isBlank())) {
            throw new IllegalArgumentException("Una conducta prohibida requiere justificacion normativa");
        }
    }
}
