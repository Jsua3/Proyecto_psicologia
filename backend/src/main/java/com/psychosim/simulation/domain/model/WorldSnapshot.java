package com.psychosim.simulation.domain.model;

import java.util.List;

/**
 * Instantánea plana del mundo de una versión de caso para validación.
 * Sin dependencias de Spring, JPA ni frameworks externos — dominio puro.
 */
public record WorldSnapshot(
        long caseVersionId,
        List<NodeSnap> nodes,
        List<DecisionSnap> decisions,
        List<MapSnap> maps,
        List<ObjectSnap> objects,
        List<CollisionSnap> collisions,
        List<DialogueSnap> dialogues,
        boolean hasSafeExit
) {

    public record NodeSnap(
            long id,
            boolean startNode,
            boolean terminalNode,
            int positionX,
            int positionY
    ) {}

    public record DecisionSnap(
            long id,
            long sourceNodeId,
            long targetNodeId,
            boolean prohibitedConduct,
            String prohibitionReason
    ) {}

    public record MapSnap(
            long id,
            int width,
            int height,
            int spawnX,
            int spawnY
    ) {}

    public record ObjectSnap(
            long id,
            long mapId,
            int positionX,
            int positionY,
            int width,
            int height,
            String objectType
    ) {}

    public record CollisionSnap(
            long id,
            long mapId,
            int positionX,
            int positionY,
            int width,
            int height
    ) {}

    public record DialogueSnap(
            long id,
            long mapId
    ) {}
}
