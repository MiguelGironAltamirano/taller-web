# Extensiones del contrato de API para el diseño final

Complementa [`contrato-v1.md`](contrato-v1.md) (contrato del MVP). Todo lo de este documento **ya está simulado con MSW** en `frontend/src/mocks/` y lo consume el frontend. Antes de implementar cada endpoint se acuerda aquí (o se mueve a `contrato-v1.md`) con el módulo dueño.

> **Nota (2026-10-17):** el contrato completo de comentarios, evidencias, notificaciones y cambio de contraseña (módulo AngelCarrasco) está en [`collaboration-notifications-v1.md`](collaboration-notifications-v1.md). Los endpoints correspondientes en la tabla de abajo ya están acordados; se reproduce aquí solo para referencia cruzada.

**Ya implementado en el backend** (módulos `catalog`, `requests`, `workflow`, `collaboration` y `satisfaction`; ver [`contrato-v1.md`](contrato-v1.md#solicitudes)):

- Los campos añadidos a `RequestSummary` (`description`, `requesterName`, `areaName`, `technicianName`, `facultyName`, `pavilionName`, `roomName`), que también devuelve `RequestDetail`, `size` hasta 500 en `GET /requests`, `totalPages` en `Page` y `GET /requests/counts`.
- En `RequestDetail`: `allowedTransitions` y `allowedActions` calculados por usuario, `comments`, `evidences`, `solution` y `rating`.
- `HistoryEntry.note`, `ClosureReason.AUTOMATICA` (con el proceso de [cierre automático](#cierre-automático)), `TransitionBody.closureReason` y `activeRequests` en `GET /users/technicians`.
- `PATCH /requests/{code}/priority` y `POST /requests/{code}/rating`.

`CreateRequestBody.evidences` no se implementa: los adjuntos se suben con `POST /requests/{code}/evidences` después de crear la solicitud.

Convenciones: las mismas del contrato v1 (fechas ISO-8601 UTC, errores `application/problem+json` con `errors: { campo: mensaje }`, `404` cuando el recurso no es visible).

## Campos añadidos a tipos existentes

| Tipo | Campos nuevos | Para qué |
|---|---|---|
| `RequestSummary` | `description`, `requesterName`, `areaName`, `technicianName`, `facultyName`, `pavilionName`, `roomName` | Mostrar y buscar en las bandejas sin pedir el detalle de cada solicitud |
| `RequestDetail` | `allowedActions`, `comments`, `evidences`, `solution`, `rating` | Botones de flujo, pestañas del detalle y tarjeta de satisfacción |
| `HistoryEntry` | `note?` | Entradas que no cambian de estado (p. ej. `Prioridad Alta → Prioridad Media`) |
| `ClosureReason` | `AUTOMATICA` | Cierre automático a las 72 h |
| `TransitionBody` | `closureReason?: 'NO_PROCEDE' \| 'DUPLICADA'` | Distinguir los dos cierres sin resolver; el texto libre va en `reason` |
| `CreateRequestBody` | `evidences?: [{ fileName, sizeBytes }]` | Metadatos de adjuntos (la subida del archivo irá en un endpoint aparte) |
| Técnicos (`GET /users/technicians`) | `activeRequests` | Mostrar la carga al asignar |

`allowedActions` es la lista de acciones de interfaz que el servidor permite al usuario: `eval`, `assign`, `reassign`, `prio`, `noproc`, `dup`, `start`, `resolve`. Completa a `allowedTransitions`, que no puede expresar el cambio de prioridad ni distinguir los dos cierres.

## Endpoints nuevos

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `PATCH /requests/{code}/priority` | `SUPERVISOR` del área | `{ priorityId }` | **Implementado.** `200` `RequestDetail` con `dueAt` recalculado · `409` si ya está resuelta o cerrada · `400` si la prioridad no existe o es la actual |
| `POST /requests/{code}/comments` | Token, con visibilidad | `{ text }` (1–1000) | `201` `CommentDto` (el comentario creado); el frontend recarga la lista con `GET /comments`. Notifica a la otra parte. Ver [`collaboration-notifications-v1.md §3`](collaboration-notifications-v1.md#3-comentarios) |
| `POST /requests/{code}/rating` | `SOLICITANTE` dueño | `{ score: 1..5, comment? }` | `200` `RequestDetail` con la calificación (no cambia el estado: la solicitud ya se cerró con `POST /transitions {to:CERRADA}`; la calificación es opcional, spec §6) · `409` si no está `CERRADA` por confirmación o ya fue calificada. **Implementado** (comentario opcional, máx. 500 caracteres) |
| `PUT /auth/password` | Token | `{ currentPassword, newPassword }` | `204` · `400` con `errors` |
| `GET /notifications` | Token | — | `200` `[ { id, text, createdAt, read, requestCode } ]` |
| `PATCH /notifications/{id}/read` | Token | — | `204` |
| `POST /notifications/read-all` | Token | — | `204` |
| `POST /triage/suggestions` | `SOLICITANTE` | `{ description }` | `200` `{ categoryId, priorityId }` o `null` sin coincidencias; regla con más coincidencias (spec §5.1) |
| `GET /dashboard/indicators?range=7\|30\|mes&areaId=` | `SUPERVISOR` (su área), `ADMINISTRADOR` | — | `200` `DashboardIndicators` (ver abajo) |
| `GET /dashboard/team` | `SUPERVISOR` | — | `200` `[ { id, fullName, assigned, inProgress, overdue, capacity } ]` |

`DashboardIndicators`: `{ rangeDays, areaName, kpis: { registered, pending, attended, resolvedPct, avgAttentionHours, overdue }, byCategory, byPriority, byTechnician, timeline, slaCompliancePct, dueSoon }`. Cada KPI es `{ value, deltaPct, series }` (variación respecto del periodo anterior y serie para el mini-gráfico). Los valores `deltaPct` de los mocks son fijos de ejemplo.

> Triage (`/triage/suggestions`, `/admin/triage-rules*`) ya está implementado en el backend según [`contrato-v1.md`](contrato-v1.md#triage); la sugerencia queda solo para `SOLICITANTE`, que es quien registra solicitudes. El dashboard (`/dashboard/indicators`, `/dashboard/team`) tiene contrato acordado en [`contrato-v1.md`](contrato-v1.md#dashboard-indicadores), con la definición de cada indicador; aún falta su backend.

### Administración (`ADMINISTRADOR`)

> Usuarios (`/admin/users*`), estados (`/admin/states`) y catálogos (`/admin/areas`, `/admin/categories`, `/admin/priorities`, `/admin/locations`) ya están acordados e implementados según [`contrato-v1.md`](contrato-v1.md#administración-usuarios-y-estados) (usuarios con contraseña temporal; áreas y categorías se desactivan en lugar de borrarse). Auditoría (`/admin/audit`) y configuración (`/admin/config`) también están implementadas según [`contrato-v1.md`](contrato-v1.md#administración-auditoría-y-configuración). Las filas de abajo para esas rutas quedan como referencia histórica.

| Método y ruta | Cuerpo | Notas |
|---|---|---|
| `GET /admin/users`, `POST /admin/users`, `PUT /admin/users/{id}` | `{ fullName, email, role, areaId }` | `areaId` obligatorio para `TECNICO` y `SUPERVISOR`; correo `@unmsm.edu.pe` único |
| `PATCH /admin/users/{id}/active` | `{ active }` | Desactivar no borra al usuario |
| `POST /admin/areas`, `DELETE /admin/areas/{id}` | `{ name }` | `409` si tiene categorías |
| `POST /admin/categories`, `DELETE /admin/categories/{id}` | `{ name, areaId }` | `409` si hay solicitudes con la categoría |
| `PUT /admin/priorities/{id}` | `{ slaHours }` | Entero mayor a 0; no recalcula solicitudes ya registradas |
| `POST /admin/locations`, `DELETE /admin/locations/{id}` | `{ faculty, pavilion, room }` | Crea los niveles que falten; `409` si hay solicitudes en el ambiente |
| `GET /admin/states` | — | Solo lectura (spec §5) |
| `GET /admin/triage-rules`, `POST`, `PUT /{id}`, `DELETE /{id}` | `{ keywords[], categoryId, priorityId, active? }` | **Implementado.** Gestión de reglas de triage; ver [`contrato-v1.md`](contrato-v1.md#triage) |
| `GET /admin/audit?q=` | — | **Implementado.** Eventos derivados del historial de cambios (máx. 40, más recientes primero) |
| `GET /admin/config`, `PUT /admin/config` | `{ autoCloseHours, emailNotifications, triageSuggestions }` | **Implementado.** `autoCloseHours` entero mayor a 0 |

#### Lectura de la configuración desde otros módulos

La configuración se guarda en la tabla `system_settings` (una sola fila tipada, migración `V20261026_0300`) y la administra el módulo `admin`. Los demás módulos la leen mediante un puerto compartido, sin depender de clases de `admin`:

- Puerto: `pe.edu.unmsm.servicios.shared.application.SystemSettingsProvider`, con el método `SystemSettings current()`.
- Valor: `pe.edu.unmsm.servicios.shared.domain.SystemSettings` (`autoCloseHours`, `emailNotifications`, `triageSuggestions`; `autoCloseHours` siempre es mayor a 0).
- Uso: inyectar `SystemSettingsProvider` en el constructor del caso de uso o del job (por ejemplo, el cierre automático de `workflow` usa `current().autoCloseHours()` y las sugerencias de triage consultan `current().triageSuggestions()`). En las pruebas unitarias basta con una lambda: `() -> new SystemSettings(72, false, true)`.
- Cada llamada lee el valor vigente en la base de datos; no se debe guardar en caché para que los cambios del administrador se apliquen de inmediato.
- Se ubica en `shared` porque las reglas de ArchUnit permiten que cualquier capa `application` dependa de `shared.application`, y así ningún módulo depende de la infraestructura ni de los casos de uso de `admin`. La única implementación es `JdbcSystemSettingsStore` (infraestructura de `admin`).
- Agregar un parámetro nuevo implica una migración que añada la columna, el campo en `SystemSettings` y su validación en `ManageSystemSettings`.

## Pendiente de definir

- ~~Subida y descarga real de evidencias~~ **Resuelto:** ver [`collaboration-notifications-v1.md §4`](collaboration-notifications-v1.md#4-evidencias). Subida multipart `POST /requests/{code}/evidences`; descarga autenticada `GET /requests/{code}/evidences/{id}`.
- ~~Cambio de contraseña~~ **Resuelto:** ver [`collaboration-notifications-v1.md §2`](collaboration-notifications-v1.md#2-cambio-de-contraseña).
- ~~Notificaciones~~ **Resuelto:** ver [`collaboration-notifications-v1.md §5`](collaboration-notifications-v1.md#5-notificaciones).
- ~~Cierre automático a las 72 h~~ **Resuelto:** ver [Cierre automático](#cierre-automático).
- ~~Filtros avanzados y paginación del listado en el servidor~~ **Resuelto:** ver [`contrato-v1.md` · Listado de solicitudes](contrato-v1.md#listado-de-solicitudes). `GET /requests` filtra (estado múltiple, área, categoría, prioridad, técnico, sin asignar, vencidas, texto y rango de fechas), ordena con una lista cerrada y pagina; `GET /requests/counts` da los contadores de la bandeja. La bandeja del frontend consulta página a página; Inicio, calendario, reportes y la paleta de comandos siguen pidiendo hasta 500 porque agregan sobre todo lo visible (candidatos a endpoints de agregación propios).
- Exportación a PDF de reportes y dashboard.

## Cierre automático

Lo ejecuta el backend (módulo `workflow`); no hay endpoint ni acción de usuario.

- **Qué cierra:** las solicitudes en `RESUELTA` cuyo solicitante no confirmó ni rechazó la solución durante al menos `autoCloseHours` horas (configuración del administrador, `72` por defecto; se lee en cada ejecución). El plazo se cuenta desde la **última entrada a `RESUELTA`** registrada en el historial, no desde la creación: si el solicitante rechaza y el técnico vuelve a resolver, el plazo empieza de nuevo. Los cambios que no cambian de estado (p. ej. prioridad) no lo reinician.
- **Resultado:** `status = CERRADA`, `closureReason = AUTOMATICA`; se conservan el técnico y `solution`. Se agrega una entrada de historial `RESUELTA → CERRADA` sin actor (`request_history.actor_id = NULL`, migración `V20261026_0400`) que las lecturas muestran con `actorName = "Sistema"` (detalle y auditoría) y `reason = "Cierre automático: el solicitante no confirmó la solución en <N> h"`.
- **Calificación:** una solicitud cerrada automáticamente no se puede calificar (`POST /requests/{code}/rating` responde `409`); solo se califica tras `CONFIRMADA`.
- **Notificaciones:** se publica el mismo evento de cambio de estado que en las transiciones de usuario. Como no hay actor, no se excluye a nadie: reciben la notificación el solicitante, el técnico asignado y los supervisores activos del área, con el texto `"La solicitud <código> se cerró automáticamente porque la solución no se confirmó a tiempo."` (ver [`collaboration-notifications-v1.md §5`](collaboration-notifications-v1.md#5-notificaciones)).
- **Concurrencia e idempotencia:** cada solicitud se cierra en su propia transacción, con la fila bloqueada y volviendo a comprobar el estado y el plazo. Si el solicitante confirma o rechaza al mismo tiempo, o si otra instancia del backend ya la cerró, no se hace nada. Un error en una solicitud no detiene a las demás; se reintenta en la siguiente ejecución.
- **Programación (propiedades de Spring):**

| Propiedad | Variable de entorno | Valor por defecto | Descripción |
|---|---|---|---|
| `app.workflow.auto-close.enabled` | `APP_WORKFLOW_AUTO_CLOSE_ENABLED` | `true` | Activa el proceso periódico (en las pruebas está en `false`) |
| `app.workflow.auto-close.interval` | `APP_WORKFLOW_AUTO_CLOSE_INTERVAL` | `PT15M` | Espera entre ejecuciones (duración ISO-8601). Solo define cuánto después del plazo se cierra la solicitud |
| `app.workflow.auto-close.initial-delay` | — | `PT1M` | Espera tras el arranque antes de la primera ejecución |
| `app.workflow.auto-close.batch-size` | — | `100` | Máximo de solicitudes cerradas por ejecución; el resto queda para la siguiente |

## Semántica de `POST /requests/{code}/transitions` (módulo workflow)

- Orden de validación: `409` (transición fuera de la tabla del spec §6) → `403` (el usuario no puede ejecutarla) → `400` (datos faltantes o inválidos).
- `closureReason` (`NO_PROCEDE` | `DUPLICADA`) distingue el cierre sin resolver; también se acepta el valor en `reason` al estilo del contrato original.
- `GET /users/technicians?areaId=` (solo supervisor, de su propia área) devuelve además `activeRequests` (asignadas + en atención).
- El detalle expone `allowedTransitions` y `allowedActions` calculadas por usuario; el frontend usa `allowedActions` para los botones y solo las deriva de `allowedTransitions` si una respuesta no las trae.
