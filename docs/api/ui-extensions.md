# Extensiones del contrato de API para el diseño final

Complementa [`contrato-v1.md`](contrato-v1.md) (contrato del MVP). Todo lo de este documento **ya está simulado con MSW** en `frontend/src/mocks/` y lo consume el frontend. Antes de implementar cada endpoint se acuerda aquí (o se mueve a `contrato-v1.md`) con el módulo dueño.

**Ya implementado en el backend** (módulos `catalog` y `requests`): los campos añadidos a `RequestSummary` (`description`, `requesterName`, `areaName`, `technicianName`, `facultyName`, `pavilionName`, `roomName`), que también devuelve `RequestDetail`, y `size` hasta 500 en `GET /requests` (el frontend pide la lista completa). `allowedTransitions` existe pero es siempre `[]` hasta que el módulo de workflow lo calcule. **Todo lo demás de este documento sigue sin existir en el backend.**

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
| `PATCH /requests/{code}/priority` | `SUPERVISOR` del área | `{ priorityId }` | `200` `RequestDetail` con `dueAt` recalculado · `409` si ya está resuelta o cerrada |
| `POST /requests/{code}/comments` | Token, con visibilidad | `{ text }` (1–1000) | `201` `RequestDetail`; notifica a la otra parte |
| `POST /requests/{code}/rating` | `SOLICITANTE` dueño | `{ score: 1..5, comment? }` | `200` `RequestDetail` con la calificación (no cambia el estado: la solicitud ya se cerró con `POST /transitions {to:CERRADA}`; la calificación es opcional, spec §6) · `409` si no está `CERRADA` por confirmación o ya fue calificada |
| `PUT /auth/password` | Token | `{ currentPassword, newPassword }` | `204` · `400` con `errors` |
| `GET /notifications` | Token | — | `200` `[ { id, text, createdAt, read, requestCode } ]` |
| `PATCH /notifications/{id}/read` | Token | — | `204` |
| `POST /notifications/read-all` | Token | — | `204` |
| `POST /triage/suggestions` | Token | `{ description }` | `200` `{ categoryId, priorityId }` o `null` sin coincidencias; regla con más coincidencias (spec §5.1) |
| `GET /dashboard/indicators?range=7\|30\|mes&areaId=` | `SUPERVISOR` (su área), `ADMINISTRADOR` | — | `200` `DashboardIndicators` (ver abajo) |
| `GET /dashboard/team` | `SUPERVISOR` | — | `200` `[ { id, fullName, assigned, inProgress, overdue, capacity } ]` |

`DashboardIndicators`: `{ rangeDays, areaName, kpis: { registered, pending, attended, resolvedPct, avgAttentionHours, overdue }, byCategory, byPriority, byTechnician, timeline, slaCompliancePct, dueSoon }`. Cada KPI es `{ value, deltaPct, series }` (variación respecto del periodo anterior y serie para el mini-gráfico). Los valores `deltaPct` de los mocks son fijos de ejemplo.

### Administración (`ADMINISTRADOR`)

| Método y ruta | Cuerpo | Notas |
|---|---|---|
| `GET /admin/users`, `POST /admin/users`, `PUT /admin/users/{id}` | `{ fullName, email, role, areaId }` | `areaId` obligatorio para `TECNICO` y `SUPERVISOR`; correo `@unmsm.edu.pe` único |
| `PATCH /admin/users/{id}/active` | `{ active }` | Desactivar no borra al usuario |
| `POST /admin/areas`, `DELETE /admin/areas/{id}` | `{ name }` | `409` si tiene categorías |
| `POST /admin/categories`, `DELETE /admin/categories/{id}` | `{ name, areaId }` | `409` si hay solicitudes con la categoría |
| `PUT /admin/priorities/{id}` | `{ slaHours }` | Entero mayor a 0; no recalcula solicitudes ya registradas |
| `POST /admin/locations`, `DELETE /admin/locations/{id}` | `{ faculty, pavilion, room }` | Crea los niveles que falten; `409` si hay solicitudes en el ambiente |
| `GET /admin/states` | — | Solo lectura (spec §5) |
| `GET /admin/triage-rules`, `POST`, `PUT /{id}`, `DELETE /{id}` | `{ keywords[], categoryId, priorityId, active? }` | Gestión de reglas de triage |
| `GET /admin/audit?q=` | — | Eventos derivados del historial de cambios (máx. 40) |
| `GET /admin/config`, `PUT /admin/config` | `{ autoCloseHours, emailNotifications, triageSuggestions }` | `autoCloseHours` entero mayor a 0 |

## Pendiente de definir

- Subida y descarga real de evidencias (hoy el front solo envía los metadatos y simula la descarga).
- Cierre automático a las 72 h y marca `AUTOMATICA` (lo ejecuta el backend).
- Filtros avanzados y paginación del listado en el servidor: hoy el front pide hasta 500 solicitudes y filtra en el cliente.
- Exportación a PDF de reportes y dashboard.

## Semántica de `POST /requests/{code}/transitions` (módulo workflow)

- Orden de validación: `409` (transición fuera de la tabla del spec §6) → `403` (el usuario no puede ejecutarla) → `400` (datos faltantes o inválidos).
- `closureReason` (`NO_PROCEDE` | `DUPLICADA`) distingue el cierre sin resolver; también se acepta el valor en `reason` al estilo del contrato original.
- `GET /users/technicians?areaId=` (solo supervisor, de su propia área) devuelve además `activeRequests` (asignadas + en atención).
- El detalle expone `allowedTransitions` calculadas por usuario; el frontend deriva los botones (`allowedActions`) a partir de ellas.
