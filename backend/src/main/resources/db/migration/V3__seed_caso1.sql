-- V3: Caso 1 — Violencia Familiar y Tentativa de Feminicidio

-- Insertar profesora de ejemplo
INSERT INTO users (email, password_hash, nombre, apellido, role, activo, created_at)
VALUES (
    'profesora@psychosim.edu.co',
    '$2b$10$NBsptwdR7LTD2m.0oR/7HOnsVtyvk/oye56iPvPiGzCnmaUjS91fK',
    'María',
    'González',
    'PROFESOR',
    TRUE,
    NOW()
);

-- Insertar caso
INSERT INTO casos (titulo, descripcion, contexto_narrativo, activo, created_by, created_at)
VALUES (
    'Violencia Familiar y Tentativa de Feminicidio',
    'Caso clínico sobre atención psicológica en contexto de violencia intrafamiliar grave con riesgo de feminicidio.',
    'Una mujer de 34 años, con dos hijos menores de edad (7 y 10 años), acude al servicio de urgencias del hospital con hematomas visibles en rostro y brazos. Refiere que su pareja la agredió físicamente durante la madrugada. Esta es la tercera vez que acude al servicio en seis meses. Expresa temor por su vida y la de sus hijos. Su pareja tiene antecedentes de consumo de alcohol y amenazas previas con arma blanca.',
    TRUE,
    1,
    NOW()
);

-- Escenario 1: Hospital — Urgencias
INSERT INTO escenarios (caso_id, orden, nombre, contexto, mapa_key)
VALUES (
    1,
    1,
    'Sala de Urgencias del Hospital',
    'La psicóloga atiende a la víctima en una sala privada del servicio de urgencias. La paciente está asustada, con llanto contenido y dificultad para hablar. Los niños están en la sala de espera con una enfermera.',
    'hospital'
);

-- Escenario 2: Comisaría de Familia
INSERT INTO escenarios (caso_id, orden, nombre, contexto, mapa_key)
VALUES (
    1,
    2,
    'Comisaría de Familia',
    'Al día siguiente, la víctima asiste a la Comisaría de Familia acompañada por la psicóloga del hospital. Se realiza la valoración integral del riesgo y se coordinan las rutas de protección interinstitucional.',
    'comisaria'
);

-- ================================================================
-- ESCENARIO 1 — PREGUNTAS Y OPCIONES
-- ================================================================

-- Pregunta 1 del Escenario 1
INSERT INTO preguntas (escenario_id, orden, enunciado, puntos_correcta)
VALUES (
    1,
    1,
    'La paciente se encuentra en estado de shock emocional, con llanto contenido y dificultad para comunicarse. ¿Cuál es la primera intervención psicológica más adecuada en este momento?',
    10
);

INSERT INTO opciones (pregunta_id, texto, es_correcta, feedback_texto, normativa_ref) VALUES
(1, 'Aplicar de inmediato el cuestionario estandarizado de violencia intrafamiliar para recolectar evidencia.',
 FALSE,
 'La aplicación de instrumentos estandarizados no es prioritaria en la fase aguda. La víctima necesita primero estabilización emocional antes de cualquier evaluación formal.',
 NULL),
(1, 'Aplicar Primeros Auxilios Psicológicos (PAP) con técnicas de contención emocional: escucha activa, validación del dolor, reducción de la activación fisiológica y establecimiento de seguridad inmediata.',
 TRUE,
 '¡Correcto! Los PAP son la intervención de primera línea en crisis. El objetivo es estabilizar emocionalmente, garantizar la seguridad y restablecer la capacidad de toma de decisiones de la víctima.',
 'Protocolo de Atención Integral a Víctimas de Violencia Sexual y de Género — Ministerio de Salud Colombia'),
(1, 'Llamar inmediatamente a la Policía Nacional para documentar el hecho antes de realizar cualquier intervención psicológica.',
 FALSE,
 'Aunque el reporte policial es importante, la primera prioridad es la estabilización emocional de la víctima. La decisión de denunciar debe tomarse junto con ella, salvo riesgo inminente de vida.',
 NULL),
