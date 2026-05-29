package com.psychosim.domain.reporte;

import com.psychosim.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ReporteService.DashboardDTO>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(reporteService.getDashboard()));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<ApiResponse<ReporteService.ReporteGrupoDTO>> reporGrupo(
            @PathVariable Long grupoId,
            @RequestParam(required = false) Long casoId,
            @RequestParam(required = false) Long caseVersionId) {
        return ResponseEntity.ok(ApiResponse.ok(reporteService.generarReporteGrupo(grupoId, casoId, caseVersionId)));
    }

    @GetMapping("/grupo/{grupoId}/export")
    public ResponseEntity<byte[]> exportarCsv(
            @PathVariable Long grupoId,
            @RequestParam(required = false) Long casoId,
            @RequestParam(required = false) Long caseVersionId) {
        String csv = reporteService.exportarCsv(grupoId, casoId, caseVersionId);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-grupo-" + grupoId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }
}
