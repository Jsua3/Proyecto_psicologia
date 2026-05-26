package com.psychosim.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.psychosim.client.model.UserSession;

import java.util.Map;

public class SessionService {

    private static SessionService instance;

    private SessionService() {}

    public static SessionService getInstance() {
        if (instance == null) instance = new SessionService();
        return instance;
    }

    public Long iniciarSesion(Long casoId) throws Exception {
        JsonNode res = ApiService.getInstance().post("/api/sesiones", Map.of("casoId", casoId));
        if (res.path("success").asBoolean()) {
            Long sesionId = res.path("data").path("id").asLong();
            UserSession.getInstance().setSesionJuegoId(sesionId);
            return sesionId;
        }
        throw new RuntimeException("No se pudo iniciar sesión de juego");
    }

    public JsonNode responder(Long sesionId, Long preguntaId, Long opcionId, int tiempoMs) throws Exception {
        return ApiService.getInstance().post(
                "/api/sesiones/" + sesionId + "/respuesta",
                Map.of("preguntaId", preguntaId, "opcionId", opcionId, "tiempoMs", tiempoMs)
        );
    }

    public JsonNode finalizar(Long sesionId) throws Exception {
        return ApiService.getInstance().put("/api/sesiones/" + sesionId + "/finalizar", null);
    }

    public JsonNode getCasoDetalle(Long casoId) throws Exception {
        return ApiService.getInstance().get("/api/casos/" + casoId);
    }

    public JsonNode listarCasos() throws Exception {
        return ApiService.getInstance().get("/api/casos");
    }
}