(1, 'Informar a la paciente sobre sus derechos legales y entregarle el directorio de casas de refugio disponibles en la ciudad.',
 FALSE,
 'La información sobre recursos es importante pero prematura como primera intervención. La víctima en estado de shock no puede procesar información compleja. Primero: contención emocional.',
 NULL);

-- Pregunta 2 del Escenario 1
INSERT INTO preguntas (escenario_id, orden, enunciado, puntos_correcta)
VALUES (
    1,
    2,
    'La paciente revela que esta es la tercera agresión física en seis meses y que su pareja la amenazó con un cuchillo la semana anterior. ¿Qué ruta de atención y marco normativo debe activar el profesional de salud?',
    10
);

INSERT INTO opciones (pregunta_id, texto, es_correcta, feedback_texto, normativa_ref) VALUES
(2, 'Únicamente registrar el caso en la historia clínica y recomendar a la paciente que interponga la denuncia por sus propios medios en la Fiscalía.',
 FALSE,
 'El profesional de salud tiene obligación legal de activar la ruta de atención. Dejar esta responsabilidad exclusivamente a la víctima incumple el deber institucional y puede aumentar el riesgo.',
 NULL),
(2, 'Activar la ruta de atención a víctimas de violencia basada en género: notificación a la Comisaría de Familia, coordinación con trabajo social, aplicación del protocolo Resolución 459/2012 del MSPS y garantía de los derechos establecidos en la Ley 1257 de 2008.',
 TRUE,
 '¡Correcto! La Resolución 459/2012 establece el protocolo de atención integral para víctimas de violencia sexual y de género en el sector salud. La Ley 1257/2008 garantiza los derechos de las mujeres a una vida libre de violencias.',
 'Resolución 459/2012 MSPS — Protocolo de Atención Integral Víctimas VBG; Ley 1257 de 2008'),
(2, 'Contactar al agresor para realizar una sesión de mediación familiar en el hospital antes de tomar otras medidas.',
 FALSE,
 'La mediación con el agresor está contraindicada en casos de violencia con riesgo para la vida. Puede aumentar el peligro para la víctima y revictimizarla. Está expresamente prohibida por la Ley 1257/2008.',
 'Ley 1257 de 2008, Art. 8 — Prohibición de mecanismos alternativos de solución de conflictos en casos de violencia contra la mujer'),
(2, 'Remitir el caso únicamente al servicio de psiquiatría para evaluación del trastorno de estrés postraumático y postergar la ruta de protección.',
 FALSE,
 'La evaluación psiquiátrica puede ser parte del proceso, pero no debe postergar la activación de la ruta de protección. La seguridad inmediata de la víctima es prioritaria.',
 NULL);

-- Pregunta 3 del Escenario 1
INSERT INTO preguntas (escenario_id, orden, enunciado, puntos_correcta)
VALUES (
    1,
    3,
    'Al elaborar el informe psicológico de urgencias, ¿qué elementos debe incluir obligatoriamente para garantizar una evaluación integral conforme a los estándares colombianos?',
    10
);

INSERT INTO opciones (pregunta_id, texto, es_correcta, feedback_texto, normativa_ref) VALUES
(3, 'Solo el diagnóstico clínico DSM-5 y la descripción de los síntomas observados durante la consulta.',
 FALSE,
 'Un informe de urgencias en contexto de VBG requiere elementos adicionales: descripción de lesiones, valoración de riesgo, red de apoyo, recursos activados y recomendaciones de seguridad.',
 NULL),
(3, 'Descripción objetiva de la situación, estado mental observado, sintomatología presente, diagnóstico provisional y medicación sugerida exclusivamente.',
 FALSE,
 'La medicación no es competencia del psicólogo. Además, el informe debe incluir valoración de riesgo, protección de menores y coordinación interinstitucional.',
 NULL),
