# Feature: satisfaction

## Objective
Backend de la encuesta de satisfacción (módulo de Lita): calificación opcional de 1 a 5 después de confirmar la solución y cierre automático a las 72 horas sin respuesta del solicitante.

## Sources
- Spec: `docs/specs/2026-10-14-servicios-universitarios-design.md` §6 (Resuelta → Cerrada, calificación opcional, cierre a las 72 h).
- Contrato: `docs/api/ui-extensions.md` (`POST /requests/{code}/rating`, `ClosureReason AUTOMATICA`, sección «Cierre automático»); `docs/api/collaboration-notifications-v1.md §5` (puerto `NotificationService`).
- Plan: `plan/lita.md` §3 y §5.

## Scope
In: endpoint de calificación, calificación en el detalle de la solicitud, cierre automático con historial y notificación, pantallas de satisfacción conectadas al backend real, pruebas.
Out: consulta de calificaciones para el dashboard (por definir con AngelCruz).

## Estado tras la integración con `finish-local` (PR #19, 2026-11-14)
Miguel unificó las implementaciones duplicadas (ver `odd/tasks/finish-local.md`, FL13). Lo que cambió respecto de lo que describe este archivo:
- **Calificación:** se conservó la de este módulo (`SatisfactionController`, `RateRequest`, `RatingRepository`/`RatingQueries`); el detalle la lee por el puerto `RequestRatings` de `requests`. Las pruebas de API se portaron a la implementación unificada.
- **Cierre automático:** ya no vive en `satisfaction`. Corre un solo planificador, `AutoCloseJob` en `workflow` (`AutoCloseUnconfirmedRequests`), que lee `autoCloseHours` de la configuración del administrador y notifica al solicitante, al técnico y a los supervisores. Se eliminaron `AutoCloseScheduler`, `AutoCloseResolvedRequests` y `JdbcResolvedRequestStore`; `AutoCloseApiTests` ahora está en `workflow`.
- **Solución propuesta:** el backend envía `solution` en el detalle y el frontend la usa directamente; se eliminó `lib/solution.ts` (el respaldo desde el historial).
- **Plazo configurable:** `RequestDetail.autoCloseAt` indica cuándo se cierra; la tarjeta de cierre automático ya no fija "72 horas".
- `closureReason` pasó a ser obligatorio en `RequestDetailView` del frontend.

