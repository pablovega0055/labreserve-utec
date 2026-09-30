# LabReserve UTEC

API REST para registrar laboratorios, publicar turnos de equipos y reservarlos.

Spring Boot 3.5, PostgreSQL, JPA, JWT.

## Cómo correrlo

```bash
docker compose up -d
```

Luego ejecutar `ExamenApplication`. La API queda en `http://localhost:8080`.

Tests: `mvn clean verify`

Usuarios de prueba:
- admin / admin12345
- technician / tech12345

## Endpoints

| Método | Ruta | Rol |
|---|---|---|
| POST | `/auth/register` | público |
| POST | `/auth/login` | público |
| POST | `/laboratories` | ADMIN |
| GET | `/laboratories` | autenticado |
| POST | `/equipment-slots` | TECHNICIAN, ADMIN |
| GET | `/equipment-slots` | autenticado |
| POST | `/reservations` | STUDENT |
| GET | `/reservations/me` | autenticado |
| PATCH | `/reservations/{id}/cancel` | autenticado |

La colección de Postman está en `postman/`.
