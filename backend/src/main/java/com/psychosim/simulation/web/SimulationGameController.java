package com.psychosim.simulation.web;

import com.psychosim.domain.user.User;
import com.psychosim.shared.ApiResponse;
import com.psychosim.simulation.application.SimulationGameService;
import com.psychosim.simulation.application.SimulationWorldService;
import com.psychosim.simulation.web.SimulationDtos.AttemptState;
import com.psychosim.simulation.web.SimulationDtos.CaseSummary;
import com.psychosim.simulation.web.SimulationDtos.InteractionRequest;
import com.psychosim.simulation.web.SimulationDtos.InteractionResult;
import com.psychosim.simulation.web.SimulationDtos.ReflectionRequest;
import com.psychosim.simulation.web.SimulationDtos.ReflectionSaved;
import com.psychosim.simulation.web.SimulationDtos.SafeExitRequest;
import com.psychosim.simulation.web.SimulationDtos.SelectDecisionRequest;
import com.psychosim.simulation.web.SimulationDtos.StartAttemptRequest;
import com.psychosim.simulation.web.SimulationDtos.UpdateWorldStateRequest;
import com.psychosim.simulation.web.SimulationDtos.ToolUseResult;
import com.psychosim.simulation.web.SimulationDtos.UseToolRequest;
import com.psychosim.simulation.web.SimulationDtos.WorldState;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationGameController {

    private final SimulationGameService simulationGameService;
    private final SimulationWorldService simulationWorldService;

    @GetMapping("/cases")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<CaseSummary>>> cases() {
        return ResponseEntity.ok(ApiResponse.ok(simulationGameService.listPublishedCases()));
    }

    @PostMapping("/attempts")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<AttemptState>> start(
            @Valid @RequestBody StartAttemptRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Intento iniciado", simulationGameService.startAttempt(request.caseVersionId(), user)));
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<AttemptState>> attempt(
            @PathVariable UUID attemptId,
            @RequestParam String attemptToken,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(simulationGameService.getAttempt(attemptId, attemptToken, user)));
    }

    @PostMapping("/attempts/{attemptId}/decisions")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<AttemptState>> choose(
            @PathVariable UUID attemptId,
            @Valid @RequestBody SelectDecisionRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Decision procesada", simulationGameService.chooseDecision(
                attemptId,
                request.attemptToken(),
                request.decisionOptionId(),
                user
        )));
    }

    @PostMapping("/attempts/{attemptId}/reflections")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<ReflectionSaved>> reflection(
            @PathVariable UUID attemptId,
            @Valid @RequestBody ReflectionRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Bitacora guardada", simulationGameService.saveReflection(
                attemptId,
                request.attemptToken(),
                request.nodeId(),
                request.text(),
                user
        )));
    }

    @GetMapping("/attempts/{attemptId}/world")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<WorldState>> world(
            @PathVariable UUID attemptId,
            @RequestParam String attemptToken,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(simulationWorldService.getWorld(attemptId, attemptToken, user)));
    }

    @PatchMapping("/attempts/{attemptId}/world-state")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<WorldState>> updateWorldState(
            @PathVariable UUID attemptId,
            @Valid @RequestBody UpdateWorldStateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Estado de mundo actualizado", simulationWorldService.updatePosition(
                attemptId,
                request.attemptToken(),
                request.playerX(),
                request.playerY(),
                user
        )));
    }

    @PostMapping("/attempts/{attemptId}/interactions/{interactionKey}")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<InteractionResult>> interact(
            @PathVariable UUID attemptId,
            @PathVariable String interactionKey,
            @Valid @RequestBody InteractionRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Interaccion registrada", simulationWorldService.openInteraction(
                attemptId,
                request.attemptToken(),
                interactionKey,
                user
        )));
    }

    @PostMapping("/attempts/{attemptId}/tools/use")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<ToolUseResult>> useTool(
            @PathVariable UUID attemptId,
            @Valid @RequestBody UseToolRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Herramienta usada", simulationWorldService.useTool(
                attemptId,
                request.attemptToken(),
                request.toolCode(),
                request.targetInteractionKey(),
                user
        )));
    }

    @PostMapping("/attempts/{attemptId}/safe-exit")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','ADMIN')")
    public ResponseEntity<ApiResponse<AttemptState>> safeExit(
            @PathVariable UUID attemptId,
            @Valid @RequestBody SafeExitRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Salida segura registrada", simulationGameService.safeExit(
                attemptId,
                request.attemptToken(),
                request.reason(),
                user
        )));
    }
}
