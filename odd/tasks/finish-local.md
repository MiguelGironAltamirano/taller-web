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

> FL13: al integrar `origin/dev` estas versiones se renumeraron a `V20261114_0100`..`_0500` (mismo orden), después de las migraciones compartidas `V20261113_*`. Los textos de FL1–FL7 citan los nombres originales.

## Tareas

Ola 1 (en paralelo, un worktree por paquete):

- [x] FL1 — triage: `POST /triage/suggestions`, CRUD `/admin/triage-rules` — ruta: delegada (worktree `wt-triage`). Integrada en `e159b46` (commits `8540473`..`7acc18c`; conflicto en `ui-extensions.md` resuelto a mano). Evidencia en su rama: `./mvnw verify` 398/398, frontend 179/179. Pendiente trasladado: FL6 conecta `triageSuggestions` y FL8 agrega reglas demo.
- [x] FL2 — requests-ext: calificación, cambio de prioridad, campos extra de `RequestDetail` y `RequestSummary`, `activeRequests` de técnicos — ruta: delegada (`wt-requests-ext`). Integrada en `baad76f` (commits `c120f95`..`4f844fe`, sin conflictos). Evidencia en su rama: `./mvnw verify` 428/428, frontend 173/173. `requests` define puertos de lectura (`AllowedActions`, `RequestCollaboration`, `RequestRatings`) que implementan los módulos dueños; `satisfaction.application.RatingQueries` para lectura de calificaciones; `ClosureReason.AUTOMATICA` listo para FL6.
- [x] FL3 — admin-ops: `GET /admin/audit`, `GET/PUT /admin/config` y puerto de configuración del sistema — ruta: delegada (`wt-admin-ops`). Integrada en `a4f0e63` (commits `4a3d297`, `52336ab`, `5f628a9`). Evidencia en su rama: `./mvnw verify` 391/391, frontend 175/175, lint, typecheck y build OK. Puerto de lectura: `shared.application.SystemSettingsProvider#current()` (fila única tipada `system_settings`); historial vía `requests.application.RequestHistoryQuery`.
- [x] FL4 — dashboard: `GET /dashboard/indicators`, `GET /dashboard/team` sobre el puerto `RequestAnalyticsQuery` — ruta: delegada (`wt-dashboard`). Integrada en `c307590` (commits `ed30908`..`64833cd`; conflicto de imports en `mocks/handlers/insights.ts` resuelto). Evidencia en su rama: `./mvnw verify` 426/426. Tras integrar: frontend typecheck y lint OK, 184/184 pruebas.
- [x] FL5 — infra: healthcheck del backend, variables en Compose, prueba de humo con Compose, ejecución local de las pruebas de integración pendientes, base de Playwright — ruta: delegada (`wt-infra`). Integrada en `83db761` (commits `8ec7e6f`..`8fb991c`). Evidencia: `./mvnw verify` 377/377; `infra/smoke.sh` OK; Playwright 3/3 y axe 0 violaciones en login; frontend 173/173. Corrige el volumen de evidencias sin permisos de escritura para el usuario no root. Pendiente: ver la primera ejecución del job `smoke` en CI; nginx maestro sigue como root.

Ola 2 (tras integrar la ola 1):

- [x] FL6 — cierre automático a las 72 h (`AUTOMATICA`, lee `autoCloseHours`, notifica) y `SuggestTriage` respeta `triageSuggestions` vía `SystemSettingsProvider` — ruta: delegada (`wt-autoclose`). Integrada en `a6099f1` (commits `0599526`..`6750a05`, sin conflictos). Evidencia en su rama: `./mvnw verify` 539/539. El cierre lo registra el sistema con `actor_id` nulo (`V20261026_0400`), mostrado como `Sistema`. Pendiente: `ResolvedCard.tsx` muestra "72 horas" fijo porque el solicitante no puede leer `/admin/config`.
- [x] FL7 — filtros avanzados y paginación del listado en el servidor — ruta: delegada (`wt-filters`). Integrada en `8b0616c` (commits `4d72e26`, `0539be2`, `3e3a718`; conflicto en `ui-extensions.md` resuelto; backend compila tras el merge). Evidencia en su rama: `./mvnw verify` 537/537, frontend 195/195. Nuevo `GET /requests/counts`; `page`/`size` fuera de rango ahora devuelven 400. Pendiente: Inicio, calendario, reportes y paleta siguen pidiendo hasta 500 filas.
- [x] FL8 — datos demo para todas las tablas — ruta: delegada (`wt-seed`). Integrada en `58c3ccb` (commits `5415415`, `6ce1f28`, `6ce67ec`). Migración repetible opcional `db/seed-demo/R__demo_data.sql` (corre después de todas las versionadas; no rompe pruebas que usan `seed-dev`), protegida por `DemoSeedTests`. Evidencia en su rama: `./mvnw verify` 520/520, `infra/smoke.sh` OK, datos visibles por rol a través de nginx. Hallazgo: `GET /notifications` responde 500 con JWT real (`@AuthenticationPrincipal AuthenticatedUser` nulo) → FL9a.

