package com.psychosim.client.controller;

import com.psychosim.client.MainApp;
import com.psychosim.client.service.AuthService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator spinner;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        spinner.setVisible(false);
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Complete todos los campos");
            return;
        }

        setLoading(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return AuthService.getInstance().login(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue()) {
                navigateToLobby();
            } else {
                showError("Credenciales inválidas. Verifique su correo y contraseña.");
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showError("Error de conexión con el servidor.");
        });

        new Thread(task).start();
    }

    private void navigateToLobby() {
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

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        spinner.setVisible(loading);
        errorLabel.setVisible(false);
    }
}
