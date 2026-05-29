-- V8: Métricas clínicas del intento para trazabilidad y retroalimentación formativa.

ALTER TABLE simulation_attempts_v2
    ADD COLUMN IF NOT EXISTS victim_risk INTEGER NOT NULL DEFAULT 50,
    ADD COLUMN IF NOT EXISTS user_trust INTEGER NOT NULL DEFAULT 50,
    ADD COLUMN IF NOT EXISTS institutional_route_activated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS revictimization_risk BOOLEAN NOT NULL DEFAULT FALSE;
