# Contrato de API (v1)

Base: `/api/v1`. JSON en UTF-8. Fechas ISO-8601 en UTC. Errores: `application/problem+json` con `type`, `title`, `status`, `detail` y, en validación, `errors: { campo: mensaje }`.

## Autenticación

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /auth/register` | Público | `{ fullName, email, password, requesterType }` | `201` `UserSummary` · `400` validación · `409` correo ya registrado |
| `POST /auth/login` | Público | `{ email, password }` | `200` `TokenResponse` + cookie `refresh_token` · `401` credenciales inválidas · `429` al superar el límite de intentos, con cabecera `Retry-After` (segundos); por defecto 5 fallos por email+IP y 20 por email en 15 minutos, configurable con `app.auth.login-limit.{max-failures-per-client,max-failures-per-email,window,max-tracked-keys}` |
| `POST /auth/refresh` | Cookie `refresh_token` | — | `200` `TokenResponse` + cookie rotada · `401` |
| `POST /auth/logout` | Cookie `refresh_token` | — | `204`, cookie eliminada y token revocado |
| `GET /me` | Token | — | `200` `{ id, email, fullName, role, areaId }` |

Reglas de registro: `email` termina en `@unmsm.edu.pe`; `password` de 8 a 72 caracteres con al menos una letra y un dígito; `requesterType` ∈ `ESTUDIANTE`, `DOCENTE`, `ADMINISTRATIVO`; `fullName` de 3 a 150 caracteres.

Cookie `refresh_token`: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`.

## Catálogos

| Método y ruta | Acceso | Respuesta |
|---|---|---|
| `GET /catalogs` | Token | `200` `Catalogs` |
| `GET /users/technicians?areaId={id}` | Rol `SUPERVISOR` | `200` `[ { id, fullName } ]` técnicos activos del área |

