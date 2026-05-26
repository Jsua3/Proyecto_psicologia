package com.psychosim.domain.caso;

import java.time.LocalDateTime;
import java.util.List;

public record CasoDTO(
        Long id,
        String titulo,
        String descripcion,
        String contextoNarrativo,
        boolean activo,
        LocalDateTime createdAt,
        List<EscenarioDTO> escenarios) {

    static CasoDTO resumen(Caso c) {
        return new CasoDTO(c.getId(), c.getTitulo(), c.getDescripcion(),
                c.getContextoNarrativo(), c.isActivo(), c.getCreatedAt(), null);
    }

    static CasoDTO detalle(Caso c) {
        return new CasoDTO(c.getId(), c.getTitulo(), c.getDescripcion(),
                c.getContextoNarrativo(), c.isActivo(), c.getCreatedAt(),
                c.getEscenarios().stream().map(EscenarioDTO::from).toList());
    }

    record EscenarioDTO(Long id, Integer orden, String nombre, String contexto,
                        String mapaKey, List<PreguntaDTO> preguntas) {
        static EscenarioDTO from(Escenario e) {
            return new EscenarioDTO(e.getId(), e.getOrden(), e.getNombre(),
                    e.getContexto(), e.getMapaKey(),
                    e.getPreguntas().stream().map(PreguntaDTO::from).toList());
        }
    }

    record PreguntaDTO(Long id, Integer orden, String enunciado,
                       Integer puntosCorrecta, List<OpcionDTO> opciones) {
        static PreguntaDTO from(Pregunta p) {
            return new PreguntaDTO(p.getId(), p.getOrden(), p.getEnunciado(),
                    p.getPuntosCorrecta(),
                    // esCorrecta nunca se envía al cliente
                    p.getOpciones().stream().map(OpcionDTO::from).toList());
        }
    }

    record OpcionDTO(Long id, String texto) {
        static OpcionDTO from(Opcion o) {
            return new OpcionDTO(o.getId(), o.getTexto());
        }
    }
}
