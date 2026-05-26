package com.psychosim.domain.caso;

import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CasoService {

    private final CasoRepository casoRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CasoDTO> listarActivos() {
        return casoRepository.findByActivoTrue().stream()
                .map(CasoDTO::resumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public CasoDTO obtenerDetalle(Long id) {
        Caso caso = casoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado: " + id));
        return CasoDTO.detalle(caso);
    }

    @Transactional
    public CasoDTO crear(CasoRequest req, User creador) {
        Caso caso = new Caso();
        caso.setTitulo(req.titulo());
        caso.setDescripcion(req.descripcion());
        caso.setContextoNarrativo(req.contextoNarrativo());
        caso.setCreatedBy(creador);
        mapearEscenarios(caso, req);
        return CasoDTO.resumen(casoRepository.save(caso));
    }

    @Transactional
    public CasoDTO actualizar(Long id, CasoRequest req, User usuario) {
        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado: " + id));

        boolean esAdmin = usuario.getRole().name().equals("ADMIN");
        boolean esCreador = caso.getCreatedBy().getId().equals(usuario.getId());
        if (!esAdmin && !esCreador) {
            throw new AccessDeniedException("No tiene permiso para editar este caso");
        }

        caso.setTitulo(req.titulo());
        caso.setDescripcion(req.descripcion());
        caso.setContextoNarrativo(req.contextoNarrativo());
        caso.getEscenarios().clear();
        mapearEscenarios(caso, req);
        return CasoDTO.resumen(casoRepository.save(caso));
    }

    @Transactional
    public void eliminar(Long id) {
        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Caso no encontrado: " + id));
        caso.setActivo(false);
        casoRepository.save(caso);
    }

    private void mapearEscenarios(Caso caso, CasoRequest req) {
        if (req.escenarios() == null) return;
        for (var eReq : req.escenarios()) {
            Escenario esc = new Escenario();
            esc.setCaso(caso);
            esc.setOrden(eReq.orden());
            esc.setNombre(eReq.nombre());
            esc.setContexto(eReq.contexto());
            esc.setMapaKey(eReq.mapaKey());
            for (var pReq : eReq.preguntas()) {
                Pregunta preg = new Pregunta();
                preg.setEscenario(esc);
                preg.setOrden(pReq.orden());
                preg.setEnunciado(pReq.enunciado());
                preg.setPuntosCorrecta(pReq.puntosCorrecta() != null ? pReq.puntosCorrecta() : 10);
                for (var oReq : pReq.opciones()) {
                    Opcion op = new Opcion();
                    op.setPregunta(preg);
                    op.setTexto(oReq.texto());
                    op.setEsCorrecta(oReq.esCorrecta());
                    op.setFeedbackTexto(oReq.feedbackTexto());
                    op.setNormativaRef(oReq.normativaRef());
                    preg.getOpciones().add(op);
                }
                esc.getPreguntas().add(preg);
            }
            caso.getEscenarios().add(esc);
        }
    }

    // --- Records de request/response ---

    public record CasoRequest(
            String titulo, String descripcion, String contextoNarrativo,
            List<EscenarioRequest> escenarios) {}

    public record EscenarioRequest(
            Integer orden, String nombre, String contexto, String mapaKey,
            List<PreguntaRequest> preguntas) {}

    public record PreguntaRequest(
            Integer orden, String enunciado, Integer puntosCorrecta,
            List<OpcionRequest> opciones) {}

    public record OpcionRequest(
            String texto, boolean esCorrecta, String feedbackTexto, String normativaRef) {}
}
