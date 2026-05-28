-- V6: mundo configurable para juego serio top-down, dialogos, herramientas y evaluacion.

CREATE TABLE scene_maps (
    id                 BIGSERIAL PRIMARY KEY,
    case_version_id     BIGINT       NOT NULL REFERENCES case_versions(id) ON DELETE CASCADE,
    node_id             BIGINT       NOT NULL REFERENCES simulation_nodes(id) ON DELETE CASCADE,
    map_key             VARCHAR(120) NOT NULL,
    title               VARCHAR(220) NOT NULL,
    width               INTEGER      NOT NULL DEFAULT 960,
    height              INTEGER      NOT NULL DEFAULT 540,
    theme               VARCHAR(80)  NOT NULL DEFAULT 'clinical-soft',
    spawn_x             INTEGER      NOT NULL DEFAULT 145,
    spawn_y             INTEGER      NOT NULL DEFAULT 430,
    ambient_json        TEXT         NOT NULL DEFAULT '{}',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (case_version_id, map_key),
    UNIQUE (node_id)
);

CREATE TABLE map_objects (
    id                   BIGSERIAL PRIMARY KEY,
    scene_map_id          BIGINT       NOT NULL REFERENCES scene_maps(id) ON DELETE CASCADE,
    object_key            VARCHAR(120) NOT NULL,
    label                 VARCHAR(180) NOT NULL,
    object_type           VARCHAR(32)  NOT NULL CHECK (object_type IN ('PERSON', 'OBJECT', 'ROUTE', 'TOOL', 'WARNING', 'EXIT')),
    position_x            INTEGER      NOT NULL,
    position_y            INTEGER      NOT NULL,
    width                 INTEGER      NOT NULL DEFAULT 48,
    height                INTEGER      NOT NULL DEFAULT 48,
    color_hex             VARCHAR(16)  NOT NULL DEFAULT '#4FA3A5',
    icon                  VARCHAR(80)  NOT NULL DEFAULT 'psychology',
    short_code            VARCHAR(12)  NOT NULL DEFAULT 'ACT',
    collision             BOOLEAN      NOT NULL DEFAULT FALSE,
    visible               BOOLEAN      NOT NULL DEFAULT TRUE,
    interaction_prompt    VARCHAR(180) NOT NULL,
    interaction_text      TEXT         NOT NULL,
    decision_option_id    BIGINT       REFERENCES decision_options(id) ON DELETE SET NULL,
    tool_code             VARCHAR(80),
    unlock_condition_json TEXT         NOT NULL DEFAULT '{}',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (scene_map_id, object_key)
);

CREATE TABLE collision_zones (
    id            BIGSERIAL PRIMARY KEY,
    scene_map_id   BIGINT       NOT NULL REFERENCES scene_maps(id) ON DELETE CASCADE,
    zone_key       VARCHAR(120) NOT NULL,
    label          VARCHAR(160),
    position_x     INTEGER      NOT NULL,
    position_y     INTEGER      NOT NULL,
    width          INTEGER      NOT NULL,
    height         INTEGER      NOT NULL,
    UNIQUE (scene_map_id, zone_key)
);

CREATE TABLE dialogue_trees (
    id             BIGSERIAL PRIMARY KEY,
    scene_map_id    BIGINT       NOT NULL REFERENCES scene_maps(id) ON DELETE CASCADE,
    map_object_id   BIGINT       REFERENCES map_objects(id) ON DELETE CASCADE,
    tree_key        VARCHAR(120) NOT NULL,
    speaker_name    VARCHAR(160) NOT NULL,
    portrait_key    VARCHAR(120),
    emotion         VARCHAR(60)  NOT NULL DEFAULT 'neutral',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (scene_map_id, tree_key)
);

