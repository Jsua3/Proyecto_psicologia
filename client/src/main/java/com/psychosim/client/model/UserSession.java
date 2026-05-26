package com.psychosim.client.model;

public class UserSession {

    private static UserSession instance;

    private String token;
    private Long userId;
    private String email;
    private String role;
    private String nombreCompleto;
    private Long sesionJuegoId;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void init(String token, Long userId, String email, String role, String nombre) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.nombreCompleto = nombre;
    }

    public void clear() {
        token = null; userId = null; email = null; role = null;
        nombreCompleto = null; sesionJuegoId = null;
    }

    public boolean isLoggedIn() { return token != null; }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getNombreCompleto() { return nombreCompleto; }
    public Long getSesionJuegoId() { return sesionJuegoId; }
    public void setSesionJuegoId(Long id) { this.sesionJuegoId = id; }
}
