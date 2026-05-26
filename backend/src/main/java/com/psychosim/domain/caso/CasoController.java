package com.psychosim.domain.caso;

import com.psychosim.domain.user.User;
import com.psychosim.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casos")
@RequiredArgsConstructor
public class CasoController {

    private final CasoService casoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CasoDTO>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(casoService.listarActivos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CasoDTO>> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(casoService.obtenerDetalle(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<CasoDTO>> crear(
            @RequestBody CasoService.CasoRequest req,
            @AuthenticationPrincipal User usuario) {
        CasoDTO created = casoService.crear(req, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Caso creado", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
    public ResponseEntity<ApiResponse<CasoDTO>> actualizar(
            @PathVariable Long id,
            @RequestBody CasoService.CasoRequest req,
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(ApiResponse.ok("Caso actualizado", casoService.actualizar(id, req, usuario)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        casoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Caso eliminado"));
    }
}