## Solicitudes

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /requests` | Rol `SOLICITANTE` | `{ categoryId, locationId, description, priorityId }` | `201` `RequestDetail` · `400` |
| `GET /requests?status=&page=0&size=20` | Token | — | `200` `Page<RequestSummary>` filtrado por rol |
| `GET /requests/{code}` | Token, con visibilidad | — | `200` `RequestDetail` · `404` si no existe o no es visible |
| `POST /requests/{code}/transitions` | Token, según la tabla de transiciones | `{ to, reason?, technicianId? }` | `200` `RequestDetail` · `400` falta un dato obligatorio · `403` rol no autorizado · `409` transición inválida |

Visibilidad: `SOLICITANTE` ve las propias; `SUPERVISOR` las de su área; `TECNICO` las asignadas a él; `ADMINISTRADOR` todas.

Transiciones (`to`, quién, requisito):

| Desde | Hacia | Quién | Requisito |
|---|---|---|---|
| `REGISTRADA` | `EN_EVALUACION` | Supervisor del área | — |
| `EN_EVALUACION` | `ASIGNADA` | Supervisor del área | `technicianId` de un técnico del área |
| `ASIGNADA` | `ASIGNADA` | Supervisor del área | `technicianId` y `reason` (reasignación) |
| `ASIGNADA` | `EN_ATENCION` | Técnico asignado | — |
| `EN_ATENCION` | `RESUELTA` | Técnico asignado | `reason` (comentario de solución) |
| `RESUELTA` | `CERRADA` | Solicitante dueño | — (motivo de cierre `CONFIRMADA`) |
| `RESUELTA` | `EN_ATENCION` | Solicitante dueño | `reason` |
| `REGISTRADA`, `EN_EVALUACION` | `CERRADA` | Supervisor del área | `reason` ∈ `NO_PROCEDE`, `DUPLICADA` |

## Administración: usuarios y estados

Acceso: solo rol `ADMINISTRADOR`. Otro rol responde `403`; sin token, `401`. Los usuarios se identifican por `id`.

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `GET /admin/users` | — | `200` `[AdminUser]` ordenados por `fullName` |
| `POST /admin/users` | `AdminUserBody` | `201` `AdminUser` con `temporaryPassword` · `400` validación · `409` correo ya registrado |
| `PUT /admin/users/{id}` | `AdminUserBody` | `200` `AdminUser` · `400` · `404` · `409` correo ya registrado o cambio de rol propio |
| `PATCH /admin/users/{id}/active` | `{ active: boolean }` | `200` `AdminUser` · `404` · `409` si el administrador intenta desactivarse a sí mismo |
| `GET /admin/states` | — | `200` `[ { order, status, description } ]`, los 6 estados en orden (solo lectura) |

`AdminUserBody`: `{ fullName, email, role, areaId, requesterType? }`.

Reglas de validación (`400` con `errors: { campo: mensaje }`):

- `fullName` de 3 a 150 caracteres; `email` termina en `@unmsm.edu.pe` y se guarda en minúsculas.
- `role` ∈ `SOLICITANTE`, `TECNICO`, `SUPERVISOR`, `ADMINISTRADOR`.
- `areaId` es obligatorio y debe ser un área existente y activa para `TECNICO` y `SUPERVISOR`; para `SOLICITANTE` y `ADMINISTRADOR` debe ser `null`.
- `requesterType` ∈ `ESTUDIANTE`, `DOCENTE`, `ADMINISTRATIVO`; solo aplica a `SOLICITANTE` (por defecto `ESTUDIANTE`) y se ignora en los demás roles.

Contraseña inicial: el servidor genera una contraseña temporal (12 caracteres con letras y dígitos), la guarda como hash BCrypt y la devuelve **una sola vez** en `temporaryPassword` del `201`. Ningún otro endpoint la devuelve. El usuario la reemplaza con `PUT /auth/password`.

Protecciones para no dejar el sistema sin administradores: el administrador autenticado no puede desactivar su propia cuenta ni cambiar su propio rol (`409`).

Desactivar una cuenta (`active: false`) no la borra: el login responde `401` (mismo mensaje que credenciales inválidas), se revocan sus tokens de renovación y el token de acceso vigente deja de servir como máximo a los 15 minutos de su emisión. Reactivarla (`active: true`) restablece el acceso con la misma contraseña. Cambiar el rol o el área de un usuario se refleja en su token en el siguiente login o renovación.

`PUT` no modifica `active` ni la contraseña.

## Administración: catálogos

Acceso: solo rol `ADMINISTRADOR` (`403` otro rol, `401` sin token). Los cambios se ven en `GET /catalogs`, que solo lista áreas y categorías activas.

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `POST /admin/areas` | `{ name }` | `201` `{ id, name }` · `400` · `409` ya existe un área activa con ese nombre |
| `DELETE /admin/areas/{id}` | — | `204` (se desactiva) · `404` no existe o ya está inactiva · `409` tiene categorías activas o técnicos/supervisores activos |
| `POST /admin/categories` | `{ name, areaId }` | `201` `{ id, name, areaId }` · `400` (`areaId` inexistente o inactiva) · `409` ya existe una categoría activa con ese nombre |
| `DELETE /admin/categories/{id}` | — | `204` (se desactiva) · `404` no existe o ya está inactiva |
| `PUT /admin/priorities/{id}` | `{ slaHours }` | `200` `{ id, name, slaHours }` · `400` · `404` |
| `POST /admin/locations` | `{ faculty, pavilion, room }` | `201` `{ id, name, kind: 'AMBIENTE', parentId }` · `400` · `409` el ambiente ya existe en ese pabellón |
| `DELETE /admin/locations/{id}` | — | `204` · `404` · `409` no es un ambiente o tiene solicitudes |

Reglas:

- Nombres sin espacios sobrantes: área y categoría de 1 a 100 caracteres; facultad, pabellón y ambiente de 1 a 150. La comparación de nombres no distingue mayúsculas ni tildes (colación de MySQL).
- **Áreas y categorías no se borran, se desactivan**: las solicitudes y usuarios que las referencian las conservan. Crear un área o categoría con el nombre de una inactiva la reactiva (la categoría, con el `areaId` enviado) y responde `201` con su `id` original.
- Una categoría se puede desactivar aunque tenga solicitudes: deja de ofrecerse al registrar, y las solicitudes existentes la mantienen.
- `slaHours`: entero de 1 a 8760. El cambio aplica a las solicitudes que se registren después; **no recalcula el vencimiento de las ya registradas**.
- `POST /admin/locations` reutiliza la facultad y el pabellón si ya existen con ese nombre y crea los niveles que falten.
- Solo se eliminan ambientes sin solicitudes; si su pabellón y su facultad quedan sin hijos ni solicitudes, también se eliminan.

## Triage

> Estado: acordado, **por implementar** en el backend (release candidate, semanas 10–12). El frontend ya lo consume contra la API simulada con estas mismas formas.

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /triage/suggestions` | Rol `SOLICITANTE` | `{ description }` | `200` `{ categoryId, priorityId }`, o `200` con cuerpo `null` si ninguna regla coincide · `400` |
| `GET /admin/triage-rules` | Rol `ADMINISTRADOR` | — | `200` `[TriageRule]` ordenadas por `id` |
| `POST /admin/triage-rules` | Rol `ADMINISTRADOR` | `TriageRuleBody` | `201` `TriageRule` · `400` |
| `PUT /admin/triage-rules/{id}` | Rol `ADMINISTRADOR` | `TriageRuleBody` | `200` `TriageRule` · `400` · `404` |
| `DELETE /admin/triage-rules/{id}` | Rol `ADMINISTRADOR` | — | `204` · `404` |

