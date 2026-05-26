package com.psychosim.simulation.domain.model;

public record ScoreRule(
        int scoreDelta,
        int stressDelta,
        int prohibitedPenalty
) {
    public DecisionConsequence evaluate(boolean prohibitedConduct) {
        int finalScoreDelta = prohibitedConduct ? scoreDelta + prohibitedPenalty : scoreDelta;
        int finalStressDelta = prohibitedConduct ? stressDelta + 40 : stressDelta;
        return new DecisionConsequence(finalScoreDelta, finalStressDelta, prohibitedConduct);
    }

    public static ScoreRule adequate() {
        return new ScoreRule(10, -5, -50);
    }

    public static ScoreRule risky() {
        return new ScoreRule(0, 15, -50);
    }

    public static ScoreRule inadequate() {
        return new ScoreRule(-10, 25, -50);
    }
}
