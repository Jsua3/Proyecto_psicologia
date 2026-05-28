package com.psychosim.simulation.web;

import com.psychosim.domain.user.User;
import com.psychosim.shared.ApiResponse;
import com.psychosim.simulation.application.SimulationAuthoringService;
import com.psychosim.simulation.web.SimulationDtos.CaseEditorView;
import com.psychosim.simulation.web.SimulationDtos.ChecklistUpdateRequest;
import com.psychosim.simulation.web.SimulationDtos.DecisionOptionUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.DialogueUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.MapObjectUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.MapUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.NodeUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.ToolUpsertRequest;
import com.psychosim.simulation.web.SimulationDtos.WorldDefinition;
import com.psychosim.simulation.web.SimulationDtos.WorldSaveRequest;
import com.psychosim.simulation.web.SimulationDtos.WorldValidationState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cases")
@RequiredArgsConstructor
public class SimulationAuthoringController {

    private final SimulationAuthoringService simulationAuthoringService;

    // ─── Editor view ────────────────────────────────────────────────────────────

    @GetMapping("/{caseVersionId}/editor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> editor(@PathVariable Long caseVersionId) {
        return ResponseEntity.ok(ApiResponse.ok(simulationAuthoringService.editor(caseVersionId)));
    }

    @PostMapping("/{caseVersionId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> publish(@PathVariable Long caseVersionId) {
        return ResponseEntity.ok(ApiResponse.ok("Caso publicado", simulationAuthoringService.publish(caseVersionId)));
    }

    @PostMapping("/{caseVersionId}/clone-version")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> cloneVersion(
            @PathVariable Long caseVersionId,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Version clonada", simulationAuthoringService.cloneVersion(caseVersionId, actor)));
    }

    // ─── Node CRUD ──────────────────────────────────────────────────────────────

    @PostMapping("/{caseVersionId}/nodes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> createNode(
            @PathVariable Long caseVersionId,
            @RequestBody NodeUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Nodo creado", simulationAuthoringService.createNode(caseVersionId, request)));
    }

    @PutMapping("/{caseVersionId}/nodes/{nodeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateNode(
            @PathVariable Long caseVersionId,
            @PathVariable Long nodeId,
            @RequestBody NodeUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Nodo actualizado", simulationAuthoringService.updateNode(caseVersionId, nodeId, request)));
    }

    @DeleteMapping("/{caseVersionId}/nodes/{nodeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> deleteNode(
            @PathVariable Long caseVersionId,
            @PathVariable Long nodeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Nodo eliminado", simulationAuthoringService.deleteNode(caseVersionId, nodeId)));
    }

    // ─── Decision CRUD ──────────────────────────────────────────────────────────

    @PostMapping("/{caseVersionId}/decisions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> createDecision(
            @PathVariable Long caseVersionId,
            @RequestBody DecisionOptionUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Decision creada", simulationAuthoringService.createDecision(caseVersionId, request)));
    }

    @PutMapping("/{caseVersionId}/decisions/{decisionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateDecision(
            @PathVariable Long caseVersionId,
            @PathVariable Long decisionId,
            @RequestBody DecisionOptionUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Decision actualizada", simulationAuthoringService.updateDecision(caseVersionId, decisionId, request)));
    }

    @DeleteMapping("/{caseVersionId}/decisions/{decisionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> deleteDecision(
            @PathVariable Long caseVersionId,
            @PathVariable Long decisionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Decision eliminada", simulationAuthoringService.deleteDecision(caseVersionId, decisionId)));
    }

    // ─── Map CRUD ───────────────────────────────────────────────────────────────

    @PostMapping("/{caseVersionId}/maps")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> createMap(
            @PathVariable Long caseVersionId,
            @RequestBody MapUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mapa creado", simulationAuthoringService.createMap(caseVersionId, request)));
    }

    @PutMapping("/{caseVersionId}/maps/{mapId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateMap(
            @PathVariable Long caseVersionId,
            @PathVariable Long mapId,
            @RequestBody MapUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mapa actualizado", simulationAuthoringService.updateMap(caseVersionId, mapId, request)));
    }

    @DeleteMapping("/{caseVersionId}/maps/{mapId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> deleteMap(
            @PathVariable Long caseVersionId,
            @PathVariable Long mapId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mapa eliminado", simulationAuthoringService.deleteMap(caseVersionId, mapId)));
    }

    // ─── Map Object CRUD ────────────────────────────────────────────────────────

    @PostMapping("/{caseVersionId}/maps/{mapId}/objects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> createObject(
            @PathVariable Long caseVersionId,
            @PathVariable Long mapId,
            @RequestBody MapObjectUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Objeto creado", simulationAuthoringService.createObject(caseVersionId, mapId, request)));
    }

