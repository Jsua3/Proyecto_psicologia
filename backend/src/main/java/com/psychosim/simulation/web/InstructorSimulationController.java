package com.psychosim.simulation.web;

import com.psychosim.domain.user.User;
import com.psychosim.shared.ApiResponse;
import com.psychosim.simulation.application.InstructorSimulationService;
import com.psychosim.simulation.web.SimulationDtos.AttemptTrace;
import com.psychosim.simulation.web.SimulationDtos.RecentAttempt;
import com.psychosim.simulation.web.SimulationDtos.RubricEvaluationView;
import com.psychosim.simulation.web.SimulationDtos.SaveRubricEvaluationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
public class InstructorSimulationController {
    private final InstructorSimulationService instructorSimulationService;

    @GetMapping("/attempts/recent")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<RecentAttempt>>> recentAttempts() {
        return ResponseEntity.ok(ApiResponse.ok(instructorSimulationService.recentAttempts()));
    }

    @GetMapping("/attempts/{attemptId}/trace")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<AttemptTrace>> trace(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(ApiResponse.ok(instructorSimulationService.trace(attemptId)));
    }

    @GetMapping("/attempts/{attemptId}/rubric-evaluation")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<RubricEvaluationView>> rubric(@PathVariable UUID attemptId) {
        return ResponseEntity.ok(ApiResponse.ok(instructorSimulationService.rubric(attemptId)));
    }

    @PostMapping("/attempts/{attemptId}/rubric-evaluation")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<RubricEvaluationView>> saveRubric(
            @PathVariable UUID attemptId,
            @Valid @RequestBody SaveRubricEvaluationRequest request,
            @AuthenticationPrincipal User instructor
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Rubrica guardada", instructorSimulationService.saveRubric(attemptId, request, instructor)));
    }
}
