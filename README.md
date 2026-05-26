# PsychoSim

Plataforma web institucional para la Facultad de Psicología: simulación gamificada, evaluación formativa, bienestar, investigación y seguimiento docente.

## Arquitectura

| Componente | Tecnología |
|------------|------------|
| Frontend web principal | Angular 21 + Angular Material + SCSS tokens |
| Backend | Spring Boot 3.5 + Spring Security 6 + JWT |
| Cliente de escritorio | JavaFX 21 + Phaser 3 (legado, no superficie principal) |
| Base de datos | PostgreSQL 16 (producción) / H2 (tests) |
| Infraestructura | Docker Compose + Nginx + GitHub Actions |

## Inicio rápido

### Con Docker Compose

```bash
docker compose up -d
```

- Web / Portal: http://localhost
- API: http://localhost/api
- Swagger UI: http://localhost:8080/swagger-ui.html

### Credenciales por defecto

- Email: `admin@psychosim.edu.co`
- Password: `Admin123!`

## Desarrollo local

### Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Admin Panel

```bash
cd admin-panel
npm install
ng serve
```

Frontend local: http://localhost:4200

El proxy local apunta a `http://localhost:8080`.

### Cliente JavaFX

```bash
cd client
mvn javafx:run
```

El cliente JavaFX se conserva como legado durante la migración, pero el producto principal pasa a ser la experiencia web Angular.

## Estructura del proyecto

```
psychosim/
├── backend/          ← Spring Boot (Maven)
├── client/           ← JavaFX + Phaser 3 (legado)
├── admin-panel/      ← Angular 21
├── docker/           ← Dockerfiles + nginx.conf
├── docs/             ← Prompt maestro y documentación de transformación
├── docker-compose.yml
└── .github/workflows/ci.yml
```