`TriageRuleBody = { keywords: string[], categoryId, priorityId, active? }` (`active` por defecto `true`).
`TriageRule = { id, keywords, categoryId, categoryName, priorityId, priorityName, active }`.

Reglas:

- `keywords`: de 1 a 20 palabras o frases, cada una de 2 a 50 caracteres sin espacios sobrantes; se guardan en minúsculas y sin repetir. `categoryId` debe ser una categoría activa y `priorityId` una prioridad existente (`400` con `errors` en el campo).
- Sugerencia (spec §5.1): se normalizan la descripción y las palabras clave (minúsculas, sin tildes). Una palabra clave coincide si aparece en la descripción como palabra o frase completa. Para cada regla **activa** cuya categoría siga activa se cuentan sus palabras clave que coinciden; gana la de **más coincidencias** y, si hay empate, la de menor `id` (la más antigua). Con cero coincidencias en todas, la respuesta es `null`.
- La descripción se acepta con 1 a 2000 caracteres; el frontend pide la sugerencia cuando hay al menos 8.
- La sugerencia no se guarda: el solicitante puede aceptarla o cambiarla, y `POST /requests` recibe lo que finalmente elija.

## Dashboard (indicadores)

> Estado: acordado, **por implementar** en el backend (release candidate, semanas 10–12). El frontend ya lo consume contra la API simulada con estas mismas formas.

| Método y ruta | Acceso | Respuesta |
|---|---|---|
| `GET /dashboard/indicators?range=7\|30\|mes&areaId=` | Roles `SUPERVISOR` y `ADMINISTRADOR` | `200` `DashboardIndicators` · `400` `range` inválido · `403` otro rol |
| `GET /dashboard/team` | Rol `SUPERVISOR` | `200` `[ { id, fullName, assigned, inProgress, overdue, capacity } ]` · `403` otro rol |

Alcance y periodo:

- `SUPERVISOR`: siempre su área; `areaId` se ignora. `ADMINISTRADOR`: todas las áreas, o solo `areaId` si se envía. `areaName` es el nombre del área filtrada, o `null` para todas.
- `range` (por defecto `30`): `7` y `30` son los últimos 7 o 30 días contando hoy; `mes` va desde el día 1 del mes actual. Los días se cuentan en hora de Lima (`America/Lima`). `rangeDays` es la cantidad de días del periodo.
- Salvo `dueSoon`, todo se calcula sobre las **solicitudes registradas en el periodo** dentro del alcance.

`DashboardIndicators = { rangeDays, areaName, kpis, byCategory, byPriority, byTechnician, timeline, slaCompliancePct, dueSoon }`, con `kpis = { registered, pending, attended, resolvedPct, avgAttentionHours, overdue }` y cada KPI `{ value, deltaPct, series }`:

| Indicador del enunciado | Campo | Definición |
|---|---|---|
| Solicitudes registradas | `kpis.registered` | Registradas en el periodo |
| Solicitudes pendientes | `kpis.pending` | De ellas, las que hoy no están `RESUELTA` ni `CERRADA` |
| Solicitudes atendidas | `kpis.attended` | De ellas, las que llegaron a `RESUELTA` (estén resueltas o ya cerradas); un cierre por `NO_PROCEDE` o `DUPLICADA` no cuenta |
| Porcentaje de solicitudes resueltas | `kpis.resolvedPct` | `attended / registered × 100`, con un decimal; `0` si no hay registradas |
| Tiempo promedio de atención | `kpis.avgAttentionHours` | Promedio, en horas con un decimal, desde el registro hasta la primera vez que pasó a `RESUELTA`, entre las atendidas; `0` si no hay atendidas |
| Solicitudes vencidas | `kpis.overdue` | De ellas, las marcadas `overdue` (misma regla que `GET /requests`: no llegaron a `RESUELTA` antes de `dueAt`) |
| Solicitudes por categoría | `byCategory` | `[ { label: nombre de la categoría, value } ]`, de mayor a menor |
| Solicitudes por prioridad | `byPriority` | `[ { label, value } ]` en el orden de las prioridades, incluidas las que tienen `0` |
| Solicitudes por responsable | `byTechnician` | `[ { label: técnico asignado, value } ]`, de mayor a menor; las no asignadas van como `"Sin asignar"` |

