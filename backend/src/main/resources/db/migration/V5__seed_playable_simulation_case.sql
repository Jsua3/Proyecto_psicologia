-- V5: Primer caso jugable DAG para PsychoSim.

-- Credenciales demo:
-- estudiante@psychosim.edu.co / Estudiante123!
-- profesora@psychosim.edu.co / Profesor123!

UPDATE users
SET password_hash = '$2b$10$uGLpRS4NkaWYvn2WiZ2YIeH7mVxZsfMMfBAEVWjyisROdKzYpY6kG'
WHERE email = 'profesora@psychosim.edu.co';

INSERT INTO users (email, password_hash, nombre, apellido, role, activo, created_at)
VALUES (
    'estudiante@psychosim.edu.co',
    '$2b$10$pb5UszYfp.WuZiNzabW4rOzo/HpAJat4VkonlCWl/VATEEi.dacTu',
    'Laura',
    'Martinez',
    'ESTUDIANTE',
    TRUE,
    NOW()
)
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = 'ESTUDIANTE',
    activo = TRUE;

INSERT INTO simulation_cases (code, title, description, active, created_by, created_at, updated_at)
SELECT
    'SIM-VBG-001',
    'Violencia Familiar y Tentativa de Feminicidio',
    'Simulacion de decisiones para atencion psicologica inicial, activacion de rutas, valoracion de riesgo y proteccion de NNA en contexto de violencia basada en genero.',
    TRUE,
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'profesora@psychosim.edu.co'
ON CONFLICT (code) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    active = TRUE,
    updated_at = NOW();

INSERT INTO case_versions (simulation_case_id, semantic_version, status, narrative_context, published_at, created_by, created_at)
SELECT
    c.id,
    '1.0.0',
    'PUBLISHED',
    'Caso formativo sobre atencion psicologica, seguridad, marco normativo colombiano, riesgo de feminicidio, NNA y cierre psicosocial integral.',
    NOW(),
    u.id,
    NOW()
FROM simulation_cases c
JOIN users u ON u.email = 'profesora@psychosim.edu.co'
WHERE c.code = 'SIM-VBG-001'
ON CONFLICT (simulation_case_id, semantic_version) DO UPDATE
SET status = 'PUBLISHED',
    narrative_context = EXCLUDED.narrative_context,
    published_at = NOW();

INSERT INTO simulation_nodes (
    case_version_id, node_key, title, narrative, support_resources_json, required_tools_json,
    sensitive_content, safe_exit_required, warning_message, start_node, terminal_node, position_x, position_y
)
SELECT v.id, seed.node_key, seed.title, seed.narrative, seed.resources, seed.tools,
       seed.sensitive, seed.safe_exit, seed.warning, seed.start_node, seed.terminal_node, seed.x, seed.y
