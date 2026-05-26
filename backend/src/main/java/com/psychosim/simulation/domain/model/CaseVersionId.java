package com.psychosim.simulation.domain.model;

public record CaseVersionId(Long value) {
    public CaseVersionId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("El identificador de version de caso debe ser positivo");
        }
    }
}