    @PutMapping("/{caseVersionId}/objects/{objectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateObject(
            @PathVariable Long caseVersionId,
            @PathVariable Long objectId,
            @RequestBody MapObjectUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Objeto actualizado", simulationAuthoringService.updateObject(caseVersionId, objectId, request)));
    }

    @DeleteMapping("/{caseVersionId}/objects/{objectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> deleteObject(
            @PathVariable Long caseVersionId,
            @PathVariable Long objectId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Objeto eliminado", simulationAuthoringService.deleteObject(caseVersionId, objectId)));
    }

    // ─── Dialogue CRUD ──────────────────────────────────────────────────────────

    @PostMapping("/{caseVersionId}/maps/{mapId}/dialogues")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> createDialogue(
            @PathVariable Long caseVersionId,
            @PathVariable Long mapId,
            @RequestBody DialogueUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Dialogo creado", simulationAuthoringService.createDialogue(caseVersionId, mapId, request)));
    }

    @PutMapping("/{caseVersionId}/dialogues/{treeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateDialogue(
            @PathVariable Long caseVersionId,
            @PathVariable Long treeId,
            @RequestBody DialogueUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Dialogo actualizado", simulationAuthoringService.updateDialogue(caseVersionId, treeId, request)));
    }

    @DeleteMapping("/{caseVersionId}/dialogues/{treeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> deleteDialogue(
            @PathVariable Long caseVersionId,
            @PathVariable Long treeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Dialogo eliminado", simulationAuthoringService.deleteDialogue(caseVersionId, treeId)));
    }

    // ─── Tool CRUD ──────────────────────────────────────────────────────────────

    @PostMapping("/{caseVersionId}/tools")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> createTool(
            @PathVariable Long caseVersionId,
            @RequestBody ToolUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Herramienta creada", simulationAuthoringService.createTool(caseVersionId, request)));
    }

    @PutMapping("/{caseVersionId}/tools/{toolId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateTool(
            @PathVariable Long caseVersionId,
            @PathVariable Long toolId,
            @RequestBody ToolUpsertRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Herramienta actualizada", simulationAuthoringService.updateTool(caseVersionId, toolId, request)));
    }

    @DeleteMapping("/{caseVersionId}/tools/{toolId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> deleteTool(
            @PathVariable Long caseVersionId,
            @PathVariable Long toolId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Herramienta eliminada", simulationAuthoringService.deleteTool(caseVersionId, toolId)));
    }

    // ─── Checklist ──────────────────────────────────────────────────────────────

    @PutMapping("/{caseVersionId}/checklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CaseEditorView>> updateChecklist(
            @PathVariable Long caseVersionId,
            @RequestBody ChecklistUpdateRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Checklist actualizado", simulationAuthoringService.updateChecklist(caseVersionId, request, actor)));
    }

    // ─── WorldDefinition v2 (Fase 2) ────────────────────────────────────────────

    /**
     * Ensambla el WorldDefinition completo de un nodo/mapa para el editor Konva.
     * {@code nodeId} es opcional; si se omite, usa el primer mapa disponible.
     */
    @GetMapping("/{caseVersionId}/world-editor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorldDefinition>> worldEditor(
            @PathVariable Long caseVersionId,
            @RequestParam(required = false) Long nodeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(simulationAuthoringService.worldEditor(caseVersionId, nodeId)));
    }

    /**
     * Guarda el borrador del mundo desde el editor Konva.
     * Aplica bloqueo optimista: revision desactualizad → 409 Conflict.
     */
    @PutMapping("/{caseVersionId}/world")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorldDefinition>> saveWorld(
            @PathVariable Long caseVersionId,
            @RequestParam(required = false) Long nodeId,
            @RequestBody WorldSaveRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mundo guardado", simulationAuthoringService.saveWorld(caseVersionId, nodeId, request)));
    }

    /**
     * Ejecuta WorldValidationService y devuelve el estado de validación.
     * No muta datos. Disponible en cualquier estado del caso.
     */
    @PostMapping("/{caseVersionId}/world/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorldValidationState>> validateWorld(
            @PathVariable Long caseVersionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(simulationAuthoringService.validateWorld(caseVersionId)));
    }

    /**
     * Preview efímero del mundo DRAFT (Decisión #1 / Fase 5):
     * devuelve WorldDefinition sin crear SimulationAttempt ni eventos.
     */
    @GetMapping("/{caseVersionId}/world-preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorldDefinition>> worldPreview(
            @PathVariable Long caseVersionId,
            @RequestParam(required = false) Long nodeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(simulationAuthoringService.worldPreview(caseVersionId, nodeId)));
    }
}
