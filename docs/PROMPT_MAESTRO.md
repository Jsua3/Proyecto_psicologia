# Prompt Maestro PsychoSim

## Proposito

Este documento consolida la direccion funcional, visual y tecnica de PsychoSim: una plataforma web institucional para la Facultad de Psicologia de `[NOMBRE_UNIVERSIDAD]`, orientada a simulacion gamificada, aprendizaje etico, seguimiento docente y mejora continua.

El documento funciona como contrato vivo del proyecto. Toda transformacion importante del frontend, backend, base de datos, seguridad, accesibilidad o experiencia de usuario debe quedar registrada aqui.

## Fuentes Consolidadas

- Requisitos de negocio `RQ-NEG-001` a `RQ-NEG-008`.
- Requisitos no funcionales `RNF-001` a `RNF-010`.
- Tecnicas de elicitacion aplicadas: caso de estudio, analisis documental, encuesta, entrevista docente, prototipado, revision de ingenieria, revision normativa/etica y taller de validacion.
- Prompt frontend institucional premium para Facultad de Psicologia.
- Prompt backend senior para Java 21/25, Spring Boot 3.4+ y arquitectura hexagonal.

## Vision Producto

PsychoSim debe dejar de ser principalmente una aplicacion de escritorio y convertirse en una plataforma web academica completa:

- Sitio publico de Facultad de Psicologia, moderno, humano, sereno, cientifico e institucional.
- Portal autenticado para estudiantes, docentes y administradores.
- Simulador gamificado basado en casos psicosociales sensibles y no sensibles.
- Motor de simulacion basado en grafos dirigidos aciclicos (DAG), no en contenido hardcodeado.
- Evaluacion formativa por competencias, bitacoras de reflexion, rubricas, analiticas y trazabilidad.
- Gestion de catalogo con versionamiento, validacion experta y publicacion controlada.
- Auditoria persistente, privacidad por diseno, minimizacion de datos y cifrado de campos sensibles.

## Identidad Frontend

Concepto rector: ciencia, escucha y bienestar humano.

La interfaz debe sentirse calmada, clara, confiable, empatica e institucional. Debe evitar el tono de landing SaaS generica, clinica fria o pagina universitaria plana.

### Stack Frontend Objetivo

- Angular 21 con standalone components.
- Angular Router.
- Angular Material para formularios, menus, dialogos, tablas y controles.
- SCSS modular con CSS custom properties.
- Signals para estados locales de UI.
- Canvas opcional para fondos abstractos suaves.
- Soporte completo para `prefers-reduced-motion`.
- Playwright para QA visual responsive.

### Paleta Base

```css
:root {
  --psy-bg: #F4F7FA;
  --psy-surface: #FFFFFF;
  --psy-surface-soft: #EAF1F4;
  --psy-ink: #24323A;
  --psy-muted: #687A86;
  --psy-blue: #4F7CAC;
  --psy-blue-deep: #2F5F8F;
  --psy-teal: #4FA3A5;
  --psy-teal-deep: #2F7476;
  --psy-green: #8CBFA6;
  --psy-green-deep: #5D9278;
  --psy-lavender: #A99BD6;
  --psy-lavender-soft: #E7E2F5;
  --psy-border: rgba(47, 95, 143, 0.16);
  --psy-focus: rgba(79, 124, 172, 0.32);
}
```

### Experiencia Publica

La landing debe incluir:

- Hero full-bleed con senal visual institucional.
- H1: `Facultad de Psicologia`.
- CTAs: `Conoce los programas`, `Agenda orientacion`, `Explora investigacion`.
- Programas academicos.
- Clinica o centro de atencion psicologica.
- Investigacion y semilleros.
- Bienestar y rutas de apoyo.
- Docentes y grupos.
- Eventos y noticias.
- Admisiones.
- Contacto.

### Experiencia Autenticada

