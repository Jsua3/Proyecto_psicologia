package com.psychosim.simulation.domain.model;

public record DecisionOptionId(String value) {
    public DecisionOptionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El identificador de la decision es obligatorio");
        }
    }
}
