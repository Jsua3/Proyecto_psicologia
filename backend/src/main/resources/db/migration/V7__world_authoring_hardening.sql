-- ─────────────────────────────────────────────────────────────────────────────
-- V7 · World Authoring Hardening
--     Bloqueo optimista en case_versions (Decisión #2)
--     Versión de esquema del mundo
--     Campos de profundidad y comportamiento en map_objects (z_index, facing,
--     movement_pattern_json, metadata_json)
-- Compatible con PostgreSQL 16 y H2 (perfil test).
-- ─────────────────────────────────────────────────────────────────────────────

-- Bloqueo optimista (@Version JPA) sobre la versión del caso
ALTER TABLE case_versions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Versión del esquema del mundo (actualmente 2 = V6+V7 activo)
ALTER TABLE case_versions ADD COLUMN world_schema_version INTEGER NOT NULL DEFAULT 2;

-- Profundidad de renderizado (z-index) del objeto en el mapa
ALTER TABLE map_objects ADD COLUMN z_index INTEGER NOT NULL DEFAULT 0;

-- Dirección de orientación inicial del sprite: 'down'|'up'|'left'|'right'
ALTER TABLE map_objects ADD COLUMN facing VARCHAR(16) NOT NULL DEFAULT 'down';

-- Patrón de movimiento NPC (JSON): permite NPCs que patrullan, orbitan, etc.
ALTER TABLE map_objects ADD COLUMN movement_pattern_json TEXT NOT NULL DEFAULT '{}';

-- Metadatos de extensión libre (JSON): para futuras propiedades sin migración
ALTER TABLE map_objects ADD COLUMN metadata_json TEXT NOT NULL DEFAULT '{}';