Ola 3:

- [x] FL9a — correcciones de integración: notificaciones con JWT real, datos demo alineados con el cierre automático (actor nulo), `autoCloseAt` en `RequestDetail` en lugar de "72 horas" fijo — ruta: delegada (`wt-fixes`). Integrada en `7122479` (commits `b43bb11`, `368a4d5`, `d460eae`, `ce5fb2f`). Evidencia en su rama: `./mvnw verify` 574/574, frontend 198/198, flujo completo por curl a través de nginx OK, `infra/smoke.sh` OK. FL9 lanzada en `wt-e2e`.
- [x] FL9 — pruebas de punta a punta con Playwright contra el stack de Compose sin mocks, accesibilidad con axe — ruta: delegada (`wt-e2e`). Integrada en `cade3a1` (commits `0c43cda`..`5bd532e`). 26 pruebas E2E por rol (solicitante, supervisor, técnico, administrador, autenticación, accesibilidad), 26/26 dos veces seguidas con `infra/e2e.sh`; axe sin violaciones en 16 páginas. Corrige: evidencias del formulario de nueva solicitud que nunca se subían, agrupación "Hoy/Ayer" de notificaciones por día de Lima, estructura ARIA de tablas, nombres de botones y contraste. CI: el job `smoke` ahora ejecuta `infra/e2e.sh`. Decisión de producto posterior (FL11): el técnico asignado recibe una sola notificación.
- [x] FL10 — guía de levantamiento y documentos de contrato actualizados; verificación final — ruta: inline (orquestador). Cada paquete actualizó `contrato-v1.md`, `ui-extensions.md` y `guia-levantamiento.md`; verificación final abajo.
- [x] FL11 — una sola notificación al técnico asignado — ruta: delegada (un escritor). Commit `1116127`. Decisión de producto: al asignar o reasignar, el técnico recibe solo "Se te asignó la solicitud …", no también "… pasó a Asignada."; el solicitante y los supervisores siguen recibiendo la de cambio de estado. `NotificationRecipients.forAssignmentTransition` excluye al técnico y `CreateNotificationsForTransition` la usa en las asignaciones (prueba `NotificationRecipientsTest`, RED→GREEN). Datos demo: se quitan las 26 filas redundantes "pasó a Asignada." del técnico (una por asignación o reasignación). Documentado en `collaboration-notifications-v1.md §5`. `NotificationsApiTests` ajustada (el técnico ve una sola notificación). Evidencia: `./mvnw verify` 575/575 (0 fallos); `infra/e2e.sh` prueba de humo OK y 26/26 E2E; sin contenedores sobrantes.
- [x] FL12 — texto de reasignación para el técnico nuevo — ruta: delegada (un escritor). Commit `fe397bc`. Decisión de producto: en una reasignación (`ASIGNADA` → `ASIGNADA` con otro técnico) el técnico nuevo recibe "Se te reasignó la solicitud …", como ya decían `collaboration-notifications-v1.md §5` y los datos demo (fila 30166); antes recibía "Se te asignó …" porque `NotificationText.forReassignment` nunca se usaba. `RequestTransitionedEvent` incluye `fromStatus` e `isReassignment()`; `TransitionEvents.of` recibe el estado anterior (`TransitionRequest` y `CloseUnconfirmedRequest`); `CreateNotificationsForTransition` elige el texto. Destinatarios sin cambios (el técnico anterior no recibe notificación). Pruebas: `NotificationsApiTests.reassignmentNotifiesNewTechnicianWithReassignmentText` (RED: recibía "Se te asignó …" → GREEN) y `RequestTransitionedEventTest`. Evidencia: `./mvnw verify` 579/579 (0 fallos); `infra/e2e.sh` prueba de humo OK y 26/26 E2E; sin contenedores sobrantes.
- [x] FL13 — integración con `origin/dev` (PR #14 usuarios `seed-dev`, PR #15 satisfacción de Lita) en la rama nueva `integration/finish-local-dev`; `feature/finish-local` queda intacta (`173ed21`) como respaldo — ruta: inline (resolución de conflictos; cada decisión necesitaba el contexto completo de ambas ramas). Commits: `4ace8af` (merge con 18 conflictos resueltos), `d1aa65c` (migraciones), `9147525` (datos demo). Decisiones:
  - Una implementación por concepto. Calificación: se conserva la nuestra (`SatisfactionController`, `RateRequest`, `RatingRepository`/`RatingQueries`, detalle vía `RequestRatings`); se eliminan `RatingController`, `RatingStore`, `JdbcRatingStore` y `RatingAlreadyExistsException`. Cierre automático: se conserva `AutoCloseJob` (workflow, `autoCloseHours`, notifica a solicitante, técnico y supervisores); se eliminan `AutoCloseScheduler`, `AutoCloseResolvedRequests` y `JdbcResolvedRequestStore`, así que corre un solo planificador. Módulo `requests`: se conserva el nuestro (`solution`, `autoCloseAt`, colaboración).
  - Contrato único de `POST /requests/{code}/rating` (D-FL1 sin cambios): de `dev` se toman el límite de 1000 caracteres del comentario y los mensajes; de la nuestra, el `403` al personal que ve la solicitud (en `dev` era `404`) y el orden 404 → 403 → 409 → 400. Documentado en `docs/api/ui-extensions.md`.
  - Migraciones: las de `origin/dev` quedan idénticas. Las nuestras pasan a `V20261114_0100`..`_0500`; `0200` ya no crea `request_ratings` ni amplía el check de cierre (lo hacen `V20261113_1000` y `_1020`) y agrega `ix_ratings_created`; `0400` ya no vuelve nulo `actor_id` (`V20261113_1010`). Las calificaciones viven en `ratings`; `Rating` pierde `requesterId` (es el solicitante de la solicitud). Una base local que aplicó las versiones `V20261026_*` debe recrearse (`down -v`).
  - Datos demo: `V20261113_1100` ya crea `supervisor.infraestructura`, `supervisor.administrativo`, `docente` y `administrativo`, que chocaban por correo con las filas 101, 103, 110 y 111 de `R__demo_data.sql`. El script ahora los busca por correo (ids `AUTO_INCREMENT`) y no los inserta; su supervisora de Soporte pasa a ser `supervisora.informatica`. Falla de inmediato si falta alguno.
  - Frontend: se adoptan `SatisfactionPanel`, `ClosedCard` (ver la calificación y calificar después) y `StarRating` de solo lectura de Lita; `ResolvedCard` mantiene `autoCloseAt` y usa `solution` de la API (se elimina el respaldo desde el historial); la tarjeta de cierre automático ya no fija "72 horas"; `closureReason` es obligatorio en `RequestDetailView`; el mock MSW responde como el backend.
  - Pruebas portadas de Lita sobre la implementación unificada: 401 sin token, mensajes de 409, `RESUELTA` sin confirmar, mensaje de `score`, límite 1000/1001 (`SatisfactionApiTests`) y solicitud ya confirmada que el cierre automático no toca (`AutoCloseApiTests`). Nueva `DemoSeedTests.reusesTheSeedDevUsersInsteadOfDuplicatingThem`.
  - Fuera del alcance editable: `odd/tasks/satisfaction.md` (de Lita) sigue describiendo su implementación original.
  - Evidencia: `./mvnw clean verify` 584/584 (0 fallos; sin `clean` fallaba porque `target/classes` conservaba las `V20261026_*` borradas); frontend lint, typecheck, typecheck:e2e, build OK y 220/220 pruebas; `E2E_PORT=8094 infra/e2e.sh` (el 8090 lo ocupa otro proyecto local) prueba de humo OK y 26/26 E2E, sin contenedores sobrantes; `git diff --check` limpio y sin marcadores de conflicto; `git merge-tree --write-tree origin/dev integration/finish-local-dev` sin conflictos (`origin/dev` ya es ancestro); las cuatro migraciones `V20261113_*` son idénticas a `origin/dev`. No verificado: CI (sin push).

## Criterios de aceptación

- `./mvnw verify` (con Testcontainers) y `pnpm lint`, `typecheck`, `test`, `build` en verde en `feature/finish-local`.
- `docker compose -f infra/docker-compose.yml --env-file infra/.env up -d --build` levanta todo con healthchecks en verde y todas las pantallas funcionan con `VITE_USE_MOCKS=false`.

## Progreso y evidencia

- Ola 1 integrada en `feature/finish-local` (2026-10-26): `./mvnw verify` 512/512 (0 fallos) y frontend lint, typecheck, build OK con 184/184 pruebas.
- Ola 2 integrada en `feature/finish-local` (`58c3ccb`): `./mvnw verify` 572/572 (0 fallos) y frontend lint, typecheck, build OK con 195/195 pruebas.

- Verificación final en `feature/finish-local` (`cade3a1`, 2026-10-26): `./mvnw verify` 574/574 (0 fallos); frontend lint, typecheck, typecheck:e2e, build OK y 203/203 pruebas; `infra/e2e.sh` (stack desde cero con datos demo) prueba de humo OK y 26/26 E2E; sin contenedores sobrantes.
- No verificado: ejecución en CI (no se hizo push).

## Siguiente paso

Decisiones del responsable: revisar `integration/finish-local-dev` (ya contiene `origin/dev`; `git merge-tree` contra `origin/dev` sin conflictos), hacer push y abrir el PR hacia `dev`. Avisar a Lita de los cambios sobre su módulo (FL13). Decidido (FL11): al ser asignado, el técnico recibe una sola notificación ("Se te asignó la solicitud …"). Pendientes menores: Inicio, calendario, reportes y paleta piden hasta 500 filas; el proceso maestro de nginx corre como root; staging en Dokploy sigue bloqueado por el dominio.
