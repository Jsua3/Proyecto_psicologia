package com.psychosim.simulation.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psychosim.domain.user.User;
import com.psychosim.simulation.domain.model.CasePublicationStatus;
import com.psychosim.simulation.domain.model.DecisionClassification;
import com.psychosim.simulation.domain.model.WorldSnapshot;
import com.psychosim.simulation.domain.service.WorldValidationResult;
import com.psychosim.simulation.domain.service.WorldValidationService;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionEntity;
import com.psychosim.simulation.infrastructure.persistence.CaseVersionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.ClinicalToolEntity;
import com.psychosim.simulation.infrastructure.persistence.ClinicalToolJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.CollisionZoneEntity;
import com.psychosim.simulation.infrastructure.persistence.CollisionZoneJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionEntity;
import com.psychosim.simulation.infrastructure.persistence.DecisionOptionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DialogueChoiceEntity;
import com.psychosim.simulation.infrastructure.persistence.DialogueChoiceJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DialogueLineEntity;
import com.psychosim.simulation.infrastructure.persistence.DialogueLineJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.DialogueTreeEntity;
import com.psychosim.simulation.infrastructure.persistence.DialogueTreeJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.MapObjectEntity;
import com.psychosim.simulation.infrastructure.persistence.MapObjectJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.PublicationChecklistEntity;
import com.psychosim.simulation.infrastructure.persistence.PublicationChecklistJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.RubricCriterionJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.RubricCriterionEntity;
import com.psychosim.simulation.infrastructure.persistence.RubricEntity;
import com.psychosim.simulation.infrastructure.persistence.RubricJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SceneMapEntity;
import com.psychosim.simulation.infrastructure.persistence.SceneMapJpaRepository;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeEntity;
import com.psychosim.simulation.infrastructure.persistence.SimulationNodeJpaRepository;
import com.psychosim.simulation.web.SimulationDtos.CaseEditorView;
import com.psychosim.simulation.web.SimulationDtos.ChecklistUpdateRequest;
import com.psychosim.simulation.web.SimulationDtos.ClinicalToolEditorState;
import com.psychosim.simulation.web.SimulationDtos.DecisionEdgeState;
import com.psychosim.simulation.web.SimulationDtos.DecisionOptionUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.DialogueLineRequest;
import com.psychosim.simulation.web.SimulationDtos.DialogueUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.MapEditorState;
import com.psychosim.simulation.web.SimulationDtos.MapObjectEditorState;
import com.psychosim.simulation.web.SimulationDtos.MapObjectUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.MapUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.NodeEditorState;
import com.psychosim.simulation.web.SimulationDtos.NodeUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.RubricCriterionView;
import com.psychosim.simulation.web.SimulationDtos.RubricEvaluationView;
import com.psychosim.simulation.web.SimulationDtos.SafeExitConfig;
import com.psychosim.simulation.web.SimulationDtos.SceneMapDefinition;
import com.psychosim.simulation.web.SimulationDtos.ToolUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.WorldClinicalTool;
import com.psychosim.simulation.web.SimulationDtos.WorldCollisionZone;
import com.psychosim.simulation.web.SimulationDtos.WorldDefinition;
import com.psychosim.simulation.web.SimulationDtos.WorldDialogueChoice;
import com.psychosim.simulation.web.SimulationDtos.WorldDialogueLine;
import com.psychosim.simulation.web.SimulationDtos.WorldDialogueTree;
import com.psychosim.simulation.web.SimulationDtos.WorldObject;
import com.psychosim.simulation.web.SimulationDtos.WorldSaveRequest;
import com.psychosim.simulation.web.SimulationDtos.WorldValidationIssue;
import com.psychosim.simulation.web.SimulationDtos.WorldValidationState;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.psychosim.simulation.infrastructure.audit.Auditable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SimulationAuthoringService {

    /** Servicio de dominio puro — no lleva @Component, se instancia aquí. */
    private final WorldValidationService worldValidationService = new WorldValidationService();

    private final CaseVersionJpaRepository caseVersionRepository;
    private final SimulationNodeJpaRepository nodeRepository;
    private final DecisionOptionJpaRepository decisionRepository;
    private final SceneMapJpaRepository sceneMapRepository;
    private final MapObjectJpaRepository mapObjectRepository;
    private final CollisionZoneJpaRepository collisionZoneRepository;
    private final DialogueTreeJpaRepository dialogueTreeRepository;
    private final DialogueLineJpaRepository dialogueLineRepository;
    private final DialogueChoiceJpaRepository dialogueChoiceRepository;
    private final ClinicalToolJpaRepository clinicalToolRepository;
    private final RubricJpaRepository rubricRepository;
    private final RubricCriterionJpaRepository criterionRepository;
    private final PublicationChecklistJpaRepository checklistRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CaseEditorView editor(Long caseVersionId) {
        CaseVersionEntity version = requireVersion(caseVersionId);
        List<SimulationNodeEntity> nodes = nodeRepository.findByCaseVersionIdOrderById(caseVersionId);
        List<DecisionOptionEntity> decisions = decisionRepository.findByCaseVersionIdOrderById(caseVersionId);
        List<SceneMapEntity> maps = sceneMapRepository.findByCaseVersionIdOrderById(caseVersionId);
        List<MapObjectEntity> objects = maps.stream()
                .flatMap(map -> mapObjectRepository.findBySceneMapIdOrderById(map.getId()).stream())
                .toList();
        int checklist = checklistRepository.findFirstByCaseVersionIdOrderBySubmittedAtDesc(caseVersionId)
                .map(value -> value.getCompletionRatio().intValue())
                .orElse(0);

        return new CaseEditorView(
                version.getId(),
                version.getSimulationCase().getTitle(),
                version.getSemanticVersion(),
                version.getStatus().name(),
                nodes.stream().map(this::toNodeEditorState).toList(),
                decisions.stream().map(this::toDecisionEdge).toList(),
                maps.stream().map(this::toMapEditorState).toList(),
                objects.stream().map(this::toMapObjectEditor).toList(),
                clinicalToolRepository.findByCaseVersionIdOrderById(caseVersionId).stream()
                        .map(this::toClinicalToolEditor)
                        .toList(),
                rubricRepository.findByCaseVersionIdAndActiveTrueOrderById(caseVersionId).stream()
                        .map(this::toRubricView)
                        .toList(),
                checklist,
                checklist >= 100
        );
    }

    // ─── Node CRUD ───────────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CREATE_NODE", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView createNode(Long caseVersionId, NodeUpsertRequest request) {
        CaseVersionEntity version = ensureDraft(caseVersionId);
        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setCaseVersion(version);
        applyNodeRequest(node, request);
        nodeRepository.save(node);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_UPDATE_NODE", resourceType = "NODE", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView updateNode(Long caseVersionId, Long nodeId, NodeUpsertRequest request) {
        ensureDraft(caseVersionId);
        SimulationNodeEntity node = requireNodeInVersion(nodeId, caseVersionId);
        applyNodeRequest(node, request);
        nodeRepository.save(node);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_DELETE_NODE", resourceType = "NODE", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView deleteNode(Long caseVersionId, Long nodeId) {
        ensureDraft(caseVersionId);
        requireNodeInVersion(nodeId, caseVersionId);
        nodeRepository.deleteById(nodeId);
        return editor(caseVersionId);
    }

    // ─── Decision CRUD ────────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CREATE_DECISION", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView createDecision(Long caseVersionId, DecisionOptionUpsertRequest request) {
        CaseVersionEntity version = ensureDraft(caseVersionId);
        DecisionOptionEntity decision = new DecisionOptionEntity();
        decision.setCaseVersion(version);
        applyDecisionRequest(decision, request);
        decisionRepository.save(decision);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_UPDATE_DECISION", resourceType = "DECISION", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView updateDecision(Long caseVersionId, Long decisionId, DecisionOptionUpsertRequest request) {
        ensureDraft(caseVersionId);
        DecisionOptionEntity decision = decisionRepository.findById(decisionId)
                .orElseThrow(() -> new EntityNotFoundException("Decision no encontrada: " + decisionId));
        if (!decision.getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException("La decisión " + decisionId + " no pertenece a la versión " + caseVersionId);
        }
        applyDecisionRequest(decision, request);
        decisionRepository.save(decision);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_DELETE_DECISION", resourceType = "DECISION", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView deleteDecision(Long caseVersionId, Long decisionId) {
        ensureDraft(caseVersionId);
        DecisionOptionEntity decision = decisionRepository.findById(decisionId)
                .orElseThrow(() -> new EntityNotFoundException("Decision no encontrada: " + decisionId));
        if (!decision.getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException("La decisión " + decisionId + " no pertenece a la versión " + caseVersionId);
        }
        decisionRepository.deleteById(decisionId);
        return editor(caseVersionId);
    }

    // ─── Map CRUD ────────────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CREATE_MAP", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView createMap(Long caseVersionId, MapUpsertRequest request) {
        CaseVersionEntity version = ensureDraft(caseVersionId);
        SimulationNodeEntity node = nodeRepository.findById(request.nodeId())
                .orElseThrow(() -> new EntityNotFoundException("Nodo no encontrado: " + request.nodeId()));
        SceneMapEntity map = new SceneMapEntity();
        map.setCaseVersion(version);
        map.setNode(node);
        applyMapRequest(map, request);
        sceneMapRepository.save(map);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_UPDATE_MAP", resourceType = "MAP", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView updateMap(Long caseVersionId, Long mapId, MapUpsertRequest request) {
        ensureDraft(caseVersionId);
        SceneMapEntity map = requireMapInVersion(mapId, caseVersionId);
        if (request.nodeId() != null) {
            SimulationNodeEntity node = nodeRepository.findById(request.nodeId())
                    .orElseThrow(() -> new EntityNotFoundException("Nodo no encontrado: " + request.nodeId()));
            map.setNode(node);
        }
        applyMapRequest(map, request);
        sceneMapRepository.save(map);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_DELETE_MAP", resourceType = "MAP", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView deleteMap(Long caseVersionId, Long mapId) {
        ensureDraft(caseVersionId);
        requireMapInVersion(mapId, caseVersionId);
        sceneMapRepository.deleteById(mapId);
        return editor(caseVersionId);
    }

    // ─── Map Object CRUD ─────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CREATE_OBJECT", resourceType = "MAP", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView createObject(Long caseVersionId, Long mapId, MapObjectUpsertRequest request) {
        ensureDraft(caseVersionId);
        SceneMapEntity map = requireMapInVersion(mapId, caseVersionId);
        MapObjectEntity object = new MapObjectEntity();
        object.setSceneMap(map);
        applyObjectRequest(object, request);
        mapObjectRepository.save(object);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_UPDATE_OBJECT", resourceType = "OBJECT", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView updateObject(Long caseVersionId, Long objectId, MapObjectUpsertRequest request) {
        ensureDraft(caseVersionId);
        MapObjectEntity object = requireObjectInVersion(objectId, caseVersionId);
        applyObjectRequest(object, request);
        mapObjectRepository.save(object);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_DELETE_OBJECT", resourceType = "OBJECT", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView deleteObject(Long caseVersionId, Long objectId) {
        ensureDraft(caseVersionId);
        requireObjectInVersion(objectId, caseVersionId);
        mapObjectRepository.deleteById(objectId);
        return editor(caseVersionId);
    }

    // ─── Dialogue CRUD ────────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CREATE_DIALOGUE", resourceType = "MAP", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView createDialogue(Long caseVersionId, Long mapId, DialogueUpsertRequest request) {
        ensureDraft(caseVersionId);
        SceneMapEntity map = requireMapInVersion(mapId, caseVersionId);
        MapObjectEntity mapObject = request.mapObjectId() != null
                ? mapObjectRepository.findById(request.mapObjectId()).orElse(null)
                : null;
        DialogueTreeEntity tree = new DialogueTreeEntity();
        tree.setSceneMap(map);
        tree.setMapObject(mapObject);
        applyDialogueRequest(tree, request);
        tree = dialogueTreeRepository.save(tree);
        saveDialogueLines(tree, request.lines());
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_UPDATE_DIALOGUE", resourceType = "DIALOGUE", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView updateDialogue(Long caseVersionId, Long treeId, DialogueUpsertRequest request) {
        ensureDraft(caseVersionId);
        DialogueTreeEntity tree = requireDialogueInVersion(treeId, caseVersionId);
        applyDialogueRequest(tree, request);
        dialogueTreeRepository.save(tree);
        dialogueLineRepository.deleteAllByDialogueTreeId(treeId);
        saveDialogueLines(tree, request.lines());
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_DELETE_DIALOGUE", resourceType = "DIALOGUE", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView deleteDialogue(Long caseVersionId, Long treeId) {
        ensureDraft(caseVersionId);
        requireDialogueInVersion(treeId, caseVersionId);
        dialogueTreeRepository.deleteById(treeId);
        return editor(caseVersionId);
    }

    // ─── Tool CRUD ───────────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CREATE_TOOL", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView createTool(Long caseVersionId, ToolUpsertRequest request) {
        CaseVersionEntity version = ensureDraft(caseVersionId);
        ClinicalToolEntity tool = new ClinicalToolEntity();
        tool.setCaseVersion(version);
        applyToolRequest(tool, request);
        clinicalToolRepository.save(tool);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_UPDATE_TOOL", resourceType = "TOOL", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView updateTool(Long caseVersionId, Long toolId, ToolUpsertRequest request) {
        ensureDraft(caseVersionId);
        ClinicalToolEntity tool = requireToolInVersion(toolId, caseVersionId);
        applyToolRequest(tool, request);
        clinicalToolRepository.save(tool);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_DELETE_TOOL", resourceType = "TOOL", resourceIdParamIndex = 1)
    @Transactional
    public CaseEditorView deleteTool(Long caseVersionId, Long toolId) {
        ensureDraft(caseVersionId);
        requireToolInVersion(toolId, caseVersionId);
        clinicalToolRepository.deleteById(toolId);
        return editor(caseVersionId);
    }

    // ─── Checklist update ────────────────────────────────────────────────────────

    @Auditable(action = "ADMIN_CHECKLIST_UPDATE", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView updateChecklist(Long caseVersionId, ChecklistUpdateRequest request, User actor) {
        ensureDraft(caseVersionId); // checklist solo editable en DRAFT
        int trueCount = boolToInt(request.contentOriginal())
                + boolToInt(request.ethicsReviewed())
                + boolToInt(request.safetyProtocols())
                + boolToInt(request.noStigmatizing())
                + boolToInt(request.triggerWarnings())
                + boolToInt(request.accessibilityOk());
        int ratio = (int) Math.round(trueCount * 100.0 / 6);
        PublicationChecklistEntity checklist = new PublicationChecklistEntity();
        checklist.setCaseVersion(requireVersion(caseVersionId));
        checklist.setSubmittedBy(actor);
        checklist.setCompletionRatio(BigDecimal.valueOf(ratio));
        checklist.setStatus(ratio >= 100 ? "COMPLETE" : "PENDING");
        checklist.setSubmittedAt(LocalDateTime.now());
        if (ratio >= 100) checklist.setCompletedAt(LocalDateTime.now());
        checklistRepository.save(checklist);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_PUBLISH_CASE", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView publish(Long caseVersionId) {
        CaseVersionEntity version = requireVersion(caseVersionId);

        // Guard: solo se puede publicar desde DRAFT
        if (version.getStatus() == CasePublicationStatus.PUBLISHED) {
            throw new IllegalStateException("El caso ya está publicado. Use 'Clonar versión' para crear una nueva.");
        }
        if (version.getStatus() == CasePublicationStatus.ARCHIVED) {
            throw new IllegalStateException("No se puede publicar una versión archivada.");
        }

        // Gate 1: checklist ético y académico al 100%
        int checklist = checklistRepository.findFirstByCaseVersionIdOrderBySubmittedAtDesc(caseVersionId)
                .map(value -> value.getCompletionRatio().intValue())
                .orElse(0);
        if (checklist < 100) {
            throw new IllegalArgumentException("El checklist ético y académico debe estar al 100% antes de publicar.");
        }

        // Gate 2: validación de dominio del mundo (grafo, geometría, ética, límites)
        WorldValidationResult validation = runWorldValidation(version);
        if (validation.hasErrors()) {
            String summary = validation.errors().stream()
                    .map(i -> "[" + i.code() + "] " + i.message())
                    .reduce((a, b) -> a + " | " + b)
                    .orElse("Errores de validación detectados");
            throw new IllegalStateException("El caso no puede publicarse con errores de validación: " + summary);
        }

        version.setStatus(CasePublicationStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        caseVersionRepository.save(version);
        return editor(caseVersionId);
    }

    @Auditable(action = "ADMIN_CLONE_VERSION", resourceType = "CASE_VERSION")
    @Transactional
    public CaseEditorView cloneVersion(Long caseVersionId, User actor) {
        CaseVersionEntity source = requireVersion(caseVersionId);
        CaseVersionEntity clone = new CaseVersionEntity();
        clone.setSimulationCase(source.getSimulationCase());
        clone.setSemanticVersion(nextMinor(source));
        clone.setStatus(CasePublicationStatus.DRAFT);
        clone.setNarrativeContext(source.getNarrativeContext());
        clone.setClonedFrom(source);
        clone.setCreatedBy(actor);
        clone = caseVersionRepository.save(clone);

        Map<Long, SimulationNodeEntity> nodeCopies = cloneNodes(source, clone);
        Map<Long, DecisionOptionEntity> decisionCopies = cloneDecisions(source, clone, nodeCopies);
        Map<Long, SceneMapEntity> mapCopies = cloneMaps(source, clone, nodeCopies);
        cloneCollisionZones(mapCopies);
        Map<Long, MapObjectEntity> objectCopies = cloneMapObjects(mapCopies, decisionCopies);
        cloneDialogues(mapCopies, objectCopies, decisionCopies);
        cloneClinicalTools(source, clone);
        cloneRubrics(source, clone, actor);
        createDraftChecklist(clone, actor);

        return editor(clone.getId());
    }

    private Map<Long, SimulationNodeEntity> cloneNodes(CaseVersionEntity source, CaseVersionEntity clone) {
        Map<Long, SimulationNodeEntity> copies = new HashMap<>();
        nodeRepository.findByCaseVersionIdOrderById(source.getId()).forEach(node -> {
            SimulationNodeEntity copy = new SimulationNodeEntity();
            copy.setCaseVersion(clone);
            copy.setNodeKey(node.getNodeKey());
            copy.setTitle(node.getTitle());
            copy.setNarrative(node.getNarrative());
            copy.setSupportResourcesJson(node.getSupportResourcesJson());
            copy.setRequiredToolsJson(node.getRequiredToolsJson());
            copy.setSensitiveContent(node.isSensitiveContent());
            copy.setSafeExitRequired(node.isSafeExitRequired());
            copy.setWarningMessage(node.getWarningMessage());
            copy.setStartNode(node.isStartNode());
            copy.setTerminalNode(node.isTerminalNode());
            copy.setPositionX(node.getPositionX());
            copy.setPositionY(node.getPositionY());
            copies.put(node.getId(), nodeRepository.save(copy));
        });
        return copies;
    }

    private Map<Long, DecisionOptionEntity> cloneDecisions(
            CaseVersionEntity source,
            CaseVersionEntity clone,
            Map<Long, SimulationNodeEntity> nodeCopies
    ) {
        Map<Long, DecisionOptionEntity> copies = new HashMap<>();
        decisionRepository.findByCaseVersionIdOrderById(source.getId()).forEach(decision -> {
            DecisionOptionEntity copy = new DecisionOptionEntity();
            copy.setCaseVersion(clone);
            copy.setOptionKey(decision.getOptionKey());
            copy.setSourceNode(nodeCopies.get(decision.getSourceNode().getId()));
            copy.setTargetNode(nodeCopies.get(decision.getTargetNode().getId()));
            copy.setText(decision.getText());
            copy.setClassification(decision.getClassification());
            copy.setScoreDelta(decision.getScoreDelta());
            copy.setStressDelta(decision.getStressDelta());
            copy.setProhibitedPenalty(decision.getProhibitedPenalty());
            copy.setImmediateFeedback(decision.getImmediateFeedback());
            copy.setProhibitedConduct(decision.isProhibitedConduct());
            copy.setProhibitionReason(decision.getProhibitionReason());
            copies.put(decision.getId(), decisionRepository.save(copy));
        });
        return copies;
    }

    private Map<Long, SceneMapEntity> cloneMaps(
            CaseVersionEntity source,
            CaseVersionEntity clone,
            Map<Long, SimulationNodeEntity> nodeCopies
    ) {
        Map<Long, SceneMapEntity> copies = new HashMap<>();
        sceneMapRepository.findByCaseVersionIdOrderById(source.getId()).forEach(map -> {
            SceneMapEntity copy = new SceneMapEntity();
            copy.setCaseVersion(clone);
            copy.setNode(nodeCopies.get(map.getNode().getId()));
            copy.setMapKey(map.getMapKey());
            copy.setTitle(map.getTitle());
            copy.setWidth(map.getWidth());
            copy.setHeight(map.getHeight());
            copy.setTheme(map.getTheme());
            copy.setSpawnX(map.getSpawnX());
            copy.setSpawnY(map.getSpawnY());
            copy.setAmbientJson(map.getAmbientJson());
            copies.put(map.getId(), sceneMapRepository.save(copy));
        });
        return copies;
    }

    private void cloneCollisionZones(Map<Long, SceneMapEntity> mapCopies) {
        mapCopies.forEach((sourceMapId, clonedMap) ->
                collisionZoneRepository.findBySceneMapIdOrderById(sourceMapId).forEach(zone -> {
                    CollisionZoneEntity copy = new CollisionZoneEntity();
                    copy.setSceneMap(clonedMap);
                    copy.setZoneKey(zone.getZoneKey());
                    copy.setLabel(zone.getLabel());
                    copy.setPositionX(zone.getPositionX());
                    copy.setPositionY(zone.getPositionY());
                    copy.setWidth(zone.getWidth());
                    copy.setHeight(zone.getHeight());
                    collisionZoneRepository.save(copy);
                })
        );
    }

    private Map<Long, MapObjectEntity> cloneMapObjects(
            Map<Long, SceneMapEntity> mapCopies,
            Map<Long, DecisionOptionEntity> decisionCopies
    ) {
        Map<Long, MapObjectEntity> copies = new HashMap<>();
        mapCopies.forEach((sourceMapId, clonedMap) ->
                mapObjectRepository.findBySceneMapIdOrderById(sourceMapId).forEach(object -> {
                    MapObjectEntity copy = new MapObjectEntity();
                    copy.setSceneMap(clonedMap);
                    copy.setObjectKey(object.getObjectKey());
                    copy.setLabel(object.getLabel());
                    copy.setObjectType(object.getObjectType());
                    copy.setPositionX(object.getPositionX());
                    copy.setPositionY(object.getPositionY());
                    copy.setWidth(object.getWidth());
                    copy.setHeight(object.getHeight());
                    copy.setColorHex(object.getColorHex());
                    copy.setIcon(object.getIcon());
                    copy.setShortCode(object.getShortCode());
                    copy.setCollision(object.isCollision());
                    copy.setVisible(object.isVisible());
                    copy.setInteractionPrompt(object.getInteractionPrompt());
                    copy.setInteractionText(object.getInteractionText());
                    if (object.getDecisionOption() != null) {
                        copy.setDecisionOption(decisionCopies.get(object.getDecisionOption().getId()));
                    }
                    copy.setToolCode(object.getToolCode());
                    copy.setUnlockConditionJson(object.getUnlockConditionJson());
                    copies.put(object.getId(), mapObjectRepository.save(copy));
                })
        );
        return copies;
    }

    private void cloneDialogues(
            Map<Long, SceneMapEntity> mapCopies,
            Map<Long, MapObjectEntity> objectCopies,
            Map<Long, DecisionOptionEntity> decisionCopies
    ) {
        mapCopies.forEach((sourceMapId, clonedMap) ->
                dialogueTreeRepository.findBySceneMapIdOrderById(sourceMapId).forEach(tree -> {
                    DialogueTreeEntity treeCopy = new DialogueTreeEntity();
                    treeCopy.setSceneMap(clonedMap);
                    treeCopy.setMapObject(tree.getMapObject() == null ? null : objectCopies.get(tree.getMapObject().getId()));
                    treeCopy.setTreeKey(tree.getTreeKey());
                    treeCopy.setSpeakerName(tree.getSpeakerName());
                    treeCopy.setPortraitKey(tree.getPortraitKey());
                    treeCopy.setEmotion(tree.getEmotion());
                    treeCopy = dialogueTreeRepository.save(treeCopy);

                    DialogueTreeEntity savedTree = treeCopy;
                    dialogueLineRepository.findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).forEach(line -> {
                        DialogueLineEntity lineCopy = new DialogueLineEntity();
                        lineCopy.setDialogueTree(savedTree);
                        lineCopy.setDisplayOrder(line.getDisplayOrder());
                        lineCopy.setSpeakerName(line.getSpeakerName());
                        lineCopy.setText(line.getText());
                        lineCopy.setEmotion(line.getEmotion());
                        dialogueLineRepository.save(lineCopy);
                    });

                    dialogueChoiceRepository.findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).forEach(choice -> {
                        DialogueChoiceEntity choiceCopy = new DialogueChoiceEntity();
                        choiceCopy.setDialogueTree(savedTree);
                        choiceCopy.setChoiceKey(choice.getChoiceKey());
                        choiceCopy.setText(choice.getText());
                        if (choice.getDecisionOption() != null) {
                            choiceCopy.setDecisionOption(decisionCopies.get(choice.getDecisionOption().getId()));
                        }
                        choiceCopy.setRequiredToolCode(choice.getRequiredToolCode());
                        choiceCopy.setEffectJson(choice.getEffectJson());
                        choiceCopy.setDisplayOrder(choice.getDisplayOrder());
                        dialogueChoiceRepository.save(choiceCopy);
                    });
                })
        );
    }

    private void cloneClinicalTools(CaseVersionEntity source, CaseVersionEntity clone) {
        clinicalToolRepository.findByCaseVersionIdOrderById(source.getId()).forEach(tool -> {
            ClinicalToolEntity copy = new ClinicalToolEntity();
            copy.setCaseVersion(clone);
            copy.setToolCode(tool.getToolCode());
            copy.setLabel(tool.getLabel());
            copy.setIcon(tool.getIcon());
            copy.setCategory(tool.getCategory());
            copy.setDescription(tool.getDescription());
            copy.setActive(tool.isActive());
            clinicalToolRepository.save(copy);
        });
    }

    private void cloneRubrics(CaseVersionEntity source, CaseVersionEntity clone, User actor) {
        rubricRepository.findByCaseVersionIdOrderById(source.getId()).forEach(rubric -> {
            RubricEntity rubricCopy = new RubricEntity();
            rubricCopy.setCaseVersion(clone);
            rubricCopy.setName(rubric.getName());
            rubricCopy.setDescription(rubric.getDescription());
            rubricCopy.setActive(rubric.isActive());
            rubricCopy.setCreatedBy(actor);
            rubricCopy = rubricRepository.save(rubricCopy);

            RubricEntity savedRubric = rubricCopy;
            criterionRepository.findByRubricIdOrderByDisplayOrder(rubric.getId()).forEach(criterion -> {
                RubricCriterionEntity criterionCopy = new RubricCriterionEntity();
                criterionCopy.setRubric(savedRubric);
                criterionCopy.setCompetency(criterion.getCompetency());
                criterionCopy.setTitle(criterion.getTitle());
                criterionCopy.setDescription(criterion.getDescription());
                criterionCopy.setMaxScore(criterion.getMaxScore());
                criterionCopy.setDisplayOrder(criterion.getDisplayOrder());
                criterionRepository.save(criterionCopy);
            });
        });
    }

    private void createDraftChecklist(CaseVersionEntity clone, User actor) {
        PublicationChecklistEntity checklist = new PublicationChecklistEntity();
        checklist.setCaseVersion(clone);
        checklist.setSubmittedBy(actor);
        checklist.setCompletionRatio(BigDecimal.ZERO);
        checklist.setStatus("PENDING");
        checklist.setSubmittedAt(LocalDateTime.now());
        checklistRepository.save(checklist);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private void applyNodeRequest(SimulationNodeEntity node, NodeUpsertRequest request) {
        node.setNodeKey(request.nodeKey());
        node.setTitle(request.title());
        node.setNarrative(request.narrative());
        node.setRequiredToolsJson(writeStringList(request.requiredTools()));
        node.setSupportResourcesJson(writeStringList(request.supportResources()));
        node.setSensitiveContent(request.sensitiveContent());
        node.setSafeExitRequired(request.safeExitRequired());
        node.setWarningMessage(request.warningMessage());
        node.setTerminalNode(request.terminal());
        node.setStartNode(request.startNode());
        node.setPositionX(request.positionX());
        node.setPositionY(request.positionY());
    }

    private void applyDecisionRequest(DecisionOptionEntity decision, DecisionOptionUpsertRequest request) {
        SimulationNodeEntity src = nodeRepository.findById(request.sourceNodeId())
                .orElseThrow(() -> new EntityNotFoundException("Nodo origen: " + request.sourceNodeId()));
        SimulationNodeEntity tgt = nodeRepository.findById(request.targetNodeId())
                .orElseThrow(() -> new EntityNotFoundException("Nodo destino: " + request.targetNodeId()));
        decision.setOptionKey(request.optionKey() != null ? request.optionKey()
                : src.getNodeKey() + "->" + tgt.getNodeKey() + "-" + System.currentTimeMillis());
        decision.setSourceNode(src);
        decision.setTargetNode(tgt);
        decision.setText(request.text());
        decision.setClassification(DecisionClassification.valueOf(request.classification()));
        decision.setProhibitedConduct(request.prohibitedConduct());
        decision.setProhibitionReason(request.prohibitionReason());
        decision.setScoreDelta(request.scoreDelta());
        decision.setStressDelta(request.stressDelta());
        decision.setProhibitedPenalty(request.prohibitedPenalty());
        decision.setImmediateFeedback(request.immediateFeedback() != null ? request.immediateFeedback() : "");
    }

    private void applyMapRequest(SceneMapEntity map, MapUpsertRequest request) {
        map.setMapKey(request.mapKey());
        map.setTitle(request.title());
        map.setWidth(request.width() > 0 ? request.width() : 960);
        map.setHeight(request.height() > 0 ? request.height() : 540);
        map.setTheme(request.theme() != null ? request.theme() : "clinical-soft");
        map.setSpawnX(request.spawnX());
        map.setSpawnY(request.spawnY());
    }

    private void applyObjectRequest(MapObjectEntity object, MapObjectUpsertRequest request) {
        object.setObjectKey(request.objectKey());
        object.setLabel(request.label());
        object.setObjectType(request.objectType());
        object.setPositionX(request.x());
        object.setPositionY(request.y());
        object.setWidth(request.width() > 0 ? request.width() : 48);
        object.setHeight(request.height() > 0 ? request.height() : 48);
        object.setColorHex(request.colorHex() != null ? request.colorHex() : "#4FA3A5");
        object.setIcon(request.icon() != null ? request.icon() : "psychology");
        object.setShortCode(request.shortCode() != null ? request.shortCode() : "ACT");
        object.setCollision(request.collision());
        object.setVisible(request.visible());
        object.setInteractionPrompt(request.interactionPrompt());
        object.setInteractionText(request.interactionText());
        object.setToolCode(request.toolCode());
        if (request.decisionOptionId() != null) {
            decisionRepository.findById(request.decisionOptionId())
                    .ifPresent(object::setDecisionOption);
        } else {
            object.setDecisionOption(null);
        }
    }

    private void applyDialogueRequest(DialogueTreeEntity tree, DialogueUpsertRequest request) {
        tree.setTreeKey(request.treeKey());
        tree.setSpeakerName(request.speakerName());
        tree.setPortraitKey(request.portraitKey());
        tree.setEmotion(request.emotion() != null ? request.emotion() : "neutral");
    }

    private void saveDialogueLines(DialogueTreeEntity tree, List<DialogueLineRequest> lines) {
        if (lines == null) return;
        for (DialogueLineRequest lineReq : lines) {
            DialogueLineEntity line = new DialogueLineEntity();
            line.setDialogueTree(tree);
            line.setDisplayOrder(lineReq.displayOrder());
            line.setSpeakerName(lineReq.speakerName());
            line.setText(lineReq.text());
            line.setEmotion(lineReq.emotion() != null ? lineReq.emotion() : "neutral");
            dialogueLineRepository.save(line);
        }
    }

    private void applyToolRequest(ClinicalToolEntity tool, ToolUpsertRequest request) {
        tool.setToolCode(request.toolCode());
        tool.setLabel(request.label());
        tool.setIcon(request.icon() != null ? request.icon() : "psychology");
        tool.setCategory(request.category() != null ? request.category() : "clinical");
        tool.setDescription(request.description());
        tool.setActive(true);
    }

    private static int boolToInt(boolean value) {
        return value ? 1 : 0;
    }

    private NodeEditorState toNodeEditorState(SimulationNodeEntity node) {
        return new NodeEditorState(
                node.getId(),
                node.getNodeKey(),
                node.getTitle(),
                node.getNarrative(),
                readStringList(node.getSupportResourcesJson()),
                readStringList(node.getRequiredToolsJson()),
                node.isSensitiveContent(),
                node.isSafeExitRequired(),
                node.getWarningMessage(),
                node.isTerminalNode(),
                node.isStartNode(),
                node.getPositionX(),
                node.getPositionY()
        );
    }

    private DecisionEdgeState toDecisionEdge(DecisionOptionEntity d) {
        return new DecisionEdgeState(
                d.getId(),
                d.getOptionKey(),
                d.getSourceNode().getId(),
                d.getSourceNode().getNodeKey(),
                d.getTargetNode().getId(),
                d.getTargetNode().getNodeKey(),
                d.getText(),
                d.getClassification().name(),
                d.isProhibitedConduct(),
                d.getProhibitionReason(),
                d.getScoreDelta(),
                d.getStressDelta(),
                d.getProhibitedPenalty(),
                d.getImmediateFeedback()
        );
    }

    private MapEditorState toMapEditorState(SceneMapEntity map) {
        return new MapEditorState(
                map.getId(),
                map.getMapKey(),
                map.getTitle(),
                map.getWidth(),
                map.getHeight(),
                map.getTheme(),
                map.getSpawnX(),
                map.getSpawnY(),
                map.getNode().getId(),
                map.getNode().getNodeKey()
        );
    }

    private MapObjectEditorState toMapObjectEditor(MapObjectEntity object) {
        return new MapObjectEditorState(
                object.getId(),
                object.getObjectKey(),
                object.getLabel(),
                object.getObjectType(),
                object.getPositionX(),
                object.getPositionY(),
                object.getWidth(),
                object.getHeight(),
                object.getColorHex(),
                object.getIcon(),
                object.getShortCode(),
                object.isCollision(),
                object.isVisible(),
                object.getInteractionPrompt(),
                object.getInteractionText(),
                object.getDecisionOption() == null ? null : object.getDecisionOption().getId(),
                object.getToolCode(),
                object.getSceneMap().getId()
        );
    }

    private ClinicalToolEditorState toClinicalToolEditor(ClinicalToolEntity tool) {
        return new ClinicalToolEditorState(
                tool.getId(),
                tool.getToolCode(),
                tool.getLabel(),
                tool.getIcon(),
                tool.getCategory(),
                tool.getDescription(),
                tool.isActive()
        );
    }

    private CaseVersionEntity requireVersion(Long caseVersionId) {
        return caseVersionRepository.findById(caseVersionId)
                .orElseThrow(() -> new EntityNotFoundException("Version de caso no encontrada"));
    }

    /**
     * Guard de integridad: lanza IllegalStateException si el estado no es DRAFT.
     * Usar en todos los métodos mutadores del editor.
     */
    private CaseVersionEntity ensureDraft(Long caseVersionId) {
        CaseVersionEntity version = requireVersion(caseVersionId);
        if (version.getStatus() != CasePublicationStatus.DRAFT) {
            throw new IllegalStateException(
                    "Solo se puede modificar una versión en estado DRAFT. " +
                    "Estado actual: " + version.getStatus().name() + ". " +
                    "Use 'Clonar versión' para crear una copia editable.");
        }
        return version;
    }

    /** Guard: el nodo debe pertenecer a la versión dada. */
    private SimulationNodeEntity requireNodeInVersion(Long nodeId, Long caseVersionId) {
        SimulationNodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Nodo no encontrado: " + nodeId));
        if (!node.getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException(
                    "El nodo " + nodeId + " no pertenece a la versión " + caseVersionId);
        }
        return node;
    }

    /** Guard: el mapa debe pertenecer a la versión dada. */
    private SceneMapEntity requireMapInVersion(Long mapId, Long caseVersionId) {
        SceneMapEntity map = sceneMapRepository.findById(mapId)
                .orElseThrow(() -> new EntityNotFoundException("Mapa no encontrado: " + mapId));
        if (!map.getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException(
                    "El mapa " + mapId + " no pertenece a la versión " + caseVersionId);
        }
        return map;
    }

    /** Guard: el objeto debe pertenecer a la versión dada (via su mapa). */
    private MapObjectEntity requireObjectInVersion(Long objectId, Long caseVersionId) {
        MapObjectEntity obj = mapObjectRepository.findById(objectId)
                .orElseThrow(() -> new EntityNotFoundException("Objeto no encontrado: " + objectId));
        if (!obj.getSceneMap().getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException(
                    "El objeto " + objectId + " no pertenece a la versión " + caseVersionId);
        }
        return obj;
    }

    /** Guard: la herramienta debe pertenecer a la versión dada. */
    private ClinicalToolEntity requireToolInVersion(Long toolId, Long caseVersionId) {
        ClinicalToolEntity tool = clinicalToolRepository.findById(toolId)
                .orElseThrow(() -> new EntityNotFoundException("Herramienta no encontrada: " + toolId));
        if (!tool.getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException(
                    "La herramienta " + toolId + " no pertenece a la versión " + caseVersionId);
        }
        return tool;
    }

    /** Guard: el árbol de diálogo debe pertenecer a la versión dada (via su mapa). */
    private DialogueTreeEntity requireDialogueInVersion(Long treeId, Long caseVersionId) {
        DialogueTreeEntity tree = dialogueTreeRepository.findById(treeId)
                .orElseThrow(() -> new EntityNotFoundException("Diálogo no encontrado: " + treeId));
        if (!tree.getSceneMap().getCaseVersion().getId().equals(caseVersionId)) {
            throw new IllegalArgumentException(
                    "El diálogo " + treeId + " no pertenece a la versión " + caseVersionId);
        }
        return tree;
    }

    private RubricEvaluationView toRubricView(RubricEntity rubric) {
        return new RubricEvaluationView(
                rubric.getId(),
                rubric.getName(),
                rubric.getDescription(),
                criterionRepository.findByRubricIdOrderByDisplayOrder(rubric.getId()).stream()
                        .map(criterion -> new RubricCriterionView(
                                criterion.getId(),
                                criterion.getCompetency(),
                                criterion.getTitle(),
                                criterion.getDescription(),
                                criterion.getMaxScore(),
                                criterion.getDisplayOrder()
                        ))
                        .toList(),
                List.of(),
                null,
                null
        );
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

    private String writeStringList(List<String> list) {
        try {
            if (list == null) return "[]";
            return objectMapper.writeValueAsString(list);
        } catch (Exception ex) {
            return "[]";
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

    // ─── WorldDefinition v2 (Fase 2) ─────────────────────────────────────────────

    /**
     * Ensambla el WorldDefinition completo de un nodo/mapa para el editor Konva.
     * Si nodeId es null, usa el primer mapa disponible.
     * Incluye el estado de validación del mundo actual.
     */
    @Transactional(readOnly = true)
    public WorldDefinition worldEditor(Long caseVersionId, Long nodeId) {
        CaseVersionEntity version = requireVersion(caseVersionId);

        List<SceneMapEntity> allMaps = sceneMapRepository.findByCaseVersionIdOrderById(caseVersionId);
        SceneMapEntity map;
        if (nodeId != null) {
            map = allMaps.stream()
                    .filter(m -> m.getNode().getId().equals(nodeId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No hay mapa para el nodo " + nodeId + " en versión " + caseVersionId));
        } else {
            map = allMaps.stream().findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Esta versión no tiene mapas aún"));
        }

        List<MapObjectEntity> objects = mapObjectRepository.findBySceneMapIdOrderById(map.getId());
        List<CollisionZoneEntity> collisions = collisionZoneRepository.findBySceneMapIdOrderById(map.getId());
        List<DialogueTreeEntity> dialogueTrees = dialogueTreeRepository.findBySceneMapIdOrderById(map.getId());
        List<ClinicalToolEntity> tools = clinicalToolRepository.findByCaseVersionIdOrderById(caseVersionId);

        boolean hasSafeExit = objects.stream()
                .anyMatch(o -> "EXIT".equalsIgnoreCase(o.getObjectType()));
        String exitKey = objects.stream()
                .filter(o -> "EXIT".equalsIgnoreCase(o.getObjectType()))
                .map(MapObjectEntity::getObjectKey)
                .findFirst().orElse(null);

        WorldValidationResult validation = runWorldValidation(version);

        return new WorldDefinition(
                2,
                caseVersionId,
                version.getVersion(),
                map.getNode().getId(),
                toSceneMapDefinition(map),
                objects.stream().map(this::toWorldObject).toList(),
                collisions.stream().map(this::toWorldCollisionZone).toList(),
                dialogueTrees.stream().map(t -> toWorldDialogueTree(t)).toList(),
                tools.stream().map(this::toWorldClinicalTool).toList(),
                new SafeExitConfig(hasSafeExit, exitKey, List.of()),
                toValidationState(validation)
        );
    }

    /**
     * Devuelve el WorldDefinition de una versión DRAFT sin crear SimulationAttempt.
     * Usa el mismo runtime pero en modo efímero (Decisión #1 / Fase 5).
     */
    @Transactional(readOnly = true)
    public WorldDefinition worldPreview(Long caseVersionId, Long nodeId) {
        // Preview no requiere DRAFT, puede ser cualquier estado
        return worldEditor(caseVersionId, nodeId);
    }

    /**
     * Valida el mundo de una versión de caso y devuelve el estado de validación.
     * No muta ningún dato.
     */
    @Transactional(readOnly = true)
    public WorldValidationState validateWorld(Long caseVersionId) {
        CaseVersionEntity version = requireVersion(caseVersionId);
        return toValidationState(runWorldValidation(version));
    }

    /**
     * Guarda el borrador del mundo desde el editor Konva.
     * Aplica bloqueo optimista: si revision no coincide → 409 Conflict.
     */
    @Auditable(action = "ADMIN_SAVE_WORLD", resourceType = "CASE_VERSION")
    @Transactional
    public WorldDefinition saveWorld(Long caseVersionId, Long nodeId, WorldSaveRequest request) {
        CaseVersionEntity version = ensureDraft(caseVersionId);

        // Bloqueo optimista (Decisión #2)
        if (request.revision() != null && !request.revision().equals(version.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este caso fue modificado en otra sesión. Recarga el editor para continuar.");
        }

        SceneMapEntity map;
        List<SceneMapEntity> allMaps = sceneMapRepository.findByCaseVersionIdOrderById(caseVersionId);
        if (nodeId != null) {
            map = allMaps.stream()
                    .filter(m -> m.getNode().getId().equals(nodeId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("No hay mapa para el nodo " + nodeId));
        } else {
            map = allMaps.stream().findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Esta versión no tiene mapas aún"));
        }

        // Actualizar propiedades del mapa si se proveen
        if (request.map() != null) {
            SceneMapDefinition mapDef = request.map();
            if (mapDef.width() > 0) map.setWidth(mapDef.width());
            if (mapDef.height() > 0) map.setHeight(mapDef.height());
            if (mapDef.spawnX() >= 0) map.setSpawnX(mapDef.spawnX());
            if (mapDef.spawnY() >= 0) map.setSpawnY(mapDef.spawnY());
            if (mapDef.theme() != null) map.setTheme(mapDef.theme());
            sceneMapRepository.save(map);
        }

        // Actualizar objetos del mapa (upsert por objectKey)
        if (request.objects() != null) {
            for (WorldObject wo : request.objects()) {
                MapObjectEntity obj = wo.id() != null
                        ? mapObjectRepository.findById(wo.id()).orElse(new MapObjectEntity())
                        : new MapObjectEntity();
                obj.setSceneMap(map);
                obj.setObjectKey(wo.key());
                obj.setLabel(wo.label());
                obj.setObjectType(wo.type());
                obj.setPositionX(wo.x());
                obj.setPositionY(wo.y());
                obj.setWidth(wo.width() > 0 ? wo.width() : 48);
                obj.setHeight(wo.height() > 0 ? wo.height() : 48);
                obj.setZIndex(wo.zIndex());
                obj.setFacing(wo.facing() != null ? wo.facing() : "down");
                obj.setColorHex(wo.colorHex() != null ? wo.colorHex() : "#4FA3A5");
                obj.setIcon(wo.icon() != null ? wo.icon() : "psychology");
                obj.setShortCode(wo.shortCode() != null ? wo.shortCode() : "ACT");
                obj.setCollision(wo.collision());
                obj.setVisible(wo.visible());
                obj.setInteractionPrompt(wo.interactionPrompt() != null ? wo.interactionPrompt() : "");
                obj.setInteractionText(wo.interactionText() != null ? wo.interactionText() : "");
                obj.setToolCode(wo.toolCode());
                try {
                    obj.setUnlockConditionJson(wo.unlockCondition() != null
                            ? objectMapper.writeValueAsString(wo.unlockCondition()) : "{}");
                    obj.setMovementPatternJson(wo.movementPattern() != null
                            ? objectMapper.writeValueAsString(wo.movementPattern()) : "{}");
                    obj.setMetadataJson(wo.metadata() != null
                            ? objectMapper.writeValueAsString(wo.metadata()) : "{}");
                } catch (Exception e) {
                    obj.setUnlockConditionJson("{}");
                    obj.setMovementPatternJson("{}");
                    obj.setMetadataJson("{}");
                }
                if (wo.decisionOptionId() != null) {
                    decisionRepository.findById(wo.decisionOptionId()).ifPresent(obj::setDecisionOption);
                } else {
                    obj.setDecisionOption(null);
                }
                mapObjectRepository.save(obj);
            }
        }

        // El @Version en CaseVersionEntity se incrementa automáticamente al persistir
        caseVersionRepository.save(version);

        return worldEditor(caseVersionId, nodeId);
    }

    // ─── Helpers de mapeo WorldDefinition ─────────────────────────────────────────

    private WorldValidationResult runWorldValidation(CaseVersionEntity version) {
        Long caseVersionId = version.getId();
        List<SimulationNodeEntity> nodes = nodeRepository.findByCaseVersionIdOrderById(caseVersionId);
        List<DecisionOptionEntity> decisions = decisionRepository.findByCaseVersionIdOrderById(caseVersionId);
        List<SceneMapEntity> maps = sceneMapRepository.findByCaseVersionIdOrderById(caseVersionId);
        List<MapObjectEntity> allObjects = maps.stream()
                .flatMap(m -> mapObjectRepository.findBySceneMapIdOrderById(m.getId()).stream())
                .toList();
        List<CollisionZoneEntity> allCollisions = maps.stream()
                .flatMap(m -> collisionZoneRepository.findBySceneMapIdOrderById(m.getId()).stream())
                .toList();
        List<DialogueTreeEntity> allDialogues = maps.stream()
                .flatMap(m -> dialogueTreeRepository.findBySceneMapIdOrderById(m.getId()).stream())
                .toList();

        boolean hasSafeExit = allObjects.stream()
                .anyMatch(o -> "EXIT".equalsIgnoreCase(o.getObjectType()));

        WorldSnapshot snapshot = new WorldSnapshot(
                caseVersionId,
                nodes.stream().map(n -> new WorldSnapshot.NodeSnap(
                        n.getId(), n.isStartNode(), n.isTerminalNode(),
                        n.getPositionX() != null ? n.getPositionX() : 0,
                        n.getPositionY() != null ? n.getPositionY() : 0)).toList(),
                decisions.stream().map(d -> new WorldSnapshot.DecisionSnap(
                        d.getId(), d.getSourceNode().getId(), d.getTargetNode().getId(),
                        d.isProhibitedConduct(), d.getProhibitionReason())).toList(),
                maps.stream().map(m -> new WorldSnapshot.MapSnap(
                        m.getId(), m.getWidth(), m.getHeight(), m.getSpawnX(), m.getSpawnY())).toList(),
                allObjects.stream().map(o -> new WorldSnapshot.ObjectSnap(
                        o.getId(), o.getSceneMap().getId(),
                        o.getPositionX(), o.getPositionY(), o.getWidth(), o.getHeight(),
                        o.getObjectType())).toList(),
                allCollisions.stream().map(c -> new WorldSnapshot.CollisionSnap(
                        c.getId(), c.getSceneMap().getId(),
                        c.getPositionX(), c.getPositionY(), c.getWidth(), c.getHeight())).toList(),
                allDialogues.stream().map(d -> new WorldSnapshot.DialogueSnap(
                        d.getId(), d.getSceneMap().getId())).toList(),
                hasSafeExit
        );
        return worldValidationService.validate(snapshot);
    }

    private WorldValidationState toValidationState(WorldValidationResult result) {
        return new WorldValidationState(
                result.errors().stream()
                        .map(i -> new WorldValidationIssue(
                                i.severity().name(), i.code(), i.message(), i.entityRef()))
                        .toList(),
                result.warnings().stream()
                        .map(i -> new WorldValidationIssue(
                                i.severity().name(), i.code(), i.message(), i.entityRef()))
                        .toList(),
                result.canPublish()
        );
    }

    private SceneMapDefinition toSceneMapDefinition(SceneMapEntity map) {
        return new SceneMapDefinition(
                map.getId(), map.getMapKey(), map.getTitle(),
                map.getWidth(), map.getHeight(), map.getTheme(),
                map.getSpawnX(), map.getSpawnY(),
                readMap(map.getAmbientJson())
        );
    }

    private WorldObject toWorldObject(MapObjectEntity obj) {
        return new WorldObject(
                obj.getId(), obj.getObjectKey(), obj.getLabel(), obj.getObjectType(),
                obj.getPositionX(), obj.getPositionY(), obj.getWidth(), obj.getHeight(),
                obj.getZIndex(), obj.getFacing(),
                obj.getColorHex(), obj.getIcon(), obj.getShortCode(),
                obj.isCollision(), obj.isVisible(),
                obj.getInteractionPrompt(), obj.getInteractionText(),
                obj.getDecisionOption() == null ? null : obj.getDecisionOption().getId(),
                obj.getToolCode(),
                readMap(obj.getUnlockConditionJson()),
                readMap(obj.getMovementPatternJson()),
                readMap(obj.getMetadataJson())
        );
    }

    private WorldCollisionZone toWorldCollisionZone(CollisionZoneEntity col) {
        return new WorldCollisionZone(
                col.getId(), col.getZoneKey(), col.getLabel(),
                col.getPositionX(), col.getPositionY(), col.getWidth(), col.getHeight()
        );
    }

    private WorldDialogueTree toWorldDialogueTree(DialogueTreeEntity tree) {
        List<WorldDialogueLine> lines = dialogueLineRepository
                .findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).stream()
                .map(l -> new WorldDialogueLine(l.getDisplayOrder(), l.getSpeakerName(), l.getText(), l.getEmotion()))
                .toList();
        List<WorldDialogueChoice> choices = dialogueChoiceRepository
                .findByDialogueTreeIdOrderByDisplayOrder(tree.getId()).stream()
                .map(c -> new WorldDialogueChoice(
                        c.getChoiceKey(), c.getText(),
                        c.getDecisionOption() == null ? null : c.getDecisionOption().getId(),
                        c.getRequiredToolCode(),
                        readMap(c.getEffectJson()),
                        c.getDisplayOrder()
                ))
                .toList();
        return new WorldDialogueTree(
                tree.getId(), tree.getTreeKey(), tree.getSpeakerName(),
                tree.getPortraitKey(), tree.getEmotion(),
                tree.getMapObject() == null ? null : tree.getMapObject().getId(),
                lines, choices
        );
    }

    private WorldClinicalTool toWorldClinicalTool(ClinicalToolEntity tool) {
        return new WorldClinicalTool(
                tool.getId(), tool.getToolCode(), tool.getLabel(),
                tool.getIcon(), tool.getCategory(), tool.getDescription(), tool.isActive()
        );
    }

    private String nextMinor(CaseVersionEntity source) {
        String version = source.getSemanticVersion();
        String[] parts = version.split("\\.");
        if (parts.length != 3) return version + ".1";
        int major = Integer.parseInt(parts[0]);
        int maxMinor = caseVersionRepository.findBySimulationCaseIdOrderByCreatedAtDesc(source.getSimulationCase().getId()).stream()
                .map(CaseVersionEntity::getSemanticVersion)
                .map(value -> value.split("\\."))
                .filter(value -> value.length == 3)
                .filter(value -> parseVersionPart(value[0], -1) == major)
                .mapToInt(value -> parseVersionPart(value[1], 0))
                .max()
                .orElse(parseVersionPart(parts[1], 0));
        return major + "." + (maxMinor + 1) + ".0";
    }

    private int parseVersionPart(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
