-- V1: Esquema inicial de PsychoSim

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'PROFESOR', 'ESTUDIANTE')),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE grupos (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    profesor_id BIGINT       NOT NULL REFERENCES users(id),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE grupo_estudiante (
    grupo_id      BIGINT NOT NULL REFERENCES grupos(id),
    estudiante_id BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (grupo_id, estudiante_id)
);

CREATE TABLE casos (
    id                  BIGSERIAL PRIMARY KEY,
    titulo              VARCHAR(200) NOT NULL,
    descripcion         TEXT,
    contexto_narrativo  TEXT,
    activo              BOOLEAN   NOT NULL DEFAULT TRUE,
    created_by          BIGINT    NOT NULL REFERENCES users(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE escenarios (
    id          BIGSERIAL PRIMARY KEY,
    caso_id     BIGINT       NOT NULL REFERENCES casos(id) ON DELETE CASCADE,
    orden       INTEGER      NOT NULL,
    nombre      VARCHAR(200) NOT NULL,
    contexto    TEXT,
    mapa_key    VARCHAR(100) NOT NULL,
    UNIQUE (caso_id, orden)
);

CREATE TABLE preguntas (
    id           BIGSERIAL PRIMARY KEY,
    escenario_id BIGINT  NOT NULL REFERENCES escenarios(id) ON DELETE CASCADE,
    orden        INTEGER NOT NULL,
    enunciado    TEXT    NOT NULL,
    puntos_correcta INTEGER NOT NULL DEFAULT 10,
    UNIQUE (escenario_id, orden)
);

CREATE TABLE opciones (
    id             BIGSERIAL PRIMARY KEY,
    pregunta_id    BIGINT  NOT NULL REFERENCES preguntas(id) ON DELETE CASCADE,
    texto          TEXT    NOT NULL,
    es_correcta    BOOLEAN NOT NULL DEFAULT FALSE,
    feedback_texto TEXT,
    normativa_ref  VARCHAR(300)
);

CREATE TABLE sesiones_juego (
    id             BIGSERIAL PRIMARY KEY,
    estudiante_id  BIGINT    NOT NULL REFERENCES users(id),
    caso_id        BIGINT    NOT NULL REFERENCES casos(id),
    fecha_inicio   TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_fin      TIMESTAMP,
    puntaje_total  INTEGER   NOT NULL DEFAULT 0,
    completado     BOOLEAN   NOT NULL DEFAULT FALSE
);

CREATE TABLE respuestas (
    id                    BIGSERIAL PRIMARY KEY,
    sesion_id             BIGINT  NOT NULL REFERENCES sesiones_juego(id),
    pregunta_id           BIGINT  NOT NULL REFERENCES preguntas(id),
    opcion_id             BIGINT  NOT NULL REFERENCES opciones(id),
    es_correcta           BOOLEAN NOT NULL,
    tiempo_respuesta_ms   INTEGER,
    respondida_en         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices para consultas frecuentes
CREATE INDEX idx_sesiones_estudiante ON sesiones_juego(estudiante_id);
CREATE INDEX idx_sesiones_caso ON sesiones_juego(caso_id);
CREATE INDEX idx_respuestas_sesion ON respuestas(sesion_id);
CREATE INDEX idx_escenarios_caso ON escenarios(caso_id);
CREATE INDEX idx_preguntas_escenario ON preguntas(escenario_id);
CREATE INDEX idx_opciones_pregunta ON opciones(pregunta_id);
