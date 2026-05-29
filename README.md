# SIEP — Sistema de Entrenamiento Psicosocial

SIEP — Sistema de Entrenamiento Psicosocial es una plataforma web académica orientada a la formación de estudiantes de Psicología mediante simulaciones de casos psicosociales. El sistema permite practicar la toma de decisiones, registrar la trazabilidad del proceso, generar bitácoras reflexivas y facilitar la evaluación docente mediante rúbricas e informes formativos.

Desarrollada para el Programa de Psicología de la Corporación Universitaria Empresarial Alexander Von Humboldt.
## Arquitectura

| Componente | Tecnología |
|------------|------------|
| Frontend web principal | Angular 21 + Angular Material + SCSS tokens |
| Backend | Spring Boot 3.5 + Spring Security 6 + JWT |
| Cliente de escritorio | JavaFX 21 + Phaser 3 (legado, no superficie principal) |
| Base de datos | PostgreSQL 16 (producción) / H2 (tests) |
| Infraestructura | Docker Compose + Nginx + GitHub Actions |

## Inicio Rápido

### Un solo comando (recomendado)

Requisitos: Node.js 20+, Docker Desktop.

```bash
npm install
npm run up
```

Esto levanta PostgreSQL y el backend en Docker, espera a que la API esté lista y arranca el frontend Angular en modo desarrollo.

- Portal dev: http://localhost:4200
- API backend: http://localhost:8090
- Swagger UI: http://localhost:8090/swagger-ui.html

Otros comandos útiles:

```bash
npm run up:docker   # stack completo en Docker (portal en http://localhost)
npm run down        # detener contenedores
npm run logs        # ver logs de db y backend
```

### Con Docker Compose (manual)

```bash
docker compose up -d
```

- Web / Portal: http://localhost
- API vía Nginx: http://localhost/api
- API backend en host: http://localhost:8090
- Swagger UI: http://localhost:8090/swagger-ui.html

### Credenciales Por Defecto

- Email: `admin@psychosim.edu.co`
- Password: `Admin123!`
- Email estudiante demo: `estudiante@psychosim.edu.co`
- Password estudiante demo: `Estudiante123!`
- Email docente demo: `profesora@psychosim.edu.co`
- Password docente demo: `Profesor123!`

## Simulador formativo

El portal incluye un flujo de entrenamiento psicosocial en `Portal -> Simulador`. La experiencia está diseñada como simulación formativa explorable con Phaser:
- catálogo de casos publicados,
- inicio de intento con token de juego,
- escenas tipo grafo DAG,
- mapa top-down donde el estudiante puede moverse por el caso,
- interaccion con personas, objetos, rutas y herramientas del entorno,
- dialogos contextuales activados con `E`, `Espacio` o `Enter`,
- HUD con puntaje profesional, estres de escena y estado del intento,
- mapa de mision, escena visual y dialogos contextuales,
- herramientas profesionales activables,
- acciones de intervencion con retroalimentacion inmediata de supervision,
- bitacora reflexiva cifrada en backend,
- salida segura,
- cierre automatico al llegar a un nodo terminal.

## Desarrollo Local

Usa `npm run up` desde la raíz del repo (ver arriba). No hace falta Maven instalado: el backend corre en Docker.

Si prefieres el backend nativo con Maven:

```bash
docker compose up -d db
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=8090
```

En otra terminal:

```bash
cd admin-panel
npm start
```

El proxy de Angular apunta a `http://localhost:8090`.

### Cliente JavaFX

```bash
cd client
mvn javafx:run
```

El cliente JavaFX se conserva como legado durante la migración, pero el producto principal pasa a ser la experiencia web Angular.

## Estructura Del Proyecto

```text
psychosim/
├── backend/          <- Spring Boot (Maven)
├── client/           <- JavaFX + Phaser 3 (legado)
├── admin-panel/      <- Angular 21
├── docker/           <- Dockerfiles + nginx.conf
├── docs/             <- Prompt maestro y documentación de transformación
├── docker-compose.yml
└── .github/workflows/ci.yml
```
