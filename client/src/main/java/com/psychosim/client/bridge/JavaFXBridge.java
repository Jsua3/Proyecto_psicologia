package com.psychosim.client.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.psychosim.client.model.UserSession;
import com.psychosim.client.service.SessionService;
import javafx.application.Platform;
import netscape.javascript.JSObject;

/**
 * Expuesto a Phaser 3 vía webEngine.executeScript("window.bridge = ...").
 * Todos los métodos se llaman desde JS; las respuestas se envían de vuelta
 * invocando callbacks JS en el Application Thread.
 */
public class JavaFXBridge {

    private JSObject jsWindow;

    public void setJsWindow(JSObject window) {
        this.jsWindow = window;
    }

    /** Llamado por Phaser cuando el jugador responde una pregunta */
    public void saveAnswer(long preguntaId, long opcionId, int tiempoMs) {
        Long sesionId = UserSession.getInstance().getSesionJuegoId();
        if (sesionId == null) return;

        new Thread(() -> {
            try {
                JsonNode res = SessionService.getInstance().responder(sesionId, preguntaId, opcionId, tiempoMs);
                String json = res.toString();
                Platform.runLater(() -> {
                    if (jsWindow != null) {
                        jsWindow.call("onRespuestaRecibida", json);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /** Llamado por Phaser al terminar todos los escenarios */
    public void finalizarSesion(long sesionId) {
        new Thread(() -> {
            try {
                JsonNode res = SessionService.getInstance().finalizar(sesionId);
                String json = res.toString();
                Platform.runLater(() -> {
                    if (jsWindow != null) {
                        jsWindow.call("onSesionFinalizada", json);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /** Phaser solicita los datos del caso actual */
    public void getCasoData(long casoId) {
        new Thread(() -> {
            try {
                JsonNode res = SessionService.getInstance().getCasoDetalle(casoId);
                String json = res.toString();
                Platform.runLater(() -> {
                    if (jsWindow != null) {
                        jsWindow.call("onCasoDataRecibida", json);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /** Devuelve el ID de sesión actual al JS */
    public long getSesionId() {
        Long id = UserSession.getInstance().getSesionJuegoId();
        return id != null ? id : -1;
    }

    /** Devuelve el nombre del usuario para mostrar en UI */
    public String getNombreUsuario() {
        return UserSession.getInstance().getNombreCompleto();
    }
}
