package com.psychosim.domain.sesion;

import com.psychosim.domain.user.User;
import com.psychosim.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionController {

    private final SesionService sesionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SesionService.SesionResumenDTO>> iniciar(
            @RequestBody IniciarRequest req,
            @AuthenticationPrincipal User usuario) {
        SesionJuego sesion = sesionService.iniciar(req.casoId(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(SesionService.SesionResumenDTO.from(sesion)));
    }

    @PostMapping("/{id}/respuesta")
    public ResponseEntity<ApiResponse<SesionService.RespuestaDTO>> responder(
            @PathVariable Long id,
            @RequestBody RespuestaRequest req,
            @AuthenticationPrincipal User usuario) {
        var resultado = sesionService.responder(id, req.preguntaId(), req.opcionId(),
                req.tiempoMs(), usuario);
        return ResponseEntity.ok(ApiResponse.ok(resultado));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<ApiResponse<SesionService.SesionResumenDTO>> finalizar(
            @PathVariable Long id,
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(ApiResponse.ok(sesionService.finalizar(id, usuario)));
    }

    @GetMapping("/mis-sesiones")
    public ResponseEntity<ApiResponse<List<SesionService.SesionResumenDTO>>> misSesiones(
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(ApiResponse.ok(sesionService.misSesiones(usuario)));
    }

    record IniciarRequest(Long casoId) {}
    record RespuestaRequest(Long preguntaId, Long opcionId, Integer tiempoMs) {}
}