(3, 'Motivo de consulta, estado mental, factores de riesgo y protección, impacto psicológico de las agresiones, evaluación del riesgo para los menores y recomendaciones de intervención.',
 FALSE,
 'Este es un buen informe pero le faltan elementos clave: la valoración del riesgo de feminicidio, las medidas de seguridad concretas activadas y la coordinación interinstitucional.',
 NULL),
(3, 'Todos los anteriores más: valoración específica del riesgo de feminicidio, medidas de seguridad adoptadas, red de apoyo disponible, coordinación interinstitucional activada y plan de seguimiento psicosocial.',
 TRUE,
 '¡Correcto! Un informe integral en VBG debe incluir la valoración del riesgo de feminicidio (usando instrumentos validados), las acciones de protección adoptadas y el plan de articulación con otras instituciones.',
 'Protocolo de Atención Integral VBG — MSPS; Lineamientos para la Práctica de la Psicología Forense en Colombia — Colegio Colombiano de Psicólogos');

-- ================================================================
-- ESCENARIO 2 — PREGUNTAS Y OPCIONES
-- ================================================================

-- Pregunta 1 del Escenario 2
INSERT INTO preguntas (escenario_id, orden, enunciado, puntos_correcta)
VALUES (
    2,
    1,
    'En la Comisaría de Familia, la psicóloga debe realizar la valoración del riesgo. ¿Cuál es el procedimiento correcto?',
    10
);

INSERT INTO opciones (pregunta_id, texto, es_correcta, feedback_texto, normativa_ref) VALUES
(4, 'Realizar únicamente una entrevista clínica no estructurada para evaluar el estado emocional actual de la víctima.',
 FALSE,
 'La entrevista clínica es insuficiente para la valoración del riesgo de feminicidio. Se requieren instrumentos validados y un análisis de factores de riesgo específicos.',
 NULL),
(4, 'Aplicar instrumento validado de valoración de riesgo de feminicidio (como el Danger Assessment), identificar factores agravantes (armas, amenazas previas, consumo de sustancias) y proponer medidas de protección inmediata proporcionales al nivel de riesgo.',
 TRUE,
 '¡Correcto! La valoración estructurada del riesgo permite determinar medidas de protección proporcionales. Los factores presentes (arma blanca, amenazas, reincidencia, consumo de alcohol) indican riesgo alto.',
 'Ley 1761 de 2015 — Rosa Elvira Cely; Protocolo Nacional de Valoración de Riesgo de Feminicidio'),
(4, 'Solicitar a la víctima que redacte por escrito el relato completo de todos los episodios de violencia para construir el expediente jurídico.',
 FALSE,
 'Obligar a la víctima a redactar repetidamente su historia de violencia es una forma de revictimización. El profesional debe tomar el registro, no la víctima.',
 NULL),
(4, 'Citar al agresor a la Comisaría para escuchar su versión antes de determinar medidas de protección.',
 FALSE,
 'Citar al agresor antes de establecer medidas de protección puede alertarlo y aumentar el riesgo para la víctima. Las medidas de protección se otorgan con base en la valoración del riesgo.',
 'Ley 1257 de 2008, Art. 17 — Medidas de protección inmediata');

-- Pregunta 2 del Escenario 2
INSERT INTO preguntas (escenario_id, orden, enunciado, puntos_correcta)
VALUES (
    2,
    2,
    'Los dos hijos de la víctima (7 y 10 años) presenciaron las agresiones. ¿Qué marco legal obliga a protegerlos y cuál es la ruta correcta?',
    10
);

