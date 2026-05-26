-- V2: Usuario administrador por defecto
-- Contraseña: Admin123! (bcrypt hash generado con strength 10)
INSERT INTO users (email, password_hash, nombre, apellido, role, activo, created_at)
VALUES (
    'admin@psychosim.edu.co',
    '$2b$10$nLO9mYIN3iuPaqDg4Ef5quG0zE2jAHr.m.SsVGlVLa/YlEka0FbGK',
    'Admin',
    'Sistema',
    'ADMIN',
    TRUE,
    NOW()
);
