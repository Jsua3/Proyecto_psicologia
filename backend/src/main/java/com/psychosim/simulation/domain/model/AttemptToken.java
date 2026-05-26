package com.psychosim.simulation.domain.model;

import java.security.SecureRandom;
import java.util.Base64;

public record AttemptToken(String value) {
    private static final SecureRandom RANDOM = new SecureRandom();

    public AttemptToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El token del intento es obligatorio");
        }
    }

    public static AttemptToken create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new AttemptToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
