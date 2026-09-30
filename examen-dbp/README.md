 LabReserve UTEC

API REST para registrar laboratorios, publicar turnos de equipos y reservarlos.

Spring Boot 3.5, PostgreSQL, JPA, JWT.

 Endpoints
| Método | Ruta | Rol |

| POST | /auth/register |publico | | POST | /auth/login | público |
| POST | /laboratories | ADMIN |
| GET |  /laboratories | autenticado |
| POST | /equipment-slots | TECHNICIAN, ADMIN |
| GET | /equipment-slots | autenticado |
| POST | /reservations | STUDENT |
| GET | /reservations/me | autenticado |
| PATCH | /reservations/{id}/cancel` | autenticado |

.