INSERT INTO opciones (pregunta_id, texto, es_correcta, feedback_texto, normativa_ref) VALUES
(5, 'Activar la ruta de protección para niños, niñas y adolescentes conforme a la Ley 2126/2021 (sistema nacional de convivencia familiar) en articulación con la Ley 1098/2006 (Código de Infancia y Adolescencia) y la Ley 1257/2008, notificando al ICBF si hay riesgo para su integridad.',
 TRUE,
 '¡Correcto! Los NNA que presencian violencia son víctimas directas. La Ley 2126/2021 actualiza el sistema de atención a violencias en el hogar. La Ley 1098/2006 establece la obligación de denunciar y proteger a los menores. El ICBF debe ser notificado.',
 'Ley 2126 de 2021; Ley 1098 de 2006 — Código de Infancia y Adolescencia; Ley 1257 de 2008'),
(5, 'Registrar en el informe que los menores estuvieron presentes pero que al no ser agredidos físicamente no requieren intervención inmediata.',
 FALSE,
 'Presenciar violencia intrafamiliar es una forma de violencia psicológica para los NNA. Tienen derecho a atención psicosocial independientemente de si sufrieron agresión física directa.',
 'Ley 1098 de 2006, Art. 18 — Derecho a la integridad personal'),
(5, 'Separar a los menores de la madre e internarlos provisionalmente en un hogar de paso del ICBF mientras se resuelve la situación.',
 FALSE,
 'La separación de la madre no es la primera medida. El interés superior del niño indica mantener el vínculo materno-filial cuando la madre es la víctima. El ICBF debe evaluar alternativas que preserven este vínculo.',
 NULL),
(5, 'Remitir únicamente a los menores a consulta de psicología infantil en el hospital, sin activar ninguna otra ruta institucional.',
 FALSE,
 'La atención psicológica es necesaria, pero insuficiente. Debe activarse la ruta de protección integral que incluye al ICBF, la Comisaría y las medidas de seguridad para el núcleo familiar.',
 NULL);

-- Pregunta 3 del Escenario 2
INSERT INTO preguntas (escenario_id, orden, enunciado, puntos_correcta)
VALUES (
    2,
    3,
    'Al cierre de la atención en la Comisaría, ¿cuál debe ser el plan de intervención psicosocial más completo para esta familia?',
    10
);

INSERT INTO opciones (pregunta_id, texto, es_correcta, feedback_texto, normativa_ref) VALUES
(6, 'Programar citas mensuales de psicología para la víctima y esperar que ella reporte nuevas agresiones.',
 FALSE,
 'Un plan mensual es insuficiente ante un riesgo alto de feminicidio. Se requiere seguimiento activo, articulación interinstitucional y medidas de seguridad concretas.',
 NULL),
(6, 'Plan de seguridad personalizado para la víctima, atención psicosocial semanal para ella y los menores, articulación con casa de refugio si el riesgo es alto, seguimiento activo por parte de la Comisaría y coordinación con Fiscalía para medidas de protección legal.',
 FALSE,
 'Este plan es sólido pero no incluye la valoración integral de los derechos de los menores ni la activación explícita de las rutas de protección para NNA.',
 NULL),
(6, 'Plan de seguridad personalizado + atención psicosocial para la víctima y los menores + articulación interinstitucional (ICBF, Fiscalía, Comisaría, casa refugio) + seguimiento activo del caso + valoración integral del riesgo para los derechos de los NNA y activación de rutas de protección específicas.',
 TRUE,
 '¡Correcto! El plan integral debe incluir todas estas dimensiones: seguridad física, atención psicosocial diferenciada, protección legal, protección de los NNA y coordinación entre todas las instituciones responsables.',
 'Ley 1257 de 2008; Ley 2126 de 2021; Ley 1098 de 2006; Modelo de Atención Integral a Víctimas de Violencias Basadas en Género — MSPS'),
(6, 'Derivar el caso completamente a la Fiscalía y cerrar la intervención psicosocial una vez activada la denuncia penal.',
 FALSE,
 'La denuncia penal es un paso importante, pero no reemplaza la intervención psicosocial continua. El proceso judicial puede ser largo y la víctima necesita acompañamiento durante todo ese tiempo.',
 NULL);
