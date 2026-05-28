# PsychoSim

Plataforma web institucional para el Programa de Psicología de la Corporación Universitaria Empresarial Alexander Von Humboldt: simulación gamificada, evaluación formativa, bienestar, investigación y seguimiento docente.

## Arquitectura

| Componente | Tecnología |
|------------|------------|
| Frontend web principal | Angular 21 + Angular Material + SCSS tokens |
| Backend | Spring Boot 3.5 + Spring Security 6 + JWT |
| Cliente de escritorio | JavaFX 21 + Phaser 3 (legado, no superficie principal) |
| Base de datos | PostgreSQL 16 (producción) / H2 (tests) |
| Infraestructura | Docker Compose + Nginx + GitHub Actions |

## Inicio Rápido

### Con Docker Compose

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

## Simulador Jugable

El portal incluye un primer flujo de juego serio en `Portal -> Simulador`. La experiencia esta disenada como una mision clinica explorable con Phaser, no como un examen de opciones ABCD:

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

### Backend

El backend corre por defecto en `8080`, pero en este workspace se usa `8090` porque `8080` está ocupado por un proceso local `httpd`.

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=8090
```

### Admin Panel

```bash
cd admin-panel
npm install
ng serve
```

Frontend local: http://localhost:4200

El proxy local apunta a `http://localhost:8090`.

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
