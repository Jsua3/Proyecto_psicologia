package com.psychosim.simulation.domain.model;

public record DecisionConsequence(
        int scoreDelta,
        int stressDelta,
        boolean prohibitedConduct
) {
}