## Tasks
- [x] Migraciones: tabla `ratings` (una por solicitud, puntaje 1–5), `request_history.actor_id` admite nulo (entradas del sistema), `AUTOMATICA` en `ck_service_requests_closure`.
- [x] Dominio `Rating`: puntaje 1–5, comentario opcional recortado (vacío = sin comentario), máximo 1000 caracteres.
- [x] `RateRequest`: solo el solicitante dueño (`404` para el resto, también para el personal que ve la solicitud); solo `CERRADA` con `CONFIRMADA` (`409`); una calificación por solicitud (`409`); la fila de la solicitud queda bloqueada durante la operación.
- [x] `POST /api/v1/requests/{code}/rating` → `200` con `RequestDetail`; `400` con `errors.score` / `errors.comment`.
- [x] `RequestDetail.rating` en el módulo `requests`: `null` o `{ score, comment }`.
- [x] Historial: entradas sin actor humano se muestran como `"Sistema"` (antes el `join` interno las ocultaba).
- [x] `AutoCloseResolvedRequests` + `AutoCloseScheduler` (cada 15 min, `app.satisfaction.auto-close-cron`): plazo desde el último paso a `RESUELTA`; cada solicitud en su propia transacción; guardia de estado contra la respuesta simultánea del solicitante; incrementa `version` (bloqueo optimista de JPA); notifica al solicitante sin bloquear el cierre si la notificación falla. *Reemplazado en la integración por `AutoCloseJob` de `workflow` (ver arriba).*
- [x] Pruebas unitarias: `RatingTest`, `RateRequestTest` (las de cierre automático pasaron a `workflow`: `AutoCloseUnconfirmedRequestsTest`, `AutoCloseJobTests`).
- [x] Pruebas de integración con MySQL (Testcontainers): `SatisfactionApiTests`, `AutoCloseApiTests` (esta última ahora en `workflow`).
- [x] Frontend: `SatisfactionPanel` decide qué mostrar en el detalle (la página solo lo monta). Resuelta → `ResolvedCard` para el solicitante dueño; Cerrada → `ClosedCard`: calificación en solo lectura (solicitante y personal), formulario para calificar después de confirmar (o reintentar si falló) y aviso del cierre automático.
- [x] Frontend: la «Solución propuesta» se toma del historial (`Solución: …`) porque el backend no envía `solution`; `closureReason` llega al detalle del dominio. *Desde la integración el backend envía `solution` y el frontend la usa directamente.*
- [x] Frontend (PR #16): `ClosedCard` muestra la «Solución aplicada» también en la solicitud cerrada (antes desaparecía al confirmar), con pruebas de regresión.
- [x] `StarRating` en modo solo lectura (`role="img"`, «4 de 5 estrellas»), con prueba.
- [x] Pruebas con Testing Library y MSW: `features/satisfaction/satisfaction.test.tsx` (confirmar con y sin calificación, fallo de la calificación, fallo del cierre, rechazo con y sin motivo válido, calificación cerrada, calificar después, `409`, personal, cierre automático, no procede).
- [x] Verificación manual en navegador contra el backend real (Docker): confirmar con 4 estrellas y comentario; confirmar sin calificar y calificar después; vista móvil a 375 px; cierre automático por el scheduler.
- [ ] Revisión de otro integrante en el PR (toca `requests`: `RequestDetailView`, `JdbcRequestQueries`, `RequestDtos`; y `ClosureReason`).

## Decisions
- **Calificar no cierra la solicitud.** El plan original de Lita lo proponía, pero el contrato acordado, el frontend y el spec §6 separan confirmar (transición del workflow) y calificar (opcional, posterior). Se siguió el contrato y se ajustó `plan/lita.md`.
- **Respuesta `200` con el detalle**, no `201`: así lo define el contrato y lo consume `features/satisfaction/api.ts`.
- **El personal recibe `404`**, no `403`: el endpoint es solo del solicitante dueño y no se revela más de lo necesario (misma regla que el resto de la API).
- **Plazo desde el último paso a `RESUELTA`** y no desde `updated_at`: otros cambios no deben reiniciarlo, y un rechazo seguido de una nueva resolución sí.
- **Fechas en UTC**: se escriben como `LocalDateTime` en UTC, igual que el resto del esquema (`hibernate.jdbc.time_zone=UTC`); con `Timestamp` se habrían guardado en la zona de la JVM.
- **`@EnableScheduling` en `SatisfactionConfig`** y desactivado en las pruebas (`app.satisfaction.auto-close-cron=-`) para que una ejecución programada no interfiera con sus datos.

- **La solución propuesta se deriva del historial en el frontend** en lugar de añadir `solution` al backend: el campo pertenece al módulo `workflow`/`requests`; si algún día se envía, el frontend lo usa primero. *Superada: el backend ya envía `solution` (PR #19).*
- **`closureReason` opcional en `RequestDetailView`** del frontend, para no tocar los datos de prueba de otros módulos. *Superada: ahora es obligatorio (PR #19).*

## Known issues (otros módulos)
- ~~`GET /api/v1/notifications` responde `500` en `dev`~~: corregido en la integración (`NotificationsController` usa `Jwt` + `CurrentUser.from(jwt)`, FL9a de `finish-local`).

## Acceptance criteria
- `./mvnw verify` en verde, incluidas `ArchitectureTest` y las pruebas de los demás módulos.
- Las pruebas obligatorias de `plan/lita.md` §5 sobre calificación y cierre automático tienen su prueba.

## Evidence
- PR #15 (`feature/lita-satisfaction-quality`), fusionado el 2026-11-14 con el CI en verde.
- Integración en PR #19: `./mvnw clean verify` 591/591 y 26/26 E2E según `odd/tasks/finish-local.md` (FL14).
