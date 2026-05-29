package com.psychosim.simulation.application;

import com.psychosim.simulation.domain.model.DecisionClassification;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeEntity;
import org.springframework.stereotype.Component;

@Component
public class DecisionEffectCalculator {

    public DecisionEffects resolve(DecisionOptionEntity decision) {
        int scoreDelta = decision.getScoreDelta()
                + (decision.isProhibitedConduct() ? decision.getProhibitedPenalty() : 0);
        int stressDelta = decision.getStressDelta()
                + (decision.isProhibitedConduct() ? 40 : 0);

        DecisionClassification classification = decision.getClassification();
        int trustDelta = switch (classification) {
            case ADEQUATE -> 10;
            case RISKY -> -5;
            case INADEQUATE -> -12;
        };
        int victimRiskDelta = switch (classification) {
            case ADEQUATE -> -5;
            case RISKY -> 8;
            case INADEQUATE -> 12;
        };

        boolean revictimizationRisk = decision.isProhibitedConduct();
        if (revictimizationRisk) {
            trustDelta -= 20;
            victimRiskDelta += 15;
        }

        SimulationNodeEntity targetNode = decision.getTargetNode();
        boolean institutionalRoute = classification == DecisionClassification.ADEQUATE
                && targetNode != null
                && (containsRouteKeyword(targetNode.getNodeKey()) || containsRouteKeyword(targetNode.getTitle()));

        return new DecisionEffects(
                scoreDelta,
                stressDelta,
                trustDelta,
                victimRiskDelta,
                institutionalRoute,
                revictimizationRisk
        );
    }

    public void apply(SimulationAttemptEntity attempt, DecisionEffects effects) {
        attempt.setAccumulatedScore(attempt.getAccumulatedScore() + effects.scoreDelta());
        attempt.setStressIndex(clamp(attempt.getStressIndex() + effects.stressDelta(), 0, 100));
        attempt.setUserTrust(clamp(attempt.getUserTrust() + effects.trustDelta(), 0, 100));
        attempt.setVictimRisk(clamp(attempt.getVictimRisk() + effects.victimRiskDelta(), 0, 100));
        if (effects.institutionalRouteActivated()) {
            attempt.setInstitutionalRouteActivated(true);
        }
        if (effects.revictimizationRisk()) {
            attempt.setRevictimizationRisk(true);
        }
    }

    public String formatFeedback(DecisionOptionEntity decision, DecisionEffects effects) {
        if (decision.isProhibitedConduct()) {
            return "Alerta ética: la intervención puede aumentar el riesgo de revictimización. "
                    + decision.getImmediateFeedback();
        }
        return switch (decision.getClassification()) {
            case ADEQUATE -> "Decisión adecuada: fortaleciste la contención profesional. "
                    + decision.getImmediateFeedback();
            case RISKY -> "Decisión con riesgo: revisa las implicaciones clínicas y procedimentales. "
                    + decision.getImmediateFeedback();
            case INADEQUATE -> "Decisión inadecuada: la ruta elegida puede aumentar el riesgo para la persona. "
                    + decision.getImmediateFeedback();
        };
    }

    private static boolean containsRouteKeyword(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("ruta")
                || normalized.contains("proteccion")
                || normalized.contains("protección")
                || normalized.contains("institucional");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record DecisionEffects(
            int scoreDelta,
            int stressDelta,
            int trustDelta,
            int victimRiskDelta,
            boolean institutionalRouteActivated,
            boolean revictimizationRisk
    ) {
    }
}
