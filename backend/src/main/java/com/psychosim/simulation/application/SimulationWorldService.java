package com.psychosim.simulation.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRole;
import com.psychosim.simulation.domain.model.AttemptEventType;
import com.psychosim.simulation.domain.model.AttemptStatus;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventEntity;
import com.psychosim.simulation.infrastructure.persistence.AttemptEventJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.AttemptWorldStateEntity;
import com.psychosim.simulation.infrastructure.persistence.AttemptWorldStateJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.ClinicalToolEntity;
import com.psychosim.simulation.infrastructure.persistence.ClinicalToolJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.CollisionZoneEntity;
import com.psychosim.simulation.infrastructure.persistence.CollisionZoneJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DialogueChoiceEntity;
import com.psychosim.simulation.infrastructure.persistence.DialogueChoiceJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DialogueLineEntity;
import com.psychosim.simulation.infrastructure.persistence.DialogueLineJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DialogueTreeEntity;
import com.psychosim.simulation.infrastructure.persistence.DialogueTreeJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.MapObjectEntity;
import com.psychosim.simulation.infrastructure.persistence.MapObjectJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SceneMapEntity;
import com.psychosim.simulation.infrastructure.persistence.SceneMapJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationAttemptJpaRepository;
import com.psychosim.simulation.web.SimulationDtos.ClinicalToolState;
import com.psychosim.simulation.web.SimulationDtos.CollisionZoneState;
import com.psychosim.simulation.web.SimulationDtos.DialogueChoiceState;
import com.psychosim.simulation.web.SimulationDtos.DialogueLineState;
import com.psychosim.simulation.web.SimulationDtos.DialogueState;
import com.psychosim.simulation.web.SimulationDtos.InteractionResult;
import com.psychosim.simulation.web.SimulationDtos.MapObjectState;
import com.psychosim.simulation.web.SimulationDtos.PlayerState;
import com.psychosim.simulation.web.SimulationDtos.SceneMap;
import com.psychosim.simulation.web.SimulationDtos.ToolUseResult;
import com.psychosim.simulation.web.SimulationDtos.WorldState;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimulationWorldService {
    private final SimulationAttemptJpaRepository attemptRepository;
    private final SceneMapJpaRepository sceneMapRepository;
    private final MapObjectJpaRepository mapObjectRepository;
    private final CollisionZoneJpaRepository collisionZoneRepository;
    private final DialogueTreeJpaRepository dialogueTreeRepository;
    private final DialogueLineJpaRepository dialogueLineRepository;
    private final DialogueChoiceJpaRepository dialogueChoiceRepository;
    private final ClinicalToolJpaRepository clinicalToolRepository;
    private final AttemptWorldStateJpaRepository worldStateRepository;
    private final AttemptEventJpaRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorldState getWorld(UUID attemptId, String attemptToken, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        AttemptWorldStateEntity worldState = requireWorldState(attempt);
        return toWorldState(attempt, worldState);
    }

    @Transactional
    public WorldState updatePosition(UUID attemptId, String attemptToken, int playerX, int playerY, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        requireInProgress(attempt);
        AttemptWorldStateEntity worldState = requireWorldState(attempt);
        worldState.setPlayerX(clamp(playerX, 0, 960));
        worldState.setPlayerY(clamp(playerY, 0, 540));
        worldState.setUpdatedAt(LocalDateTime.now());
        worldStateRepository.save(worldState);
        saveEvent(attempt, AttemptEventType.WORLD_POSITION_UPDATED, "Posicion de jugador actualizada");
        return toWorldState(attempt, worldState);
    }

    @Transactional
    public InteractionResult openInteraction(UUID attemptId, String attemptToken, String interactionKey, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        requireInProgress(attempt);
        AttemptWorldStateEntity worldState = requireWorldState(attempt);
        MapObjectEntity mapObject = mapObjectRepository
                .findBySceneMapIdAndObjectKey(worldState.getSceneMap().getId(), interactionKey)
                .orElseThrow(() -> new EntityNotFoundException("Interaccion no encontrada"));

        Set<String> inspected = new LinkedHashSet<>(readStringList(worldState.getInspectedObjectKeysJson()));
        inspected.add(mapObject.getObjectKey());
        worldState.setInspectedObjectKeysJson(writeJson(inspected));

        DialogueState dialogue = toDialogue(mapObject);
        if (dialogue != null) {
            Set<String> viewed = new LinkedHashSet<>(readStringList(worldState.getViewedDialogueKeysJson()));
            viewed.add(dialogue.key());
            worldState.setViewedDialogueKeysJson(writeJson(viewed));
        }

        String unlockedTool = null;
        if (mapObject.getToolCode() != null && !mapObject.getToolCode().isBlank()) {
            Set<String> inventory = new LinkedHashSet<>(readStringList(worldState.getInventoryJson()));
            inventory.add(mapObject.getToolCode());
            worldState.setInventoryJson(writeJson(inventory));
            unlockedTool = mapObject.getToolCode();
        }

        worldState.setUpdatedAt(LocalDateTime.now());
        worldStateRepository.save(worldState);
        saveEvent(attempt, AttemptEventType.MAP_INTERACTION_OPENED, mapObject.getInteractionPrompt());

        return new InteractionResult(
                toWorldState(attempt, worldState),
                toMapObject(mapObject),
                dialogue,
                mapObject.getDecisionOption() == null ? null : mapObject.getDecisionOption().getId(),
                unlockedTool
        );
    }

    @Transactional
    public ToolUseResult useTool(UUID attemptId, String attemptToken, String toolCode, String targetInteractionKey, User actor) {
        SimulationAttemptEntity attempt = requireAttempt(attemptId, attemptToken, actor);
        AttemptWorldStateEntity worldState = requireWorldState(attempt);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("El intento ya no acepta herramientas");
        }
        ClinicalToolEntity tool = clinicalToolRepository.findByCaseVersionIdAndToolCode(attempt.getCaseVersion().getId(), toolCode)
                .orElseThrow(() -> new EntityNotFoundException("Herramienta no encontrada"));

        Set<String> inventory = new LinkedHashSet<>(readStringList(worldState.getInventoryJson()));
        inventory.add(toolCode);
        worldState.setInventoryJson(writeJson(inventory));

        String usageKey = targetInteractionKey == null || targetInteractionKey.isBlank()
                ? toolCode : toolCode + "@" + targetInteractionKey;
        Set<String> used = new LinkedHashSet<>(readStringList(worldState.getUsedToolKeysJson()));
        used.add(usageKey);
        worldState.setUsedToolKeysJson(writeJson(used));
        worldState.setUpdatedAt(LocalDateTime.now());
        worldStateRepository.save(worldState);

        // ── Fase 6: Pertinence evaluation ─────────────────────────────
        boolean pertinent = evaluateToolPertinence(toolCode, targetInteractionKey, worldState);
        int stressDelta = pertinent ? -5 : 3; // Correct tool lowers stress; wrong tool raises it slightly
        String feedbackMessage = generateToolFeedback(tool, targetInteractionKey, pertinent);

        // Apply stress delta to the attempt
        int newStress = Math.max(0, Math.min(100, attempt.getStressIndex() + stressDelta));
        attempt.setStressIndex(newStress);
        attemptRepository.save(attempt);

        saveEvent(attempt, AttemptEventType.TOOL_USED,
                "Herramienta usada: " + toolCode
                + (targetInteractionKey != null ? " sobre " + targetInteractionKey : "")
                + " — " + (pertinent ? "pertinente" : "no pertinente")
                + " (estres " + (stressDelta >= 0 ? "+" : "") + stressDelta + "%)");

        return new ToolUseResult(
                toWorldState(attempt, worldState),
                toolCode,
                targetInteractionKey,
                pertinent,
                stressDelta,
                feedbackMessage
        );
    }

    /**
     * Evaluates whether a clinical tool is pertinent for the target interaction.
     * A tool is pertinent when the target object expects it (via toolCode match
     * or dialogue choice with requiredToolCode).
     */
    private boolean evaluateToolPertinence(String toolCode, String targetKey, AttemptWorldStateEntity worldState) {
        if (targetKey == null || targetKey.isBlank()) return true; // Generic usage is always valid

        // Check if target object expects this tool
        return mapObjectRepository
                .findBySceneMapIdAndObjectKey(worldState.getSceneMap().getId(), targetKey)
                .map(obj -> {
                    // Direct tool code match on object
                    if (toolCode.equals(obj.getToolCode())) return true;
                    // Check dialogue choices requiring this tool
                    return dialogueTreeRepository.findByMapObjectId(obj.getId())
                            .map(tree -> dialogueChoiceRepository
                                    .findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).stream()
                                    .anyMatch(c -> toolCode.equals(c.getRequiredToolCode())))
                            .orElse(false);
                })
                .orElse(false);
    }

    /**
     * Generates contextual feedback for tool usage.
     * Never uses stigmatizing language (per project ethics guidelines).
     */
    private String generateToolFeedback(ClinicalToolEntity tool, String targetKey, boolean pertinent) {
        if (pertinent) {
            return "Has aplicado " + tool.getLabel() + " de forma pertinente"
                    + (targetKey != null ? " en el contexto adecuado" : "")
                    + ". Esto contribuye a una intervencion profesional y etica.";
        }
        return "Has utilizado " + tool.getLabel()
                + ", pero este no es el contexto mas apropiado para esta herramienta. "
                + "Considera revisar que herramienta se ajusta mejor a la situacion actual.";
    }

    @Transactional
    public AttemptWorldStateEntity requireWorldState(SimulationAttemptEntity attempt) {
        SceneMapEntity expectedMap = sceneMapRepository.findByNodeId(attempt.getCurrentNode().getId())
                .orElseThrow(() -> new EntityNotFoundException("La escena no tiene mapa configurado"));
        AttemptWorldStateEntity state = worldStateRepository.findById(attempt.getId()).orElseGet(() -> {
            AttemptWorldStateEntity created = new AttemptWorldStateEntity();
            created.setAttempt(attempt);
            created.setSceneMap(expectedMap);
            created.setPlayerX(expectedMap.getSpawnX());
            created.setPlayerY(expectedMap.getSpawnY());
            return created;
        });
        if (state.getSceneMap() == null || !state.getSceneMap().getId().equals(expectedMap.getId())) {
            state.setSceneMap(expectedMap);
            state.setPlayerX(expectedMap.getSpawnX());
            state.setPlayerY(expectedMap.getSpawnY());
        }
        state.setUpdatedAt(LocalDateTime.now());
        return worldStateRepository.save(state);
    }

    @Transactional
    public WorldState worldForAttempt(SimulationAttemptEntity attempt) {
        return toWorldState(attempt, requireWorldState(attempt));
    }

    private SimulationAttemptEntity requireAttempt(UUID attemptId, String attemptToken, User actor) {
        if (attemptToken == null || attemptToken.isBlank()) {
            throw new IllegalArgumentException("El token del intento es obligatorio");
        }
        SimulationAttemptEntity attempt = attemptRepository.findByIdAndAttemptTokenHash(attemptId, hashToken(attemptToken))
                .orElseThrow(() -> new EntityNotFoundException("Intento no encontrado"));
        boolean owner = attempt.getStudent().getId().equals(actor.getId());
        boolean staff = actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.PROFESOR;
        if (!owner && !staff) {
            throw new AccessDeniedException("No tiene acceso a este intento");
        }
        return attempt;
    }

    private void requireInProgress(SimulationAttemptEntity attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("El intento ya no acepta interacciones en el mundo");
        }
    }

    private WorldState toWorldState(SimulationAttemptEntity attempt, AttemptWorldStateEntity state) {
        SceneMapEntity sceneMap = state.getSceneMap();
        return new WorldState(
                attempt.getId(),
                attempt.getStatus().name(),
                new SceneMap(
                        sceneMap.getId(),
                        sceneMap.getMapKey(),
                        sceneMap.getTitle(),
                        sceneMap.getWidth(),
                        sceneMap.getHeight(),
                        sceneMap.getTheme(),
                        sceneMap.getSpawnX(),
                        sceneMap.getSpawnY(),
                        readMap(sceneMap.getAmbientJson())
                ),
                new PlayerState(state.getPlayerX(), state.getPlayerY()),
                mapObjectRepository.findBySceneMapIdAndVisibleTrueOrderById(sceneMap.getId()).stream()
                        .map(this::toMapObject)
                        .toList(),
                collisionZoneRepository.findBySceneMapIdOrderById(sceneMap.getId()).stream()
                        .map(this::toCollisionZone)
                        .toList(),
                clinicalToolRepository.findByCaseVersionIdAndActiveTrueOrderById(attempt.getCaseVersion().getId()).stream()
                        .map(this::toClinicalTool)
                        .toList(),
                readStringList(state.getInventoryJson()),
                readStringList(state.getInspectedObjectKeysJson()),
                readStringList(state.getViewedDialogueKeysJson()),
                readStringList(state.getUsedToolKeysJson()),
                readMap(state.getFlagsJson())
        );
    }

    private MapObjectState toMapObject(MapObjectEntity mapObject) {
        return new MapObjectState(
                mapObject.getObjectKey(),
                mapObject.getLabel(),
                mapObject.getObjectType(),
                mapObject.getPositionX(),
                mapObject.getPositionY(),
                mapObject.getWidth(),
                mapObject.getHeight(),
                mapObject.getColorHex(),
                mapObject.getIcon(),
                mapObject.getShortCode(),
                mapObject.isCollision(),
                mapObject.getInteractionPrompt(),
                mapObject.getInteractionText(),
                mapObject.getDecisionOption() == null ? null : mapObject.getDecisionOption().getId(),
                mapObject.getToolCode(),
                toDialogue(mapObject)
        );
    }

    private CollisionZoneState toCollisionZone(CollisionZoneEntity zone) {
        return new CollisionZoneState(
                zone.getZoneKey(),
                zone.getLabel(),
                zone.getPositionX(),
                zone.getPositionY(),
                zone.getWidth(),
                zone.getHeight()
        );
    }

    private ClinicalToolState toClinicalTool(ClinicalToolEntity tool) {
        return new ClinicalToolState(
                tool.getToolCode(),
                tool.getLabel(),
                tool.getIcon(),
                tool.getCategory(),
                tool.getDescription(),
                tool.isActive()
        );
    }

    private DialogueState toDialogue(MapObjectEntity mapObject) {
        return dialogueTreeRepository.findByMapObjectId(mapObject.getId())
                .map(tree -> new DialogueState(
                        tree.getTreeKey(),
                        tree.getSpeakerName(),
                        tree.getPortraitKey(),
                        tree.getEmotion(),
                        dialogueLineRepository.findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).stream()
                                .map(this::toDialogueLine)
                                .toList(),
                        dialogueChoiceRepository.findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).stream()
                                .map(this::toDialogueChoice)
                                .toList()
                ))
                .orElse(null);
    }

    private DialogueLineState toDialogueLine(DialogueLineEntity line) {
        return new DialogueLineState(line.getDisplayOrder(), line.getSpeakerName(), line.getText(), line.getEmotion());
    }

    private DialogueChoiceState toDialogueChoice(DialogueChoiceEntity choice) {
        return new DialogueChoiceState(
                choice.getChoiceKey(),
                choice.getText(),
                choice.getDecisionOption() == null ? null : choice.getDecisionOption().getId(),
                choice.getRequiredToolCode(),
                readMap(choice.getEffectJson())
        );
    }

    private void saveEvent(SimulationAttemptEntity attempt, AttemptEventType type, String detail) {
        AttemptEventEntity event = new AttemptEventEntity();
        event.setAttempt(attempt);
        event.setEventType(type);
        event.setNode(attempt.getCurrentNode());
        event.setDetail(detail);
        event.setOccurredAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    private List<String> readStringList(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String writeJson(Iterable<String> values) {
        try {
            List<String> clean = new ArrayList<>();
            values.forEach(clean::add);
            return objectMapper.writeValueAsString(clean);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar el estado del mundo", ex);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proteger el token del intento", ex);
        }
    }
}
