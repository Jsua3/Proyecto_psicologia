package com.psychosim.domain.sesion;

import com.psychosim.domain.caso.CasoRepository;
import com.psychosim.domain.caso.Opcion;
import com.psychosim.domain.caso.Pregunta;
import com.psychosim.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SesionService {

    private final SesionRepository sesionRepository;
    private final CasoRepository casoRepository;

    @Transactional
    public SesionJuego iniciar(Long casoId, User estudiante) {
        var caso = casoRepository.findByIdAndActivoTrue(casoId)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado: " + casoId));
        var sesion = new SesionJuego();
        sesion.setEstudiante(estudiante);
        sesion.setCaso(caso);
        return sesionRepository.save(sesion);
    }

    @Transactional
    public RespuestaDTO responder(Long sesionId, Long preguntaId, Long opcionId,
                                  Integer tiempoMs, User estudiante) {
        SesionJuego sesion = obtenerSesionPropia(sesionId, estudiante);

        // Buscar pregunta en los escenarios del caso
        Pregunta pregunta = sesion.getCaso().getEscenarios().stream()
                .flatMap(e -> e.getPreguntas().stream())
                .filter(p -> p.getId().equals(preguntaId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Pregunta no encontrada: " + preguntaId));

        Opcion opcion = pregunta.getOpciones().stream()
                .filter(o -> o.getId().equals(opcionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Opción no encontrada: " + opcionId));

        var respuesta = new RespuestaEstudiante();
        respuesta.setSesion(sesion);
        respuesta.setPregunta(pregunta);
        respuesta.setOpcion(opcion);
        respuesta.setEsCorrecta(opcion.isEsCorrecta());
        respuesta.setTiempoRespuestaMs(tiempoMs);
        sesion.getRespuestas().add(respuesta);

        int puntosGanados = opcion.isEsCorrecta() ? pregunta.getPuntosCorrecta() : 0;
        sesion.setPuntajeTotal(sesion.getPuntajeTotal() + puntosGanados);
        sesionRepository.save(sesion);

        // Opción correcta para mostrar feedback
        Opcion correcta = pregunta.getOpciones().stream()
                .filter(Opcion::isEsCorrecta)
                .findFirst()
                .orElse(opcion);

        return new RespuestaDTO(
                opcion.isEsCorrecta(),
                opcion.getFeedbackTexto(),
                opcion.getNormativaRef(),
                puntosGanados,
                correcta.getId()
        );
    }

    @Transactional
    public SesionResumenDTO finalizar(Long sesionId, User estudiante) {
        SesionJuego sesion = obtenerSesionPropia(sesionId, estudiante);
        sesion.setCompletado(true);
        sesion.setFechaFin(LocalDateTime.now());
        sesionRepository.save(sesion);
        return SesionResumenDTO.from(sesion);
    }

    @Transactional(readOnly = true)
    public List<SesionResumenDTO> misSesiones(User estudiante) {
        return sesionRepository.findByEstudianteIdOrderByFechaInicioDesc(estudiante.getId())
                .stream().map(SesionResumenDTO::from).toList();
    }

    private SesionJuego obtenerSesionPropia(Long sesionId, User estudiante) {
        SesionJuego sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión no encontrada: " + sesionId));
        if (!sesion.getEstudiante().getId().equals(estudiante.getId())) {
            throw new AccessDeniedException("Esta sesión no pertenece al usuario");
        }
        return sesion;
    }

    public record RespuestaDTO(boolean esCorrecta, String feedback,
                               String normativaRef, int puntosObtenidos,
                               Long opcionCorrectaId) {}

    public record SesionResumenDTO(Long id, Long casoId, String casoTitulo,
                                   String fechaInicio, String fechaFin,
                                   int puntajeTotal, boolean completado) {
        static SesionResumenDTO from(SesionJuego s) {
            return new SesionResumenDTO(s.getId(), s.getCaso().getId(), s.getCaso().getTitulo(),
                    s.getFechaInicio().toString(),
                    s.getFechaFin() != null ? s.getFechaFin().toString() : null,
                    s.getPuntajeTotal(), s.isCompletado());
        }
    }
}