- Login sin navbar publica.
- Desktop: dos columnas, imagen/escena institucional y card Liquid Glass con formulario.
- Mobile: card centrada con fondo suave.
- Dashboard y modulos administrativos densos, sobrios y legibles.
- Skeleton loading antes que spinners cuando aplique.
- Focus ring visible y navegacion completa por teclado.

## Arquitectura Backend Objetivo

El backend debe operar bajo Arquitectura Hexagonal:

- Dominio puro sin dependencias de Spring, JPA, Web o Security.
- Casos de uso en capa de aplicacion.
- Puertos de entrada y salida.
- Adaptadores REST, JPA, seguridad, auditoria y cifrado.
- Pruebas unitarias del dominio sin Spring.
- Pruebas de integracion con Testcontainers.

### Stack Backend Objetivo

- Java 21 LTS en el entorno actual; Java 25 como ruta futura cuando el entorno lo soporte.
- Spring Boot 3.4+.
- Virtual Threads: `spring.threads.virtual.enabled=true`.
- Spring Security 6.4+ con JWT stateless y ruta compatible con SSO Google/Microsoft.
- PostgreSQL 16+ como base principal.
- Hibernate 6.5+ gestionado por Spring Boot.
- Auditoria persistente con retencion minima de 12 meses.
- OpenAPI 3 mediante Springdoc.

## Dominio Objetivo

Entidades y conceptos principales:

- `SimulationCase`: contenedor academico del caso.
- `CaseVersion`: version inmutable publicable del caso.
- `SimulationNode`: nodo/escena dentro del grafo DAG.
- `DecisionOption`: arista de decision entre nodo origen y destino.
- `DecisionClassification`: `ADEQUATE`, `RISKY`, `INADEQUATE`.
- `SimulationAttempt`: intento del estudiante con token inmutable.
- `AttemptEvent`: evento trazable del intento.
- `ReflectionJournal`: bitacora cifrada y bloqueable al finalizar.
- `Rubric` y `RubricEvaluation`: evaluacion docente por competencias.
- `PublicationChecklist`: validacion etica/academica antes de publicar.
- `AuditLog`: auditoria global de acciones sensibles.

## Reglas Criticas

- Un caso en borrador no puede ser visible para estudiantes.
- Un caso publicado no debe modificarse destructivamente; se clona y versiona.
- Las decisiones prohibidas generan penalizacion drastica e inmutable.
- Todo intento finalizado bloquea la bitacora.
- La salida segura debe guardar estado y devolver recursos de apoyo.
- Las analiticas agregadas deben anonimizar datos por defecto.
- Las vistas individuales identificables solo son permitidas para docentes autorizados.
- Los textos libres sensibles deben cifrarse en reposo.
- Los logs deben registrar usuario, rol, accion, contexto y sello de tiempo.

## Registro De Cambios Aplicados

### 2026-05-26 - Inicio De Transformacion

- Se consolido este Prompt Maestro como documento rector del proyecto.
- Se confirmo que el workspace actual no contiene repositorio Git inicializado.
- Se verifico el entorno local:
  - Node.js `22.13.1`.
  - npm `11.11.0`.
  - Java `21.0.7`.
  - Maven `3.9.14`.
- Se verifico disponibilidad de Angular 21 en npm.
- Se verifico disponibilidad de Spring Boot 3.5.x como linea compatible con el requisito `3.4+`.

### 2026-05-26 - Backend Hexagonal, Dominio DAG Y Base Tecnica

- Se actualizo el backend de Spring Boot `3.3.0` a `3.5.14`.
- Se actualizo Springdoc a `2.8.17`.
- Se agrego `spring-boot-starter-aop` como base para auditoria por aspectos.
- Se activo `spring.threads.virtual.enabled=true`.
- Se cambio el puerto backend por defecto a `8080` para alinear Docker, Nginx, README y proxy local.
- Se desactivo `spring.jpa.open-in-view`.
- Se agrego el paquete `com.psychosim.simulation` como inicio de arquitectura hexagonal:
  - Dominio puro para nodos, opciones de decision, versiones de caso, intentos, eventos y motor de transicion.
  - Puertos de entrada para flujo estudiante.
  - Puertos de salida para grafo, intentos, cifrado de bitacora y auditoria.
