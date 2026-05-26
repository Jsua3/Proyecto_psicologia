package com.psychosim.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.psychosim.client.MainApp;
import com.psychosim.client.model.UserSession;
import com.psychosim.client.service.AuthService;
import com.psychosim.client.service.SessionService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LobbyController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<CasoItem> casosListView;
    @FXML private Button jugarButton;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bienvenido, " + UserSession.getInstance().getNombreCompleto());
        jugarButton.setDisable(true);

        casosListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> jugarButton.setDisable(sel == null));

        cargarCasos();
    }

    private void cargarCasos() {
        statusLabel.setText("Cargando casos...");
        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return SessionService.getInstance().listarCasos();
            }
        };
        task.setOnSucceeded(e -> {
            JsonNode res = task.getValue();
            if (res.path("success").asBoolean()) {
                res.path("data").forEach(node -> {
                    casosListView.getItems().add(
                            new CasoItem(node.path("id").asLong(), node.path("titulo").asText())
                    );
                });
                statusLabel.setText("Selecciona un caso para comenzar");
            }
        });
        task.setOnFailed(e -> statusLabel.setText("Error al cargar casos"));
        new Thread(task).start();
    }

    @FXML
    private void onJugar() {
        CasoItem selected = casosListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        jugarButton.setDisable(true);
        statusLabel.setText("Iniciando partida...");

        Task<Long> task = new Task<>() {
            @Override
            protected Long call() throws Exception {
                return SessionService.getInstance().iniciarSesion(selected.id());
            }
        };
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
                    Scene scene = new Scene(loader.load(), 1024, 768);
                    scene.getStylesheets().add(getClass().getResource("/css/psychosim.css").toExternalForm());
                    GameController controller = loader.getController();
                    controller.initGame(selected.id(), task.getValue());
                    MainApp.primaryStage.setScene(scene);
                    MainApp.primaryStage.setFullScreen(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
        task.setOnFailed(e -> {
            statusLabel.setText("Error al iniciar la sesión de juego.");
            jugarButton.setDisable(false);
        });
        new Thread(task).start();
    }

    @FXML
    private void onLogout() {
        AuthService.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/css/psychosim.css").toExternalForm());
            MainApp.primaryStage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    record CasoItem(Long id, String titulo) {
        @Override public String toString() { return titulo; }
    }
}