CREATE TABLE dialogue_lines (
    id               BIGSERIAL PRIMARY KEY,
    dialogue_tree_id  BIGINT      NOT NULL REFERENCES dialogue_trees(id) ON DELETE CASCADE,
    display_order     INTEGER     NOT NULL DEFAULT 1,
    speaker_name      VARCHAR(160) NOT NULL,
    text              TEXT        NOT NULL,
    emotion           VARCHAR(60) NOT NULL DEFAULT 'neutral',
    UNIQUE (dialogue_tree_id, display_order)
);

CREATE TABLE dialogue_choices (
    id                 BIGSERIAL PRIMARY KEY,
    dialogue_tree_id    BIGINT       NOT NULL REFERENCES dialogue_trees(id) ON DELETE CASCADE,
    choice_key          VARCHAR(120) NOT NULL,
    text                TEXT         NOT NULL,
    decision_option_id  BIGINT       REFERENCES decision_options(id) ON DELETE SET NULL,
    required_tool_code  VARCHAR(80),
    effect_json         TEXT         NOT NULL DEFAULT '{}',
    display_order       INTEGER      NOT NULL DEFAULT 1,
    UNIQUE (dialogue_tree_id, choice_key)
);

CREATE TABLE clinical_tools (
    id              BIGSERIAL PRIMARY KEY,
    case_version_id  BIGINT       REFERENCES case_versions(id) ON DELETE CASCADE,
    tool_code        VARCHAR(80)  NOT NULL,
    label            VARCHAR(120) NOT NULL,
    icon             VARCHAR(80)  NOT NULL DEFAULT 'psychology',
    category         VARCHAR(80)  NOT NULL DEFAULT 'clinical',
    description      TEXT         NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (case_version_id, tool_code)
);

