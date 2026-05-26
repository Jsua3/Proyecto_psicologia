package com.psychosim.client.controller;

import com.psychosim.client.MainApp;
import com.psychosim.client.bridge.JavaFXBridge;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;

public class GameController {

    @FXML private WebView gameWebView;

    private WebEngine engine;
    private JavaFXBridge bridge;
    private Long casoId;
    private Long sesionId;

    @FXML
    public void initialize() {
        engine = gameWebView.getEngine();
        bridge = new JavaFXBridge();

        // Inyectar bridge cuando el DOM esté listo
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                bridge.setJsWindow(window);
                window.setMember("javaBridge", bridge);
                // Iniciar el juego pasando casoId y sesionId a Phaser
                engine.executeScript(String.format(
                        "if(typeof startGame === 'function') startGame(%d, %d);",
                        casoId, sesionId));
            }
        });

        // Manejo de errores JS en consola
        engine.setOnError(e -> System.err.println("WebEngine error: " + e.getMessage()));
    }

    public void initGame(Long casoId, Long sesionId) {
        this.casoId = casoId;
        this.sesionId = sesionId;
        URL gameUrl = getClass().getResource("/game/index.html");
        if (gameUrl != null) {
            engine.load(gameUrl.toExternalForm());
        } else {
            System.err.println("ERROR: no se encontró /game/index.html");
        }
    }

    @FXML
    private void onVolver() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
                Scene scene = new Scene(loader.load(), 1024, 768);
                scene.getStylesheets().add(getClass().getResource("/css/psychosim.css").toExternalForm());
                MainApp.primaryStage.setScene(scene);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
