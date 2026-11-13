# Feature: satisfaction

## Objective
Backend de la encuesta de satisfacción (módulo de Lita): calificación opcional de 1 a 5 después de confirmar la solución y cierre automático a las 72 horas sin respuesta del solicitante.

## Sources
- Spec: `docs/specs/2026-10-14-servicios-universitarios-design.md` §6 (Resuelta → Cerrada, calificación opcional, cierre a las 72 h).
- Contrato: `docs/api/ui-extensions.md` (`POST /requests/{code}/rating`, `ClosureReason AUTOMATICA`, sección «Cierre automático»); `docs/api/collaboration-notifications-v1.md §5` (puerto `NotificationService`).
- Plan: `plan/lita.md` §3 y §5.

## Scope
In: endpoint de calificación, calificación en el detalle de la solicitud, cierre automático con historial y notificación, pruebas.
Out: consulta de calificaciones para el dashboard (por definir con AngelCruz), `autoCloseHours` configurable desde administración, conexión y pruebas del frontend (Tarea 2).

## Tasks
- [x] Migraciones: tabla `ratings` (una por solicitud, puntaje 1–5), `request_history.actor_id` admite nulo (entradas del sistema), `AUTOMATICA` en `ck_service_requests_closure`.
- [x] Dominio `Rating`: puntaje 1–5, comentario opcional recortado (vacío = sin comentario), máximo 1000 caracteres.
- [x] `RateRequest`: solo el solicitante dueño (`404` para el resto, también para el personal que ve la solicitud); solo `CERRADA` con `CONFIRMADA` (`409`); una calificación por solicitud (`409`); la fila de la solicitud queda bloqueada durante la operación.
- [x] `POST /api/v1/requests/{code}/rating` → `200` con `RequestDetail`; `400` con `errors.score` / `errors.comment`.
- [x] `RequestDetail.rating` en el módulo `requests`: `null` o `{ score, comment }`.
- [x] Historial: entradas sin actor humano se muestran como `"Sistema"` (antes el `join` interno las ocultaba).
- [x] `AutoCloseResolvedRequests` + `AutoCloseScheduler` (cada 15 min, `app.satisfaction.auto-close-cron`): plazo desde el último paso a `RESUELTA`; cada solicitud en su propia transacción; guardia de estado contra la respuesta simultánea del solicitante; incrementa `version` (bloqueo optimista de JPA); notifica al solicitante sin bloquear el cierre si la notificación falla.
- [x] Pruebas unitarias: `RatingTest`, `RateRequestTest`, `AutoCloseResolvedRequestsTest`.
- [x] Pruebas de integración con MySQL (Testcontainers): `SatisfactionApiTests`, `AutoCloseApiTests`.
- [ ] Revisión de otro integrante en el PR (toca `requests`: `RequestDetailView`, `JdbcRequestQueries`, `RequestDtos`; y `ClosureReason`).

## Decisions
- **Calificar no cierra la solicitud.** El plan original de Lita lo proponía, pero el contrato acordado, el frontend y el spec §6 separan confirmar (transición del workflow) y calificar (opcional, posterior). Se siguió el contrato y se ajustó `plan/lita.md`.
- **Respuesta `200` con el detalle**, no `201`: así lo define el contrato y lo consume `features/satisfaction/api.ts`.
- **El personal recibe `404`**, no `403`: el endpoint es solo del solicitante dueño y no se revela más de lo necesario (misma regla que el resto de la API).
- **Plazo desde el último paso a `RESUELTA`** y no desde `updated_at`: otros cambios no deben reiniciarlo, y un rechazo seguido de una nueva resolución sí.
- **Fechas en UTC**: se escriben como `LocalDateTime` en UTC, igual que el resto del esquema (`hibernate.jdbc.time_zone=UTC`); con `Timestamp` se habrían guardado en la zona de la JVM.
- **`@EnableScheduling` en `SatisfactionConfig`** y desactivado en las pruebas (`app.satisfaction.auto-close-cron=-`) para que una ejecución programada no interfiera con sus datos.

## Acceptance criteria
- `./mvnw verify` en verde, incluidas `ArchitectureTest` y las pruebas de los demás módulos.
- Las pruebas obligatorias de `plan/lita.md` §5 sobre calificación y cierre automático tienen su prueba.

## Evidence
- Ver el PR de `feature/lita-satisfaction-quality` (resultado de `./mvnw verify`).
