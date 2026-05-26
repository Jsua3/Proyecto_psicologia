package com.psychosim.client.model;

import java.util.List;

public record CasoDTO(
        Long id,
        String titulo,
        String descripcion,
        String contextoNarrativo,
        boolean activo,
        List<EscenarioDTO> escenarios) {

    public record EscenarioDTO(Long id, Integer orden, String nombre,
                               String contexto, String mapaKey,
                               List<PreguntaDTO> preguntas) {}

    public record PreguntaDTO(Long id, Integer orden, String enunciado,
                              Integer puntosCorrecta, List<OpcionDTO> opciones) {}

    public record OpcionDTO(Long id, String texto) {}
}
