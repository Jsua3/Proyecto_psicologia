package com.psychosim.simulation.domain.model;

public record ContentSafety(
        boolean sensitiveContent,
        boolean safeExitRequired,
        String warningMessage
) {
    public ContentSafety {
        if (sensitiveContent && (warningMessage == null || warningMessage.isBlank())) {
            throw new IllegalArgumentException("Los nodos sensibles requieren advertencia previa");
        }
    }

    public static ContentSafety standard() {
        return new ContentSafety(false, false, null);
    }
}