- Se agrego `V4__simulation_hexagonal_foundation.sql` con tablas para:
  - catalogo versionado,
  - nodos DAG,
  - opciones de decision,
  - intentos v2,
  - eventos trazables,
  - bitacoras cifradas,
  - rubricas,
  - checklist de publicacion,
  - auditoria persistente.
- Se agregaron pruebas unitarias de dominio:
  - validacion de un unico nodo inicial,
  - rechazo de ciclos en el DAG,
  - exposicion de decisiones salientes,
  - transicion de intentos,
  - penalizacion de conductas prohibidas,
  - bloqueo de ejecucion de casos no publicados.

### 2026-05-26 - Frontend Angular 21 Institucional

- Se migro `admin-panel` a Angular `21.2.x`.
- Se actualizo TypeScript a `5.9.3`, Zone.js a `0.16.2`, RxJS a `7.8.2` y Playwright a `1.60.0`.
- Se agrego imagen institucional libre de Unsplash para el hero y login.
- Se reemplazo la ruta publica inicial por una landing completa de Facultad de Psicologia:
  - hero full-bleed,
  - programas,
  - centro de atencion,
  - investigacion,
  - bienestar,
  - docentes/eventos/admisiones,
  - contacto.
- Se redisenio el login como experiencia independiente, sin navbar publica, con dos columnas en escritorio y card Liquid Glass.
- Se redisenio el shell del portal autenticado con navegacion sobria, glassmorphism institucional y responsive.
- Se redisenio el dashboard con metricas, estado de API no disponible, tabla/empty-state y lectura docente.
- Se crearon tokens globales y clases base en `styles.scss`:
  - `.psy-button`,
  - `.psy-button--primary`,
  - `.psy-button--ghost`,
  - `.psy-button--glass`,
  - `.psy-icon-button`,
  - `.psy-action-row`,
  - `.liquid-glass`,
  - `.liquid-tilt`,
  - `.psy-reveal`,
  - `.psy-skeleton`,
  - `.psy-chip`,
  - `.psy-public-nav`,
  - `.psy-auth-card`.
- Se agrego soporte global para `prefers-reduced-motion`.

### 2026-05-26 - Verificacion

- Backend: `mvn test` exitoso, 15 pruebas ejecutadas, 0 fallos.
- Frontend: `npm run build` exitoso con Angular 21.
- Seguridad npm produccion: `npm audit --omit=dev --audit-level=moderate` sin vulnerabilidades.
- Seguridad npm dev: quedan 4 vulnerabilidades moderadas transitivas en `webpack-dev-server -> sockjs -> uuid`, sin fix disponible reportado por npm al 2026-05-26.
- QA visual Playwright exitoso en:
  - landing desktop 1440px,
  - landing mobile 390px con menu abierto,
  - login desktop 1440px,
  - login mobile 390px,
  - portal desktop 1280px,
  - portal mobile 390px.
- Resultado QA: sin scroll horizontal y sin texto recortado en los escenarios revisados.
- Capturas generadas en `admin-panel/qa-screenshots`.

## Pendientes De Implementacion

- Implementar adaptadores JPA/REST para el nuevo dominio hexagonal.
- Migrar endpoints existentes hacia `simulation` sin romper compatibilidad.
- Implementar cifrado real de bitacoras con `ReflectionCipherPort`.
- Implementar auditoria por AOP o listeners hacia `audit_logs`.
- Implementar publicacion con checklist al 100%.
- Implementar versionamiento/clonado real de casos.
- Redisenar modulos de casos, grupos y reportes con el nuevo sistema visual.
- Agregar pruebas de integracion Testcontainers sobre el esquema V4.
- Ejecutar QA visual adicional en 360, 768, 1024 y 1440px sobre modales, tablas y formularios administrativos.