CREATE TABLE attempt_world_states (
    attempt_id                 UUID PRIMARY KEY REFERENCES simulation_attempts_v2(id) ON DELETE CASCADE,
    scene_map_id                BIGINT REFERENCES scene_maps(id),
    player_x                    INTEGER NOT NULL DEFAULT 145,
    player_y                    INTEGER NOT NULL DEFAULT 430,
    inventory_json              TEXT    NOT NULL DEFAULT '[]',
    inspected_object_keys_json  TEXT    NOT NULL DEFAULT '[]',
    viewed_dialogue_keys_json   TEXT    NOT NULL DEFAULT '[]',
    used_tool_keys_json         TEXT    NOT NULL DEFAULT '[]',
    flags_json                  TEXT    NOT NULL DEFAULT '{}',
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE criterion_scores (
    id                    BIGSERIAL PRIMARY KEY,
    rubric_evaluation_id   BIGINT      NOT NULL REFERENCES rubric_evaluations(id) ON DELETE CASCADE,
    rubric_criterion_id    BIGINT      NOT NULL REFERENCES rubric_criteria(id) ON DELETE CASCADE,
    score                 NUMERIC(6,2) NOT NULL DEFAULT 0,
    comment               TEXT,
    evidence_json         TEXT         NOT NULL DEFAULT '{}',
    UNIQUE (rubric_evaluation_id, rubric_criterion_id)
);

CREATE INDEX idx_scene_maps_version ON scene_maps(case_version_id);
CREATE INDEX idx_map_objects_scene ON map_objects(scene_map_id);
CREATE INDEX idx_dialogue_lines_tree ON dialogue_lines(dialogue_tree_id, display_order);
CREATE INDEX idx_attempt_world_scene ON attempt_world_states(scene_map_id);
CREATE INDEX idx_criterion_scores_eval ON criterion_scores(rubric_evaluation_id);

INSERT INTO clinical_tools (case_version_id, tool_code, label, icon, category, description)
SELECT v.id, seed.tool_code, seed.label, seed.icon, seed.category, seed.description
FROM case_versions v
JOIN simulation_cases c ON c.id = v.simulation_case_id
JOIN (
    VALUES
    ('PAP', 'Primeros Auxilios Psicologicos', 'psychology', 'contencion', 'Escucha activa, validacion emocional, respiracion y orientacion inmediata.'),
    ('SPIKES', 'SPIKES', 'forum', 'comunicacion', 'Comunicacion cuidadosa para entregar informacion sensible sin revictimizar.'),
    ('RISK_METER', 'Medidor de riesgo', 'monitoring', 'riesgo', 'Busca armas, amenazas, reincidencia, aislamiento y otros factores de riesgo.'),
    ('SAFETY_ROUTE', 'Ruta de proteccion', 'health_and_safety', 'proteccion', 'Prioriza medidas de proteccion, red de apoyo y articulacion institucional.'),
    ('REFLECTION_JOURNAL', 'Bitacora reflexiva', 'edit_note', 'reflexion', 'Registra razonamiento profesional, hipotesis y criterios eticos.')
) AS seed(tool_code, label, icon, category, description) ON TRUE
WHERE c.code = 'SIM-VBG-001' AND v.semantic_version = '1.0.0'
ON CONFLICT (case_version_id, tool_code) DO UPDATE SET
    label = EXCLUDED.label,
    icon = EXCLUDED.icon,
    category = EXCLUDED.category,
    description = EXCLUDED.description,
    active = TRUE;

INSERT INTO scene_maps (case_version_id, node_id, map_key, title, theme, spawn_x, spawn_y, ambient_json)
SELECT n.case_version_id, n.id, n.node_key, n.title,
       CASE n.node_key
           WHEN 'ruta-proteccion' THEN 'protection-route'
           WHEN 'informe-integral' THEN 'technical-record'
           WHEN 'valoracion-comisaria' THEN 'risk-assessment'
           WHEN 'proteccion-nna' THEN 'child-protection'
           WHEN 'cierre-seguimiento' THEN 'follow-up'
           ELSE 'clinical-soft'
       END,
       145, 430,
       '{"music":"none","mood":"calm-institutional"}'
FROM simulation_nodes n
JOIN case_versions v ON v.id = n.case_version_id
JOIN simulation_cases c ON c.id = v.simulation_case_id
WHERE c.code = 'SIM-VBG-001' AND v.semantic_version = '1.0.0'
ON CONFLICT (node_id) DO UPDATE SET
    title = EXCLUDED.title,
    theme = EXCLUDED.theme,
    updated_at = NOW();

INSERT INTO collision_zones (scene_map_id, zone_key, label, position_x, position_y, width, height)
SELECT sm.id, seed.zone_key, seed.label, seed.position_x, seed.position_y, seed.width, seed.height
FROM scene_maps sm
JOIN (
    VALUES
    ('registro', 'Mesa de registro', 133, 107, 210, 78),
    ('valoracion', 'Area de valoracion', 648, 112, 193, 92),
    ('escucha', 'Espacio de escucha', 374, 320, 230, 88)
) AS seed(zone_key, label, position_x, position_y, width, height) ON TRUE
ON CONFLICT (scene_map_id, zone_key) DO UPDATE SET
    label = EXCLUDED.label,
    position_x = EXCLUDED.position_x,
    position_y = EXCLUDED.position_y,
    width = EXCLUDED.width,
    height = EXCLUDED.height;

INSERT INTO map_objects (
    scene_map_id, object_key, label, object_type, position_x, position_y, color_hex, icon, short_code,
    collision, visible, interaction_prompt, interaction_text, decision_option_id, tool_code
)
SELECT sm.id, seed.object_key, seed.label, seed.object_type, seed.position_x, seed.position_y, seed.color_hex,
       seed.icon, seed.short_code, FALSE, TRUE, seed.prompt, seed.interaction_text, d.id, seed.tool_code
FROM scene_maps sm
JOIN simulation_nodes n ON n.id = sm.node_id
JOIN (
    VALUES
    ('urgencias-crisis', 'escucha-segura', 'Escucha segura', 'PERSON', 502, 292, '#4FA3A5', 'self_improvement', 'PAP', 'Contener y estabilizar', 'Acercate a la consultante, valida su emocion y estabiliza antes de documentar.', 'n1-pap', NULL),
    ('urgencias-crisis', 'cuestionario-prematuro', 'Cuestionario prematuro', 'OBJECT', 246, 226, '#6F8490', 'clinical_notes', 'DOC', 'Registrar sin contener', 'El expediente esta disponible, pero iniciar con un cuestionario extenso puede aumentar angustia.', 'n1-instrumento', NULL),
    ('urgencias-crisis', 'aviso-policial', 'Aviso policial', 'WARNING', 744, 262, '#A99BD6', 'monitoring', 'RISK', 'Valorar riesgo', 'La autoridad puede ser necesaria, pero debe explicarse y valorarse el riesgo inmediato.', 'n1-policia', NULL),
    ('ruta-proteccion', 'ruta-vbg', 'Ruta VBG', 'ROUTE', 850, 322, '#4FA3A5', 'health_and_safety', 'RUTA', 'Activar proteccion', 'Ruta institucional VBG: comisaria, trabajo social, medidas y coordinacion.', 'n2-ruta-vbg', NULL),
    ('ruta-proteccion', 'mediacion-prohibida', 'Mediacion prohibida', 'WARNING', 108, 186, '#A85062', 'warning', 'ALRT', 'Intervenir vinculo', 'Contactar al agresor para mediar es una conducta prohibida y de alto riesgo.', 'n2-mediacion', NULL),
    ('ruta-proteccion', 'psiquiatria-aislada', 'Psiquiatria aislada', 'OBJECT', 654, 96, '#A99BD6', 'medical_services', 'SAL', 'Remitir atencion', 'La remision clinica no reemplaza la ruta de proteccion inmediata.', 'n2-psiquiatria', NULL),
    ('informe-integral', 'informe-integral', 'Informe integral', 'OBJECT', 246, 226, '#4FA3A5', 'clinical_notes', 'DOC', 'Registrar tecnicamente', 'Construye un informe tecnico, integral y no revictimizante.', 'n3-informe-integral', NULL),
    ('informe-integral', 'dsm-aislado', 'DSM aislado', 'OBJECT', 330, 126, '#6F8490', 'clinical_notes', 'DSM', 'Registrar tecnicamente', 'Un diagnostico aislado no cubre riesgo, red de apoyo ni medidas activadas.', 'n3-dsm', NULL),
    ('valoracion-comisaria', 'riesgo-estructurado', 'Riesgo estructurado', 'OBJECT', 744, 262, '#4FA3A5', 'monitoring', 'RISK', 'Valorar riesgo', 'Identifica armas, amenazas, reincidencia y medidas de proteccion proporcionales.', 'n4-riesgo-estructurado', NULL),
    ('valoracion-comisaria', 'contacto-agresor', 'Contacto agresor', 'WARNING', 108, 186, '#A85062', 'warning', 'ALRT', 'Intervenir vinculo', 'Citar al agresor antes de medidas puede escalar el peligro.', 'n4-citar-agresor', NULL),
    ('proteccion-nna', 'ruta-nna', 'Ruta NNA', 'ROUTE', 850, 322, '#4FA3A5', 'child_care', 'NNA', 'Activar proteccion', 'Activa ruta NNA, atencion psicosocial y coordinacion con ICBF si aplica.', 'n5-ruta-nna', NULL),
    ('proteccion-nna', 'nna-sin-ruta', 'NNA sin ruta', 'WARNING', 246, 226, '#6F8490', 'warning', 'NNA', 'Minimizar afectacion', 'Presenciar violencia tambien puede constituir afectacion psicologica.', 'n5-minimizar', NULL),
    ('urgencias-crisis', 'tool-pap', 'PAP', 'TOOL', 230, 455, '#4FA3A5', 'psychology', 'PAP', 'Tomar herramienta', 'Herramienta de contencion inicial para crisis.', NULL, 'PAP'),
    ('urgencias-crisis', 'tool-bitacora', 'Bitacora', 'TOOL', 340, 455, '#4FA3A5', 'edit_note', 'BIT', 'Tomar herramienta', 'Registra el razonamiento profesional durante la escena.', NULL, 'REFLECTION_JOURNAL'),
    ('ruta-proteccion', 'tool-riesgo', 'Riesgo', 'TOOL', 230, 455, '#4FA3A5', 'monitoring', 'RISK', 'Tomar herramienta', 'Medidor de riesgo para factores agravantes.', NULL, 'RISK_METER'),
    ('ruta-proteccion', 'tool-ruta', 'Ruta', 'TOOL', 340, 455, '#4FA3A5', 'health_and_safety', 'RUTA', 'Tomar herramienta', 'Guia de articulacion institucional.', NULL, 'SAFETY_ROUTE'),
    ('informe-integral', 'tool-riesgo', 'Riesgo', 'TOOL', 230, 455, '#4FA3A5', 'monitoring', 'RISK', 'Tomar herramienta', 'Medidor de riesgo para documentacion tecnica.', NULL, 'RISK_METER'),
    ('informe-integral', 'tool-ruta', 'Ruta', 'TOOL', 340, 455, '#4FA3A5', 'health_and_safety', 'RUTA', 'Tomar herramienta', 'Checklist de medidas activadas.', NULL, 'SAFETY_ROUTE'),
    ('valoracion-comisaria', 'tool-riesgo', 'Riesgo', 'TOOL', 230, 455, '#4FA3A5', 'monitoring', 'RISK', 'Tomar herramienta', 'Instrumento de valoracion estructurada.', NULL, 'RISK_METER'),
    ('valoracion-comisaria', 'tool-ruta', 'Ruta', 'TOOL', 340, 455, '#4FA3A5', 'health_and_safety', 'RUTA', 'Tomar herramienta', 'Medidas de proteccion proporcionales.', NULL, 'SAFETY_ROUTE'),
    ('proteccion-nna', 'tool-ruta', 'Ruta', 'TOOL', 230, 455, '#4FA3A5', 'health_and_safety', 'RUTA', 'Tomar herramienta', 'Ruta diferenciada para NNA.', NULL, 'SAFETY_ROUTE'),
    ('proteccion-nna', 'tool-bitacora', 'Bitacora', 'TOOL', 340, 455, '#4FA3A5', 'edit_note', 'BIT', 'Tomar herramienta', 'Registra proteccion integral y plan de seguimiento.', NULL, 'REFLECTION_JOURNAL')
) AS seed(node_key, object_key, label, object_type, position_x, position_y, color_hex, icon, short_code, prompt, interaction_text, option_key, tool_code)
    ON seed.node_key = n.node_key
LEFT JOIN decision_options d ON d.case_version_id = n.case_version_id AND d.option_key = seed.option_key
ON CONFLICT (scene_map_id, object_key) DO UPDATE SET
    label = EXCLUDED.label,
    object_type = EXCLUDED.object_type,
    position_x = EXCLUDED.position_x,
    position_y = EXCLUDED.position_y,
    color_hex = EXCLUDED.color_hex,
    icon = EXCLUDED.icon,
    short_code = EXCLUDED.short_code,
    interaction_prompt = EXCLUDED.interaction_prompt,
    interaction_text = EXCLUDED.interaction_text,
    decision_option_id = EXCLUDED.decision_option_id,
    tool_code = EXCLUDED.tool_code,
    updated_at = NOW();

INSERT INTO dialogue_trees (scene_map_id, map_object_id, tree_key, speaker_name, portrait_key, emotion)
SELECT mo.scene_map_id, mo.id, mo.object_key, 
       CASE mo.object_type WHEN 'PERSON' THEN 'Consultante' WHEN 'WARNING' THEN 'Alerta de riesgo' WHEN 'TOOL' THEN 'Herramienta profesional' WHEN 'ROUTE' THEN 'Ruta institucional' ELSE 'Objeto del entorno' END,
       mo.object_key,
       CASE mo.object_type WHEN 'WARNING' THEN 'alerta' WHEN 'PERSON' THEN 'vulnerable' ELSE 'neutral' END
FROM map_objects mo
ON CONFLICT (scene_map_id, tree_key) DO UPDATE SET
    speaker_name = EXCLUDED.speaker_name,
    portrait_key = EXCLUDED.portrait_key,
    emotion = EXCLUDED.emotion;

INSERT INTO dialogue_lines (dialogue_tree_id, display_order, speaker_name, text, emotion)
SELECT dt.id, 1, dt.speaker_name, mo.interaction_text, dt.emotion
FROM dialogue_trees dt
JOIN map_objects mo ON mo.id = dt.map_object_id
ON CONFLICT (dialogue_tree_id, display_order) DO UPDATE SET
    speaker_name = EXCLUDED.speaker_name,
    text = EXCLUDED.text,
    emotion = EXCLUDED.emotion;

INSERT INTO dialogue_choices (dialogue_tree_id, choice_key, text, decision_option_id, required_tool_code, display_order)
SELECT dt.id, 'execute', 'Preparar esta intervencion', mo.decision_option_id, mo.tool_code, 1
FROM dialogue_trees dt
JOIN map_objects mo ON mo.id = dt.map_object_id
WHERE mo.decision_option_id IS NOT NULL OR mo.tool_code IS NOT NULL
ON CONFLICT (dialogue_tree_id, choice_key) DO UPDATE SET
    text = EXCLUDED.text,
    decision_option_id = EXCLUDED.decision_option_id,
    required_tool_code = EXCLUDED.required_tool_code;

INSERT INTO rubrics (case_version_id, name, description, active, created_by)
SELECT v.id, 'Rubrica integral VBG', 'Evalua contencion, comunicacion, etica, riesgo, documentacion y rutas de proteccion.', TRUE, v.created_by
FROM case_versions v
JOIN simulation_cases c ON c.id = v.simulation_case_id
WHERE c.code = 'SIM-VBG-001' AND v.semantic_version = '1.0.0'
  AND NOT EXISTS (SELECT 1 FROM rubrics r WHERE r.case_version_id = v.id AND r.name = 'Rubrica integral VBG');

INSERT INTO rubric_criteria (rubric_id, competency, title, description, max_score, display_order)
SELECT r.id, seed.competency, seed.title, seed.description, 5, seed.display_order
FROM rubrics r
JOIN case_versions v ON v.id = r.case_version_id
JOIN simulation_cases c ON c.id = v.simulation_case_id
JOIN (
    VALUES
    ('PAP', 'Contencion inicial', 'Aplica escucha activa, validacion emocional y estabilizacion antes de documentar.', 1),
    ('ETICA', 'Etica y no revictimizacion', 'Evita mediacion, contacto prematuro con agresor y preguntas revictimizantes.', 2),
    ('RIESGO', 'Valoracion de riesgo', 'Identifica amenazas, armas, reincidencia y medidas proporcionales.', 3),
    ('RUTA', 'Activacion de rutas', 'Articula comisaria, salud, trabajo social, proteccion y red de apoyo.', 4),
    ('NNA', 'Proteccion de NNA', 'Reconoce afectacion de menores y activa respuesta diferenciada.', 5)
) AS seed(competency, title, description, display_order) ON TRUE
WHERE c.code = 'SIM-VBG-001' AND r.name = 'Rubrica integral VBG'
  AND NOT EXISTS (
      SELECT 1 FROM rubric_criteria rc WHERE rc.rubric_id = r.id AND rc.competency = seed.competency
  );

INSERT INTO publication_checklists (case_version_id, submitted_by, completion_ratio, status, completed_at)
SELECT v.id, v.created_by, 100, 'COMPLETE', NOW()
FROM case_versions v
JOIN simulation_cases c ON c.id = v.simulation_case_id
WHERE c.code = 'SIM-VBG-001' AND v.semantic_version = '1.0.0'
  AND NOT EXISTS (SELECT 1 FROM publication_checklists pc WHERE pc.case_version_id = v.id);
