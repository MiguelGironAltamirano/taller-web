# Contrato de API (v1)

Base: `/api/v1`. JSON en UTF-8. Fechas ISO-8601 en UTC. Errores: `application/problem+json` con `type`, `title`, `status`, `detail` y, en validación, `errors: { campo: mensaje }`.

## Autenticación

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /auth/register` | Público | `{ fullName, email, password, requesterType }` | `201` `UserSummary` · `400` validación · `409` correo ya registrado |
| `POST /auth/login` | Público | `{ email, password }` | `200` `TokenResponse` + cookie `refresh_token` · `401` credenciales inválidas · `429` al superar el límite de intentos, con cabecera `Retry-After` (segundos); por defecto 5 fallos por email+IP y 20 por email en 15 minutos, configurable con `app.auth.login-limit.{max-failures-per-client,max-failures-per-email,window,max-tracked-keys}` |
| `POST /auth/refresh` | Cookie `refresh_token` | — | `200` `TokenResponse` + cookie rotada · `401`, con `code: "SESSION_REPLACED"` si la sesión se cerró porque el usuario ingresó en otro dispositivo |
| `POST /auth/logout` | Cookie `refresh_token` | — | `204`, cookie eliminada y sesión cerrada |
| `GET /me` | Token | — | `200` `{ id, email, fullName, role, areaId }` |

Reglas de registro: `email` termina en `@unmsm.edu.pe`; `password` de 8 a 72 caracteres con al menos una letra y un dígito; `requesterType` ∈ `ESTUDIANTE`, `DOCENTE`, `ADMINISTRATIVO`; `fullName` de 3 a 150 caracteres.

Cookie `refresh_token`: `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`.

### Una sola sesión por usuario

Cada usuario tiene como máximo **una sesión abierta**. Gana el último ingreso: iniciar sesión con contraseña (`/auth/login`) o con huella o passkey (`/auth/passkeys/login`) cierra cualquier otra sesión del mismo usuario, en otro dispositivo, otro navegador o una ventana de incógnito.

- **Qué es una sesión.** Nace en el inicio de sesión y la identifica el claim `sid` del token de acceso. Renovarla con `/auth/refresh` rota la cookie pero conserva la misma sesión. Las pestañas y ventanas normales de un mismo navegador comparten la cookie y son, por tanto, **la misma sesión**: no se expulsan entre sí.
- **Cierre inmediato.** Cada petición con token verifica que su sesión siga abierta. Si no lo está, la respuesta es `401` aunque el token no haya vencido. Esto aplica también a `logout`, al cambio de contraseña (que cierra las demás sesiones y conserva la actual) y a la desactivación de una cuenta.
- **Motivo del cierre.** Si la sesión se cerró porque el usuario ingresó en otro lado, el `401`, tanto de cualquier ruta con token como de `/auth/refresh`, incluye `code: "SESSION_REPLACED"` y `detail: "Tu sesión se cerró porque ingresaste desde otro dispositivo"`. En cualquier otro caso el `401` no trae `code`. El frontend consulta `GET /me` cuando la ventana recupera el foco y cada 15 s mientras está visible, de modo que la pantalla anterior vuelve al login con ese aviso aunque muestre datos en caché.
- **Ingresos simultáneos.** Si dos ingresos del mismo usuario llegan a la vez, solo uno queda abierto.
- **Rutas públicas de `/auth`.** `register`, `login`, `refresh`, `logout` y `passkeys/login*` ignoran la cabecera `Authorization`, de modo que un token vencido o cerrado nunca impide renovar la sesión ni volver a ingresar. `PUT /auth/password` sí requiere token.

Inicio de sesión con huella o passkey (WebAuthn): ver [`passkeys-v1.md`](passkeys-v1.md); implementado como propuesta, pendiente de acuerdo con el dueño del módulo `auth`.

## Catálogos

| Método y ruta | Acceso | Respuesta |
|---|---|---|
| `GET /catalogs` | Token | `200` `Catalogs` |
| `GET /users/technicians?areaId={id}` | Rol `SUPERVISOR` (solo su área) | `200` `[ { id, fullName, activeRequests } ]` técnicos activos del área; `activeRequests` cuenta sus solicitudes `ASIGNADA` y `EN_ATENCION` |

## Solicitudes

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /requests` | Rol `SOLICITANTE` | `{ categoryId, locationId, description, priorityId }` | `201` `RequestDetail` · `400` |
| `GET /requests?status=&areaId=&categoryId=&priorityId=&technicianId=&unassigned=&overdue=&q=&from=&to=&sort=&page=0&size=20` | Token | — | `200` `Page<RequestSummary>` filtrado por rol y por los parámetros (ver [Listado de solicitudes](#listado-de-solicitudes)) · `400` parámetro inválido |
| `GET /requests/counts` | Token | — | `200` `RequestCounts` sobre todo lo visible para el usuario |
| `GET /requests/{code}` | Token, con visibilidad | — | `200` `RequestDetail` · `404` si no existe o no es visible |
| `POST /requests/{code}/transitions` | Token, según la tabla de transiciones | `{ to, reason?, technicianId?, closureReason? }` | `200` `RequestDetail` · `400` falta un dato obligatorio · `403` rol no autorizado · `409` transición inválida |
| `PATCH /requests/{code}/priority` | Rol `SUPERVISOR` del área | `{ priorityId }` | `200` `RequestDetail` con `dueAt` recalculado · `400` prioridad inexistente o igual a la actual · `403` · `404` · `409` si está `RESUELTA` o `CERRADA`. El solicitante no ve el cambio (ver [Prioridad que ve el solicitante](#prioridad-que-ve-el-solicitante)) |
| `POST /requests/{code}/rating` | Rol `SOLICITANTE` dueño | `{ score: 1..5, comment? }` | `200` `RequestDetail` con `rating` · `400` · `403` · `404` · `409` si no está `CERRADA` por confirmación o ya fue calificada |

Visibilidad: `SOLICITANTE` ve las propias; `SUPERVISOR` las de su área; `TECNICO` las asignadas a él; `ADMINISTRADOR` todas.

### Prioridad que ve el solicitante

Cada solicitud guarda dos prioridades: la **solicitada** (la que eligió el solicitante al registrarla, que no cambia nunca) y la **vigente** (la que usa el personal; empieza igual y el supervisor del área la puede cambiar con `PATCH /requests/{code}/priority`). El solicitante nunca se entera de ese cambio:

| Dato | `SOLICITANTE` | `SUPERVISOR`, `TECNICO`, `ADMINISTRADOR` |
|---|---|---|
| `priorityName` (lista y detalle) | Prioridad solicitada | Prioridad vigente |
| `dueAt` | **Fecha estimada**: registro + horas del SLA de la prioridad solicitada, fijada al registrar (no cambia aunque el supervisor cambie la prioridad o el administrador las horas del SLA) | Fecha límite según la prioridad vigente |
| `estimatedDue` | `true` | `false` |
| `overdue` | Siempre `false`: pasada la fecha estimada no se muestra como vencida | Regla del SLA |
| `history` | Sin las entradas de cambio de prioridad | Completo |
| `priorityId`, `sort=priority` y `sort=due` en `GET /requests` | Sobre la prioridad solicitada y la fecha estimada | Sobre la prioridad vigente y la fecha límite |
| `overdue` en `GET /requests` | Se ignora | Según la regla |
| `overdue` en `GET /requests/counts` | `0` | Según la regla |

La fecha estimada es una referencia para el solicitante, no un compromiso del SLA: si el supervisor bajó la prioridad, el personal puede atenderla después de esa fecha sin estar vencida. La interfaz la muestra como «Fecha estimada» y, una vez pasada, indica que la solicitud sigue en atención en lugar de marcarla como vencida.

El dashboard, los reportes, el tablero y el calendario son solo del personal y usan la prioridad vigente.

### Listado de solicitudes

`GET /requests` filtra, ordena y pagina en el servidor. Todos los parámetros son opcionales y se combinan con AND; la regla de visibilidad del rol se aplica siempre, de modo que un filtro solo puede reducir el resultado (por ejemplo, un `SOLICITANTE` que envía `technicianId` sigue viendo únicamente sus solicitudes, y un `SUPERVISOR` que envía otra `areaId` obtiene una página vacía).

| Parámetro | Valor | Semántica |
|---|---|---|
| `status` | Uno o varios `RequestStatus`, repetidos (`status=A&status=B`) o separados por comas (`status=A,B`) | El estado es alguno de los indicados |
| `areaId`, `categoryId`, `priorityId`, `technicianId` | Entero positivo | Igualdad con el área, la categoría, la prioridad o el técnico asignado |
| `unassigned` | `true` / `false` | `true`: sin técnico asignado. No se combina con `technicianId` (`400`) |
| `overdue` | `true` / `false` | Misma regla que el campo `overdue`: no llegó a `RESUELTA` ni `CERRADA` y `dueAt` ya pasó. `false` devuelve las que no están vencidas |
| `q` | Texto de hasta 100 caracteres; se recorta | Aparece en el código, la descripción, el nombre del solicitante, la categoría o el ambiente. No distingue mayúsculas ni tildes (colación de MySQL); `%` y `_` se tratan como caracteres literales |
| `from`, `to` | Fecha `AAAA-MM-DD` | Fecha de registro entre ambos días, **inclusive**, en hora de Lima (`America/Lima`); se puede enviar solo uno. `to` no puede ser anterior a `from` (el mismo día sí vale) |
| `sort` | `newest` (por defecto), `oldest`, `due`, `priority` | `newest`/`oldest`: por fecha de registro. `due`: primero las pendientes y, dentro de cada grupo, la fecha límite más próxima. `priority`: de mayor a menor prioridad y luego las más recientes. Cada orden termina en un desempate estable, así las páginas no se solapan |
| `page` | Entero ≥ 0 (por defecto `0`) | Página, empezando en cero. Una página posterior a la última devuelve `items` vacío con el `total` real |
| `size` | Entero entre 1 y 500 (por defecto `20`) | Elementos por página |

- Un valor inválido (estado u orden desconocido, id no numérico, booleano distinto de `true`/`false`, fecha mal formada, `page` negativa, `size` fuera de rango, `to` anterior a `from`) responde `400` `ProblemDetail` con `errors` por parámetro, todos a la vez. `page` y `size` fuera de rango ya no se ajustan en silencio.
- **Decisión documentada (integración de filtros):** `from` y `to` son días de calendario de Lima, no instantes UTC, para que la interfaz envíe directamente lo que elige el usuario en un selector de fecha y el servidor aplique la zona horaria (la misma que usa el año del código). Es la única excepción a la convención de fechas ISO-8601 en UTC, y solo afecta a estos dos parámetros de consulta; las respuestas siguen en UTC.
- `GET /requests/counts` devuelve los contadores de la bandeja sobre todo lo visible para el usuario, sin otros filtros: `total`, `overdue` y `byStatus` con los seis estados (cero si no hay ninguna).

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

## Administración: auditoría y configuración

Acceso: solo rol `ADMINISTRADOR` (`403` otro rol, `401` sin token).

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `GET /admin/audit?q=` | — | `200` `AuditEvent[]` |
| `GET /admin/config` | — | `200` `AppConfig` |
| `PUT /admin/config` | `AppConfig` | `200` `AppConfig` · `400` con `errors.<campo>` |

`AuditEvent`: `{ id, createdAt, actorName, requestCode, detail }`. `AppConfig`: `{ autoCloseHours, emailNotifications, triageSuggestions }`.

Reglas:

- La auditoría se deriva del historial de cambios de las solicitudes (`request_history`): como máximo 40 eventos, del más reciente al más antiguo. `id` es el del registro de historial.
- `detail` tiene la forma `<estado origen> → <estado destino> · <motivo>`, con las etiquetas de estado en español (`Registrada`, `En evaluación`, `Asignada`, `En atención`, `Resuelta`, `Cerrada`). El origen de la creación es `—` y el tramo ` · <motivo>` se omite cuando no hay motivo.
- `q` (opcional) filtra por nombre del actor, código de solicitud o motivo (sin distinguir mayúsculas ni tildes, colación de MySQL), o por la etiqueta del estado de origen o destino. `%` y `_` se tratan como caracteres literales. Vacío lista todo.
- `autoCloseHours` es un entero mayor a 0; `emailNotifications` y `triageSuggestions` son obligatorios. Valores iniciales: `72`, `false`, `true`.
- Efecto inmediato (se leen en cada uso): `autoCloseHours` define el plazo del cierre automático y, con `triageSuggestions = false`, `POST /triage/suggestions` responde siempre `200` con `null`.
- Cada cambio de configuración registra quién lo hizo y cuándo (`updated_by`, `updated_at`).

## Triage

> Estado: acordado e **implementado** en el backend (módulo `triage`, migración `V20261114_0100`). Los mocks del frontend siguen las mismas reglas.

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /triage/suggestions` | Rol `SOLICITANTE` | `{ description }` | `200` `{ categoryId, priorityId }`, o `200` con cuerpo `null` si ninguna regla coincide o si el administrador desactivó las sugerencias (`triageSuggestions = false`) · `400` |
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
- Errores: `400` en formato *problem details* con `errors` por campo (`keywords`, `categoryId`, `priorityId`; `description` en la sugerencia), igual que el resto de la administración; `404` si la regla no existe. En `PUT` el cuerpo reemplaza la regla completa, por lo que `active` omitido vale `true`.
- Detalles de implementación: «palabra completa» significa que la palabra clave no está pegada a otra letra o dígito (los espacios y signos de puntuación separan); los espacios repetidos cuentan como uno; cada palabra clave cuenta una sola vez aunque se repita en el texto; dos palabras clave que solo difieren en tildes se consideran repetidas. Sin coincidencias, el cuerpo es el literal JSON `null` con `Content-Type: application/json`.
- Las migraciones no crean reglas iniciales: se registran desde la administración (o con los datos de demostración). El interruptor `triageSuggestions` de la configuración del sistema aún no se aplica en el backend.

## Dashboard (indicadores)

> Estado: **implementado** en el backend (módulo `dashboard`). La API simulada del frontend aplica las mismas reglas.

| Método y ruta | Acceso | Respuesta |
|---|---|---|
| `GET /dashboard/indicators?range=7\|30\|mes&areaId=` | Roles `SUPERVISOR` y `ADMINISTRADOR` | `200` `DashboardIndicators` · `400` `range` inválido, o `areaId` inexistente o inactiva (con `errors`) · `403` otro rol |
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
- `series` de cada KPI: el mismo cálculo del KPI aplicado a las solicitudes registradas cada día (por ejemplo, `resolvedPct` del día es el porcentaje de las registradas ese día que ya fueron atendidas). Los conteos se devuelven como números (`4.0` equivale a `4`).
- Una solicitud que llegó a `RESUELTA` y luego se reabrió cuenta como pendiente, no como atendida, mientras no vuelva a `RESUELTA` o `CERRADA`; su tiempo de atención se mide siempre hasta la **primera** resolución.
- `dueSoon` incluye las que vencen en `(ahora, ahora + 24 h]`, ordenadas por `dueAt`.
- `GET /dashboard/team` ordena los técnicos por nombre. La carga que muestra el frontend es `assigned + inProgress` frente a `capacity`.
- Un `SUPERVISOR` sin área asignada recibe `403` en ambos endpoints.

Lectura de datos (sin acceso a tablas de otros módulos):

- El módulo `requests` publica el puerto de solo lectura `RequestAnalyticsQuery` (`requests.application`), implementado en `requests.infrastructure.persistence.JdbcRequestAnalyticsQuery`:
  - `List<RequestAnalyticsRow> registeredBetween(Long areaId, Instant from, Instant to)`: solicitudes registradas en `[from, to)`.
  - `List<RequestAnalyticsRow> openRequests(Long areaId)`: solicitudes que hoy no están `RESUELTA` ni `CERRADA`.
  - `areaId` `null` significa todas las áreas; el puerto no aplica reglas de visibilidad, las decide quien lo usa.
- `RequestAnalyticsRow = (code, status, categoryName, priorityName, areaId, areaName, technicianId, technicianName, locationName, createdAt, dueAt, firstResolvedAt)`, donde `firstResolvedAt` es la primera vez que pasó a `RESUELTA` según el historial. Expone `isOpen()`, `isOverdue(now)` (la misma regla `Sla.isOverdue` de `GET /requests`) y `wasAttended()`.
- El módulo `dashboard` obtiene los nombres de áreas y el orden de prioridades con `CatalogLookup`, y los técnicos activos con `TechnicianDirectory` (módulo `workflow`). Todos los cálculos se hacen en Java puro (`dashboard.domain`) con un `Clock` inyectable; no escribe en ninguna tabla.

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
RequestSummary = { code, categoryName, locationName, priorityName, status, createdAt, dueAt, estimatedDue, overdue,
                  description, requesterName, areaName, technicianName, facultyName, pavilionName, roomName }
RequestDetail  = RequestSummary & { requester: { id, fullName }, area: { id, name },
                  technician: { id, fullName } | null, closureReason: ClosureReason | null,
                  history: [{ fromStatus, toStatus, actorName, reason, createdAt, note: string | null }],
                  allowedTransitions: RequestStatus[], allowedActions: WorkflowAction[],
                  comments: [{ id, authorName, text, createdAt }],
                  evidences: [{ id, fileName, kind: 'IMG' | 'PDF', sizeBytes }],
                  solution: string | null, rating: { score, comment: string | null } | null,
                  autoCloseAt: string | null }
ClosureReason  = 'NO_PROCEDE' | 'DUPLICADA' | 'CONFIRMADA' | 'AUTOMATICA'
WorkflowAction = 'eval' | 'assign' | 'reassign' | 'prio' | 'noproc' | 'dup' | 'start' | 'resolve'
Page<T>        = { items: T[], page, size, total, totalPages }
RequestCounts  = { total, overdue, byStatus: { [RequestStatus]: number } }
```

`allowedTransitions` lo calcula el servidor para el usuario que consulta; el frontend solo muestra botones para esos estados.

`allowedActions` también lo calcula el servidor: son las acciones de interfaz detrás de las transiciones que el usuario puede ejecutar (`noproc` y `dup` para el cierre sin resolver) más `prio`, que se ofrece al supervisor del área mientras la solicitud no esté `RESUELTA` ni `CERRADA`. La respuesta del solicitante a una solución (confirmar o rechazar) se lee de `allowedTransitions`.

Otros campos del detalle:

- `solution`: comentario de solución del técnico; se guarda al pasar a `RESUELTA`, se conserva al cerrar y se borra si el solicitante la rechaza.
- `history[].note`: solo en cambios que no cambian de estado; el cambio de prioridad registra `Prioridad Alta → Prioridad Media` y en `reason` el nuevo vencimiento (hora de Lima). El nuevo `dueAt` es la fecha de registro más las horas del SLA de la nueva prioridad. Estas entradas no se muestran al solicitante.
- `comments` y `evidences`: los mismos datos que `GET /requests/{code}/comments` y `GET /requests/{code}/evidences`.
- `autoCloseAt`: solo cuando `status = RESUELTA` (en otro caso `null`). Fecha ISO-8601 (UTC) a partir de la cual el backend puede cerrar automáticamente la solicitud: última entrada a `RESUELTA` + `autoCloseHours` vigente. Lo ve cualquier usuario que vea la solicitud, así el solicitante conoce su plazo sin leer `/admin/config`. El cierre ocurre en la siguiente ejecución del proceso posterior a esa hora.
- `rating`: `null` hasta que el solicitante califica. Calificar no cambia el estado; la solicitud ya se cerró con la transición `RESUELTA → CERRADA`.
- `closureReason = 'AUTOMATICA'`: cierre automático por falta de confirmación. El backend cierra las solicitudes que permanecen en `RESUELTA` al menos `autoCloseHours` horas (configuración del administrador, `72` por defecto) contadas desde la última entrada a `RESUELTA` según el historial. La entrada de historial la registra el sistema: `actorName` es `Sistema` y `reason` es `Cierre automático: el solicitante no confirmó la solución en <N> h`. Una solicitud cerrada así no se puede calificar (`409`). Detalles en [`ui-extensions.md`](ui-extensions.md#cierre-automático).
- `history[].actorName`: nombre del usuario que hizo el cambio, o `Sistema` cuando lo hizo el backend (cierre automático). Lo mismo aplica a `AuditEvent.actorName`.