Además:

- `deltaPct`: variación porcentual del `value` respecto del periodo anterior de la misma duración, con un decimal; `0` si el periodo anterior vale `0`.
- `series`: el valor del KPI para cada día del periodo (`rangeDays` elementos, del más antiguo al más reciente), para los mini gráficos.
- `timeline`: solicitudes registradas por día del periodo (`rangeDays` elementos).
- `slaCompliancePct`: de las atendidas, porcentaje que llegó a `RESUELTA` antes de `dueAt`, con un decimal; `0` si no hay atendidas.
- `dueSoon`: hasta 10 `RequestSummary` del alcance que hoy no están `RESUELTA` ni `CERRADA` y vencen en las próximas 24 horas, ordenadas por `dueAt` (no depende del periodo).
- `GET /dashboard/team`: técnicos activos del área del supervisor, con sus solicitudes en `ASIGNADA` (`assigned`), en `EN_ATENCION` (`inProgress`) y abiertas con `dueAt` pasado (`overdue`); `capacity` es la carga de referencia por técnico, configurable con `app.dashboard.technician-capacity` (por defecto `8`).
- El módulo `dashboard` lee las solicitudes y su historial mediante un puerto de consulta del módulo `requests` (por acordar con su responsable al implementarlo); no escribe en sus tablas.

## Reglas de acceso reutilizables

Usar siempre estas reglas en lugar de reimplementar las comprobaciones en cada módulo. Están en `pe.edu.unmsm.servicios.shared.domain` (Java puro, sin dependencias de Spring).

El usuario autenticado se obtiene en los controladores con `CurrentUser.from(jwt)` (`shared.infrastructure.security`), que devuelve `AuthenticatedUser(long id, String email, String fullName, String role, Long areaId)`. `areaId` solo tiene valor para `SUPERVISOR` y `TECNICO`.

| Método de `AccessPolicy` | Devuelve `true` cuando |
|---|---|
| `isOwner(user, ownerId)` | el usuario es el dueño del recurso (`user.id() == ownerId`) |
| `isSupervisorOfArea(user, areaId)` | el usuario es `SUPERVISOR` del área indicada |
| `isAssignedTechnician(user, technicianId)` | el usuario es `TECNICO` y es el técnico asignado |
| `isAdministrator(user)` | el usuario es `ADMINISTRADOR` |
| `canViewRequest(user, requesterId, areaId, technicianId)` | se cumple la regla de visibilidad de solicitudes de este contrato |

Cuando una solicitud no es visible para el usuario, la API responde `404` (no `403`), para no revelar que existe.

## Tipos

```ts
type Role = 'SOLICITANTE' | 'TECNICO' | 'SUPERVISOR' | 'ADMINISTRADOR'
type RequesterType = 'ESTUDIANTE' | 'DOCENTE' | 'ADMINISTRATIVO'
type RequestStatus = 'REGISTRADA' | 'EN_EVALUACION' | 'ASIGNADA' | 'EN_ATENCION' | 'RESUELTA' | 'CERRADA'

UserSummary   = { id, fullName, email, role, areaId: number | null }
AdminUser     = UserSummary & { areaName: string | null, requesterType: RequesterType | null, active: boolean,
                  temporaryPassword?: string }   // solo en la respuesta del POST
StateInfo     = { order, status: RequestStatus, description }
TokenResponse = { accessToken, expiresIn, user: UserSummary }
Catalogs      = { areas: [{ id, name }], categories: [{ id, name, areaId }],
                  priorities: [{ id, name, slaHours }], locations: [{ id, name, kind, parentId }] }
RequestSummary = { code, categoryName, locationName, priorityName, status, createdAt, dueAt, overdue }
RequestDetail  = RequestSummary & { description, requester: { id, fullName }, area: { id, name },
                  technician: { id, fullName } | null, closureReason: string | null,
                  history: [{ fromStatus, toStatus, actorName, reason, createdAt }],
                  allowedTransitions: RequestStatus[] }
Page<T>        = { items: T[], page, size, total }
```

`allowedTransitions` lo calcula el servidor para el usuario que consulta; el frontend solo muestra botones para esos estados.
