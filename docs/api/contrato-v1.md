# Contrato de API (v1)

Base: `/api/v1`. JSON en UTF-8. Fechas ISO-8601 en UTC. Errores: `application/problem+json` con `type`, `title`, `status`, `detail` y, en validación, `errors: { campo: mensaje }`.

## Autenticación

| Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|
| `POST /auth/register` | Público | `{ fullName, email, password, requesterType }` | `201` `UserSummary` · `400` validación · `409` correo ya registrado |
| `POST /auth/login` | Público | `{ email, password }` | `200` `TokenResponse` + cookie `refresh_token` · `401` credenciales inválidas · `429` tras 5 intentos fallidos en 15 minutos, con cabecera `Retry-After` (segundos) |
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

## Tipos

```ts
type Role = 'SOLICITANTE' | 'TECNICO' | 'SUPERVISOR' | 'ADMINISTRADOR'
type RequesterType = 'ESTUDIANTE' | 'DOCENTE' | 'ADMINISTRATIVO'
type RequestStatus = 'REGISTRADA' | 'EN_EVALUACION' | 'ASIGNADA' | 'EN_ATENCION' | 'RESUELTA' | 'CERRADA'

UserSummary   = { id, fullName, email, role, areaId: number | null }
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
