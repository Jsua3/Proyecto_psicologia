package com.psychosim.simulation.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SimulationDtos {
    private SimulationDtos() {
    }

    public record CaseSummary(
            Long caseVersionId,
            String code,
            String title,
            String description,
            String semanticVersion,
            int nodeCount,
            String status
    ) {
    }

    public record StartAttemptRequest(Long caseVersionId) {
    }

    public record SelectDecisionRequest(String attemptToken, Long decisionOptionId) {
    }

    public record ReflectionRequest(String attemptToken, Long nodeId, String text) {
    }

    public record SafeExitRequest(String attemptToken, String reason) {
    }

    public record AttemptState(
            UUID attemptId,
            String attemptToken,
            Long caseVersionId,
            String caseTitle,
            String status,
            int accumulatedScore,
            int stressIndex,
            NodeState currentNode,
            Feedback feedback,
            List<String> supportResources
    ) {
    }

    public record NodeState(
            Long id,
            String key,
            String title,
            String narrative,
            List<String> supportResources,
            List<String> requiredTools,
            boolean sensitiveContent,
            boolean safeExitRequired,
            String warningMessage,
            boolean terminal,
            List<DecisionOptionState> options
    ) {
    }

    public record DecisionOptionState(
            Long id,
            String text,
            String classification,
            boolean prohibitedConduct
    ) {
    }

    public record Feedback(
            String classification,
            int scoreDelta,
            int stressDelta,
            boolean prohibitedConduct,
            String message,
            String prohibitionReason
    ) {
    }

    public record ReflectionSaved(Long nodeId, boolean locked) {
    }

    public record WorldState(
            UUID attemptId,
            String status,
            SceneMap map,
            PlayerState player,
            List<MapObjectState> objects,
            List<CollisionZoneState> collisions,
            List<ClinicalToolState> tools,
            List<String> inventory,
            List<String> inspectedObjectKeys,
            List<String> viewedDialogueKeys,
            List<String> usedToolKeys,
            Map<String, Object> flags
    ) {
    }

    public record SceneMap(
            Long id,
            String key,
            String title,
            int width,
            int height,
            String theme,
            int spawnX,
            int spawnY,
            Map<String, Object> ambient
    ) {
    }

    public record PlayerState(int x, int y) {
    }

    public record MapObjectState(
            String key,
            String label,
            String type,
            int x,
            int y,
            int width,
            int height,
            String color,
            String icon,
            String shortCode,
            boolean collision,
            String interactionPrompt,
            String interactionText,
            Long decisionOptionId,
            String toolCode,
            DialogueState dialogue
    ) {
    }

    public record CollisionZoneState(
            String key,
            String label,
            int x,
            int y,
            int width,
            int height
    ) {
    }

    public record ClinicalToolState(
            String code,
            String label,
            String icon,
            String category,
            String description,
            boolean active
    ) {
    }

    public record DialogueState(
            String key,
            String speakerName,
            String portraitKey,
            String emotion,
            List<DialogueLineState> lines,
            List<DialogueChoiceState> choices
    ) {
    }

    public record DialogueLineState(
            int order,
            String speakerName,
            String text,
            String emotion
    ) {
    }

    public record DialogueChoiceState(
            String key,
            String text,
            Long decisionOptionId,
            String requiredToolCode,
            Map<String, Object> effect
    ) {
    }

    public record UpdateWorldStateRequest(
            String attemptToken,
            int playerX,
            int playerY,
            String currentMapKey
    ) {
    }

    public record InteractionRequest(String attemptToken) {
    }

    public record InteractionResult(
            WorldState world,
            MapObjectState interaction,
            DialogueState dialogue,
            Long preparedDecisionOptionId,
            String unlockedToolCode
    ) {
    }

    public record UseToolRequest(
            String attemptToken,
            String toolCode,
            String targetInteractionKey
    ) {
    }

    /**
     * Fase 6 — Contextual feedback when a clinical tool is used.
     * The stress delta and feedback message make the tool usage diegetic.
     */
    public record ToolUseResult(
            WorldState world,
            String toolCode,
            String targetKey,
            boolean pertinent,
            int stressDelta,
            String feedbackMessage
    ) {
    }

    public record AttemptTrace(
            UUID attemptId,
            String studentAlias,
            String caseTitle,
            String status,
            int accumulatedScore,
            int stressIndex,
            List<TraceEvent> events,
            WorldState world,
            List<ReflectionTrace> reflections,
            List<RubricSummary> rubricEvaluations
    ) {
    }

    public record TraceEvent(
            String type,
            String nodeTitle,
            String decisionText,
            int scoreDelta,
            int stressDelta,
            String detail,
            String occurredAt
    ) {
    }

    public record ReflectionTrace(Long nodeId, String nodeTitle, String text, boolean locked) {
    }

    public record RubricSummary(Long id, String rubricName, double totalScore, String comment, String evaluatedAt) {
    }

    public record RecentAttempt(
            UUID attemptId,
            String studentAlias,
            String caseTitle,
            String status,
            int accumulatedScore,
            int stressIndex,
            String startedAt
    ) {
    }

    public record RubricEvaluationView(
            Long rubricId,
            String rubricName,
            String description,
            List<RubricCriterionView> criteria,
            List<CriterionScoreView> scores,
            Double totalScore,
            String comment
    ) {
    }

    public record RubricCriterionView(
            Long id,
            String competency,
            String title,
            String description,
            int maxScore,
            int displayOrder
    ) {
    }

    public record CriterionScoreView(
            Long criterionId,
            double score,
            String comment,
            Map<String, Object> evidence
    ) {
    }

    public record SaveRubricEvaluationRequest(
            Long rubricId,
            String comment,
            List<CriterionScoreInput> scores
    ) {
    }

    public record CriterionScoreInput(Long criterionId, double score, String comment) {
    }

    // ─── Editor-specific states (include DB ids for CRUD) ──────────────────────

    public record NodeEditorState(
            Long id,
            String key,
            String title,
            String narrative,
            List<String> supportResources,
            List<String> requiredTools,
            boolean sensitiveContent,
            boolean safeExitRequired,
            String warningMessage,
            boolean terminal,
            boolean startNode,
            Integer positionX,
            Integer positionY
    ) {
    }

    public record DecisionEdgeState(
            Long id,
            String optionKey,
            Long sourceNodeId,
            String sourceKey,
            Long targetNodeId,
            String targetKey,
            String text,
            String classification,
            boolean prohibitedConduct,
            String prohibitionReason,
            int scoreDelta,
            int stressDelta,
            int prohibitedPenalty,
            String immediateFeedback
    ) {
    }

    public record MapEditorState(
            Long id,
            String key,
            String title,
            int width,
            int height,
            String theme,
            int spawnX,
            int spawnY,
            Long nodeId,
            String nodeKey
    ) {
    }

    public record MapObjectEditorState(
            Long id,
            String key,
            String label,
            String type,
            int x,
            int y,
            int width,
            int height,
            String colorHex,
            String icon,
            String shortCode,
            boolean collision,
            boolean visible,
            String interactionPrompt,
            String interactionText,
            Long decisionOptionId,
            String toolCode,
            Long mapId
    ) {
    }

    public record ClinicalToolEditorState(
            Long id,
            String code,
            String label,
            String icon,
            String category,
            String description,
            boolean active
    ) {
    }

    // ─── Updated CaseEditorView using editor-specific states ────────────────────

    public record CaseEditorView(
            Long caseVersionId,
            String title,
            String semanticVersion,
            String status,
            List<NodeEditorState> nodes,
            List<DecisionEdgeState> decisions,
            List<MapEditorState> maps,
            List<MapObjectEditorState> objects,
            List<ClinicalToolEditorState> tools,
            List<RubricEvaluationView> rubrics,
            int checklistCompletion,
            boolean publishable
    ) {
    }

    // ─── Authoring CRUD request DTOs ────────────────────────────────────────────

    public record NodeUpsertRequest(
            String nodeKey,
            String title,
            String narrative,
            List<String> requiredTools,
            List<String> supportResources,
            boolean sensitiveContent,
            boolean safeExitRequired,
            String warningMessage,
            boolean terminal,
            boolean startNode,
            Integer positionX,
            Integer positionY
    ) {
    }

    public record DecisionOptionUpsertRequest(
            Long sourceNodeId,
            Long targetNodeId,
            String optionKey,
            String text,
            String classification,
            boolean prohibitedConduct,
            String prohibitionReason,
            int scoreDelta,
            int stressDelta,
            int prohibitedPenalty,
            String immediateFeedback
    ) {
    }

    public record MapUpsertRequest(
            Long nodeId,
            String mapKey,
            String title,
            int width,
            int height,
            String theme,
            int spawnX,
            int spawnY
    ) {
    }

    public record MapObjectUpsertRequest(
            String objectKey,
            String label,
            String objectType,
            int x,
            int y,
            int width,
            int height,
            String colorHex,
            String icon,
            String shortCode,
            boolean collision,
            boolean visible,
            String interactionPrompt,
            String interactionText,
            Long decisionOptionId,
            String toolCode
    ) {
    }

    public record DialogueUpsertRequest(
            Long mapObjectId,
            String treeKey,
            String speakerName,
            String portraitKey,
            String emotion,
            List<DialogueLineRequest> lines
    ) {
    }

    public record DialogueLineRequest(
            int displayOrder,
            String speakerName,
            String text,
            String emotion
    ) {
    }

    public record ToolUpsertRequest(
            String toolCode,
            String label,
            String icon,
            String category,
            String description
    ) {
    }

    public record ChecklistUpdateRequest(
            boolean contentOriginal,
            boolean ethicsReviewed,
            boolean safetyProtocols,
            boolean noStigmatizing,
            boolean triggerWarnings,
            boolean accessibilityOk
    ) {
    }

    public record ChecklistView(
            int completionRatio,
            String status,
            boolean publishable,
            boolean contentOriginal,
            boolean ethicsReviewed,
            boolean safetyProtocols,
            boolean noStigmatizing,
            boolean triggerWarnings,
            boolean accessibilityOk
    ) {
    }

    // ─── Validación de mundo (Fase 1) ─────────────────────────────────────────────

    /**
     * Issue individual de validación: ERROR o WARNING, con código máquina,
     * mensaje legible y referencia opcional a la entidad afectada.
     */
    public record WorldValidationIssue(
            String severity,   // "ERROR" | "WARNING"
            String code,       // código máquina p.ej. "GRAPH_CYCLE"
            String message,    // descripción legible
            String entityRef   // nullable p.ej. "map:42", "object:7"
    ) {}

    /**
     * Estado de validación completo devuelto por {@code POST /world/validate}
     * y embebido en {@code WorldDefinition}.
     */
    public record WorldValidationState(
            List<WorldValidationIssue> errors,
            List<WorldValidationIssue> warnings,
            boolean canPublish
    ) {}

    // ─── WorldDefinition v2 — contrato canónico entre editor y juego (Fase 2) ────

    /** Mapa de escena: geometría, spawn y configuración ambiental. */
    public record SceneMapDefinition(
            Long id,
            String key,
            String title,
            int width,
            int height,
            String theme,
            int spawnX,
            int spawnY,
            Map<String, Object> ambient
    ) {}

    /** Objeto del mundo con todos los campos editables y vínculos. */
    public record WorldObject(
            Long id,
            String key,
            String label,
            String type,          // PERSON|PROP|TOOL_TARGET|EXIT|TRIGGER|NOTE|RESOURCE
            int x,
            int y,
            int width,
            int height,
            int zIndex,
            String facing,        // "down"|"up"|"left"|"right"
            String colorHex,
            String icon,
            String shortCode,
            boolean collision,
            boolean visible,
            String interactionPrompt,
            String interactionText,
            Long decisionOptionId,
            String toolCode,
            Map<String, Object> unlockCondition,
            Map<String, Object> movementPattern,
            Map<String, Object> metadata
    ) {}

    /** Zona de colisión con coordenadas en píxeles. */
    public record WorldCollisionZone(
            Long id,
            String key,
            String label,
            int x,
            int y,
            int width,
            int height
    ) {}

    /** Línea de diálogo individual. */
    public record WorldDialogueLine(
            int order,
            String speakerName,
            String text,
            String emotion
    ) {}

    /** Opción de elección en un diálogo. */
    public record WorldDialogueChoice(
            String key,
            String text,
            Long decisionOptionId,
            String requiredToolCode,
            Map<String, Object> effect,
            int displayOrder
    ) {}

    /** Árbol de diálogo completo (para editor y preview). */
    public record WorldDialogueTree(
            Long id,
            String key,
            String speakerName,
            String portraitKey,
            String emotion,
            Long mapObjectId,
            List<WorldDialogueLine> lines,
            List<WorldDialogueChoice> choices
    ) {}

    /** Herramienta clínica disponible en el caso. */
    public record WorldClinicalTool(
            Long id,
            String code,
            String label,
            String icon,
            String category,
            String description,
            boolean active
    ) {}

    /** Configuración de la salida segura del caso. */
    public record SafeExitConfig(
            boolean configured,
            String exitObjectKey,
            List<String> supportResources
    ) {}

    /**
     * Contrato completo del mundo: un único DTO que agrupa mapa, objetos,
     * colisiones, diálogos, herramientas, salida segura y estado de validación.
     * Esto es lo que devuelve GET /world-editor y lo que consume el editor Konva
     * y el runtime Phaser (preview efímero).
     */
    public record WorldDefinition(
            int schemaVersion,
            Long caseVersionId,
            Long revision,
            Long nodeId,
            SceneMapDefinition map,
            List<WorldObject> objects,
            List<WorldCollisionZone> collisionZones,
            List<WorldDialogueTree> dialogues,
            List<WorldClinicalTool> clinicalTools,
            SafeExitConfig safeExit,
            WorldValidationState validation
    ) {}

    /**
     * Request para guardar un borrador del mundo.
     * El campo {@code revision} protege contra ediciones concurrentes
     * (Decisión #2): si difiere del valor en BD → 409 Conflict.
     */
    public record WorldSaveRequest(
            Long revision,
            SceneMapDefinition map,
            List<WorldObject> objects,
            List<WorldCollisionZone> collisionZones,
            List<WorldDialogueTree> dialogues,
            List<WorldClinicalTool> clinicalTools
    ) {}
}
