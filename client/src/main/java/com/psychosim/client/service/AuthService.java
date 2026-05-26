package com.psychosim.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.psychosim.client.model.UserSession;

import java.util.Map;
import java.util.prefs.Preferences;

public class AuthService {

    private static final String PREF_TOKEN = "jwt_token";
    private static AuthService instance;
    private final Preferences prefs = Preferences.userNodeForPackage(AuthService.class);

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public boolean login(String email, String password) {
        try {
            JsonNode res = ApiService.getInstance().post("/api/auth/login",
                    Map.of("email", email, "password", password));
            if (res.path("success").asBoolean()) {
                JsonNode data = res.path("data");
                String token = data.path("token").asText();
                JsonNode user = data.path("user");
                UserSession.getInstance().init(
                        token,
                        user.path("id").asLong(),
                        user.path("email").asText(),
                        user.path("role").asText(),
                        user.path("nombre").asText() + " " + user.path("apellido").asText()
                );
                prefs.put(PREF_TOKEN, token);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void logout() {
        UserSession.getInstance().clear();
        prefs.remove(PREF_TOKEN);
    }

    public boolean tryAutoLogin() {
        String savedToken = prefs.get(PREF_TOKEN, null);
        if (savedToken == null) return false;
        // Token guardado — intentar /me para validar
        try {
            UserSession.getInstance().init(savedToken, null, null, null, null);
            JsonNode res = ApiService.getInstance().get("/api/auth/me");
            if (res.path("success").asBoolean()) {
                JsonNode user = res.path("data");
                UserSession.getInstance().init(
                        savedToken,
                        user.path("id").asLong(),
                        user.path("email").asText(),
                        user.path("role").asText(),
                        user.path("nombre").asText() + " " + user.path("apellido").asText()
                );
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        UserSession.getInstance().clear();
        return false;
    }
}
