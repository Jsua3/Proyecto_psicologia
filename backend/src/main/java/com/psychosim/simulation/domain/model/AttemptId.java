package com.psychosim.simulation.domain.model;

import java.util.UUID;

public record AttemptId(UUID value) {
    public AttemptId {
        if (value == null) {
            throw new IllegalArgumentException("El identificador del intento es obligatorio");
        }
    }

    public static AttemptId newId() {
        return new AttemptId(UUID.randomUUID());
    }
}