FROM case_versions v
JOIN simulation_cases c ON c.id = v.simulation_case_id
JOIN (
    VALUES
    (
        'urgencias-crisis',
        'Sala de urgencias: primera escucha',
        'Una mujer de 34 anos llega al servicio de urgencias con hematomas visibles, llanto contenido y temor por su vida y la de sus dos hijos. La estudiante debe decidir la primera intervencion psicologica.',
        '["PAP: escucha activa, validacion, estabilizacion y seguridad inmediata.", "Evitar preguntas invasivas en fase aguda."]',
        '["PAP","REFLECTION_JOURNAL"]',
        TRUE, TRUE,
        'Contenido sensible: violencia intrafamiliar y riesgo vital.',
        TRUE, FALSE, 80, 80
    ),
    (
        'ruta-proteccion',
        'Revelacion de riesgo alto',
        'La consultante indica que esta es la tercera agresion en seis meses y que su pareja la amenazo con arma blanca. Se requiere activar ruta sin aumentar el riesgo.',
        '["Ley 1257 de 2008.", "Resolucion 459 de 2012 y rutas VBG."]',
        '["RISK_METER","SAFETY_ROUTE","REFLECTION_JOURNAL"]',
        TRUE, TRUE,
        'La mediacion con agresor esta contraindicada en violencia de genero.',
        FALSE, FALSE, 360, 80
    ),
    (
        'informe-integral',
        'Informe psicologico de urgencias',
        'La profesional debe dejar un registro tecnico, suficiente y no revictimizante para articular salud, comisaria y proteccion.',
        '["Registrar hechos observables, estado mental, riesgo, red de apoyo y medidas activadas."]',
        '["RISK_METER","SAFETY_ROUTE","REFLECTION_JOURNAL"]',
        TRUE, FALSE,
        NULL,
        FALSE, FALSE, 640, 80
    ),
    (
        'valoracion-comisaria',
        'Comisaria de Familia: valoracion del riesgo',
        'En la Comisaria se debe valorar riesgo de feminicidio, factores agravantes y medidas de proteccion proporcionales.',
        '["Danger Assessment como referencia formativa.", "Ley 1761 de 2015."]',
        '["RISK_METER","SAFETY_ROUTE","REFLECTION_JOURNAL"]',
        TRUE, TRUE,
        'Decisiones que alerten al agresor pueden aumentar el riesgo.',
        FALSE, FALSE, 920, 80
    ),
    (
        'proteccion-nna',
        'Proteccion de ninos, ninas y adolescentes',
        'Los hijos de 7 y 10 anos presenciaron las agresiones. La estudiante debe decidir como protegerlos sin romper vinculos protectores.',
        '["Ley 1098 de 2006.", "Ley 2126 de 2021.", "Interes superior del nino."]',
        '["SAFETY_ROUTE","REFLECTION_JOURNAL"]',
        TRUE, FALSE,
        NULL,
        FALSE, FALSE, 1200, 80
    ),
    (
        'cierre-seguimiento',
        'Cierre del caso y plan de seguimiento',
        'El caso queda consolidado con plan de seguridad, articulacion interinstitucional, atencion psicosocial y seguimiento activo. La bitacora se bloquea al finalizar el intento.',
        '["Plan de seguridad personalizado.", "Coordinacion con ICBF, Fiscalia, Comisaria y red de apoyo."]',
        '["REFLECTION_JOURNAL"]',
        FALSE, FALSE,
        NULL,
        FALSE, TRUE, 1480, 80
    )
) AS seed(node_key, title, narrative, resources, tools, sensitive, safe_exit, warning, start_node, terminal_node, x, y) ON TRUE
WHERE c.code = 'SIM-VBG-001' AND v.semantic_version = '1.0.0'
ON CONFLICT (case_version_id, node_key) DO UPDATE
SET title = EXCLUDED.title,
    narrative = EXCLUDED.narrative,
    support_resources_json = EXCLUDED.support_resources_json,
    required_tools_json = EXCLUDED.required_tools_json,
    sensitive_content = EXCLUDED.sensitive_content,
    safe_exit_required = EXCLUDED.safe_exit_required,
    warning_message = EXCLUDED.warning_message,
    start_node = EXCLUDED.start_node,
    terminal_node = EXCLUDED.terminal_node,
    position_x = EXCLUDED.position_x,
    position_y = EXCLUDED.position_y;

INSERT INTO decision_options (
    case_version_id, option_key, source_node_id, target_node_id, text, classification,
    score_delta, stress_delta, prohibited_penalty, immediate_feedback, prohibited_conduct, prohibition_reason
)
SELECT v.id, seed.option_key, source_node.id, target_node.id, seed.text, seed.classification,
       seed.score_delta, seed.stress_delta, seed.prohibited_penalty, seed.feedback, seed.prohibited, seed.reason
