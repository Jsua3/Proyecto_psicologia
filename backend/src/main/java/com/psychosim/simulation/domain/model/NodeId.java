package com.psychosim.simulation.domain.model;

public record NodeId(String value) {
    public NodeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El identificador del nodo es obligatorio");
        }
    }
}
