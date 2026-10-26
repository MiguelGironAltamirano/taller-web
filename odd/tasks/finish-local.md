# Feature: finish-local

## Objetivo

Terminar el producto para que funcione completo contra el backend real (sin mocks), levantado en local con Docker Compose.

## Problema

El frontend ya consume endpoints que el backend no implementa (triage, dashboard, calificación, prioridad, auditoría, configuración, cierre automático, filtros). Con `VITE_USE_MOCKS=false` esas pantallas responden 404. Además, el stack de Docker no tiene healthcheck del backend ni datos demo más allá de los usuarios.

## Decisiones (aceptadas por el responsable, 2026-10-26)

- D-FL1: la calificación NO cambia el estado. Se cierra con `POST /transitions {to: CERRADA}` o con el cierre automático; `POST /requests/{code}/rating` responde `409` si no está `CERRADA` o ya fue calificada (`docs/api/ui-extensions.md`).
- D-FL2: el módulo `requests` publica un puerto de lectura `RequestAnalyticsQuery` (filas planas por solicitud con la primera fecha de `RESUELTA` tomada del historial); `dashboard` agrega en Java puro y no lee tablas ajenas.
- D-FL3: todo queda en local: ramas `feature/fl-*` en worktrees, integradas en `feature/finish-local`; sin push ni PR hasta que el responsable lo decida.
- D-FL4: migraciones nuevas con versión de marca de tiempo reservada por paquete (ver abajo).

## Migraciones reservadas

| Paquete | Versión |
|---|---|
| triage | `V20261026_0100` |
| requests-ext (prioridad, calificación) | `V20261026_0200` |
| admin-ops (configuración) | `V20261026_0300` |
| ola 2 (cierre automático, si hace falta) | `V20261026_0400` |
| datos demo `seed-dev` | `V1001` en adelante |

## Tareas

Ola 1 (en paralelo, un worktree por paquete):

- [ ] FL1 — triage: `POST /triage/suggestions`, CRUD `/admin/triage-rules` — ruta: delegada (worktree `wt-triage`)
- [ ] FL2 — requests-ext: calificación, cambio de prioridad, campos extra de `RequestDetail` y `RequestSummary`, `activeRequests` de técnicos — ruta: delegada (`wt-requests-ext`)
- [x] FL3 — admin-ops: `GET /admin/audit`, `GET/PUT /admin/config` y puerto de configuración del sistema — ruta: delegada (`wt-admin-ops`). Integrada en `a4f0e63` (commits `4a3d297`, `52336ab`, `5f628a9`). Evidencia en su rama: `./mvnw verify` 391/391, frontend 175/175, lint, typecheck y build OK. Puerto de lectura: `shared.application.SystemSettingsProvider#current()` (fila única tipada `system_settings`); historial vía `requests.application.RequestHistoryQuery`.
- [ ] FL4 — dashboard: `GET /dashboard/indicators`, `GET /dashboard/team` sobre el puerto `RequestAnalyticsQuery` — ruta: delegada (`wt-dashboard`)
- [x] FL5 — infra: healthcheck del backend, variables en Compose, prueba de humo con Compose, ejecución local de las pruebas de integración pendientes, base de Playwright — ruta: delegada (`wt-infra`). Integrada en `83db761` (commits `8ec7e6f`..`8fb991c`). Evidencia: `./mvnw verify` 377/377; `infra/smoke.sh` OK; Playwright 3/3 y axe 0 violaciones en login; frontend 173/173. Corrige el volumen de evidencias sin permisos de escritura para el usuario no root. Pendiente: ver la primera ejecución del job `smoke` en CI; nginx maestro sigue como root.

Ola 2 (tras integrar la ola 1):

- [ ] FL6 — cierre automático a las 72 h (`AUTOMATICA`, lee `autoCloseHours`, notifica)
- [ ] FL7 — filtros avanzados y paginación del listado en el servidor
- [ ] FL8 — datos demo `seed-dev` para todas las tablas

Ola 3:

- [ ] FL9 — pruebas de punta a punta con Playwright contra el stack de Compose sin mocks, accesibilidad con axe
- [ ] FL10 — guía de levantamiento y documentos de contrato actualizados; verificación final

## Criterios de aceptación

- `./mvnw verify` (con Testcontainers) y `pnpm lint`, `typecheck`, `test`, `build` en verde en `feature/finish-local`.
- `docker compose -f infra/docker-compose.yml --env-file infra/.env up -d --build` levanta todo con healthchecks en verde y todas las pantallas funcionan con `VITE_USE_MOCKS=false`.

## Progreso y evidencia

(se completa por tarea con el commit y las verificaciones observadas)

## Siguiente paso

Lanzar la ola 1.