FROM case_versions v
JOIN simulation_cases c ON c.id = v.simulation_case_id
JOIN (
    VALUES
    ('n1-pap', 'urgencias-crisis', 'ruta-proteccion',
     'Aplicar Primeros Auxilios Psicologicos: escucha activa, validacion emocional, respiracion guiada y verificacion de seguridad inmediata.',
     'ADEQUATE', 10, -5, -50,
     'Decision adecuada. En crisis, la prioridad es estabilizar emocionalmente, reducir activacion fisiologica y construir una base de seguridad para decisiones posteriores.',
     FALSE, NULL),
    ('n1-instrumento', 'urgencias-crisis', 'ruta-proteccion',
     'Aplicar de inmediato un cuestionario extenso para documentar evidencia antes de contener emocionalmente.',
     'INADEQUATE', -10, 20, -50,
     'La documentacion es importante, pero en fase aguda puede saturar a la victima. Primero se estabiliza y luego se evalua con cuidado.',
     FALSE, NULL),
    ('n1-policia', 'urgencias-crisis', 'ruta-proteccion',
     'Llamar a la Policia antes de explicar a la consultante lo que ocurre y sin evaluar riesgo inmediato.',
     'RISKY', 0, 12, -50,
     'Puede ser necesario activar autoridades, pero debe hacerse con explicacion, valoracion de riesgo y cuidado para no aumentar el peligro.',
     FALSE, NULL),
    ('n2-ruta-vbg', 'ruta-proteccion', 'informe-integral',
     'Activar ruta VBG: Comisaria de Familia, trabajo social, valoracion de riesgo, medidas de proteccion y coordinacion interinstitucional.',
     'ADEQUATE', 10, -5, -50,
     'Decision adecuada. La ruta institucional protege derechos, evita que la carga recaiga solo en la victima y permite medidas proporcionales al riesgo.',
     FALSE, NULL),
    ('n2-mediacion', 'ruta-proteccion', 'informe-integral',
     'Contactar al agresor para proponer una mediacion familiar antes de avanzar con medidas de proteccion.',
     'INADEQUATE', -10, 25, -70,
     'Conducta prohibida y de alto riesgo. La mediacion en violencia de genero puede revictimizar y aumentar el peligro para la victima.',
     TRUE, 'Ley 1257 de 2008: prohibicion de mecanismos alternativos de solucion de conflictos en violencia contra la mujer.'),
    ('n2-psiquiatria', 'ruta-proteccion', 'informe-integral',
     'Remitir solo a psiquiatria y postergar la activacion de la ruta de proteccion hasta tener diagnostico clinico.',
     'RISKY', -5, 15, -50,
     'La atencion especializada puede aportar, pero no reemplaza la proteccion inmediata ni la articulacion institucional.',
     FALSE, NULL),
    ('n3-informe-integral', 'informe-integral', 'valoracion-comisaria',
     'Incluir motivo de consulta, estado mental, riesgo de feminicidio, medidas activadas, red de apoyo, proteccion de NNA y plan de seguimiento.',
     'ADEQUATE', 10, -5, -50,
     'Decision adecuada. El informe debe ser tecnico, integral y util para la continuidad de la ruta sin exponer de mas a la victima.',
     FALSE, NULL),
    ('n3-dsm', 'informe-integral', 'valoracion-comisaria',
     'Registrar solo diagnostico DSM y sintomas observados durante la consulta.',
     'INADEQUATE', -10, 16, -50,
     'El diagnostico aislado es insuficiente. El caso exige valoracion de riesgo, medidas de seguridad y articulacion institucional.',
     FALSE, NULL),
    ('n4-riesgo-estructurado', 'valoracion-comisaria', 'proteccion-nna',
     'Aplicar valoracion estructurada de riesgo, identificar armas, amenazas, reincidencia y proponer medidas de proteccion inmediatas.',
     'ADEQUATE', 10, -5, -50,
     'Decision adecuada. La valoracion estructurada ayuda a priorizar medidas de proteccion proporcionales al riesgo alto.',
     FALSE, NULL),
    ('n4-citar-agresor', 'valoracion-comisaria', 'proteccion-nna',
     'Citar al agresor antes de otorgar medidas para escuchar su version.',
     'INADEQUATE', -10, 25, -70,
     'Conducta de alto riesgo. Alertar al agresor antes de medidas de proteccion puede escalar el peligro.',
     TRUE, 'Las medidas de proteccion se orientan por riesgo y seguridad; citar al agresor prematuramente puede aumentar el peligro.'),
    ('n5-ruta-nna', 'proteccion-nna', 'cierre-seguimiento',
     'Activar ruta de proteccion NNA, atencion psicosocial para los menores y coordinacion con ICBF si hay riesgo para su integridad.',
     'ADEQUATE', 10, -5, -50,
     'Decision adecuada. Los NNA que presencian violencia tambien requieren proteccion, escucha y atencion diferenciada.',
     FALSE, NULL),
    ('n5-minimizar', 'proteccion-nna', 'cierre-seguimiento',
     'Registrar que los menores estuvieron presentes, pero no activar ruta porque no fueron agredidos fisicamente.',
     'INADEQUATE', -10, 20, -50,
     'Presenciar violencia tambien puede constituir afectacion psicologica. Se requiere proteccion integral.',
     FALSE, NULL)
) AS seed(option_key, source_key, target_key, text, classification, score_delta, stress_delta, prohibited_penalty, feedback, prohibited, reason) ON TRUE
JOIN simulation_nodes source_node ON source_node.case_version_id = v.id AND source_node.node_key = seed.source_key
JOIN simulation_nodes target_node ON target_node.case_version_id = v.id AND target_node.node_key = seed.target_key
WHERE c.code = 'SIM-VBG-001' AND v.semantic_version = '1.0.0'
ON CONFLICT (case_version_id, option_key) DO UPDATE
SET source_node_id = EXCLUDED.source_node_id,
    target_node_id = EXCLUDED.target_node_id,
    text = EXCLUDED.text,
    classification = EXCLUDED.classification,
    score_delta = EXCLUDED.score_delta,
    stress_delta = EXCLUDED.stress_delta,
    prohibited_penalty = EXCLUDED.prohibited_penalty,
    immediate_feedback = EXCLUDED.immediate_feedback,
    prohibited_conduct = EXCLUDED.prohibited_conduct,
    prohibition_reason = EXCLUDED.prohibition_reason;
