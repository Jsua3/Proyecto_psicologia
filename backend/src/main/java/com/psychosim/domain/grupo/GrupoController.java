package com.psychosim.domain.grupo;

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
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROFESOR','ADMIN')")
public class GrupoController {

    private final GrupoService grupoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GrupoService.GrupoDTO>>> listar(
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(ApiResponse.ok(grupoService.listarDeProfesor(usuario.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GrupoService.GrupoDTO>> crear(
            @RequestBody CrearGrupoRequest req,
            @AuthenticationPrincipal User usuario) {
        var grupo = grupoService.crear(req.nombre(), req.codigo(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Grupo creado", grupo));
    }

    @PostMapping("/{id}/estudiantes")
    public ResponseEntity<ApiResponse<GrupoService.GrupoDTO>> agregarEstudiante(
            @PathVariable Long id,
            @RequestBody AgregarEstudianteRequest req,
            @AuthenticationPrincipal User usuario) {
        var grupo = grupoService.agregarEstudiante(id, req.email(), usuario);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante agregado", grupo));
    }

    record CrearGrupoRequest(String nombre, String codigo) {}
    record AgregarEstudianteRequest(String email) {}
}
