# Manual técnico — `catalog` y `requests`

Módulo de Sevan. Cubre el backend (paquetes `catalog` y `requests`) y el frontend (`features/requests`). El contrato de la API está en [`docs/api/contrato-v1.md`](../api/contrato-v1.md) y [`docs/api/ui-extensions.md`](../api/ui-extensions.md); el seguimiento, en [`odd/tasks/catalog-requests-backend.md`](../../odd/tasks/catalog-requests-backend.md).

## 1. Responsabilidad

- **`catalog`:** consulta de áreas, categorías, prioridades (con su SLA) y el árbol de ubicaciones Facultad → Pabellón → Ambiente. Solo lectura; devuelve únicamente áreas y categorías activas.
- **`requests`:** registro de solicitudes con código `SOL-AAAA-NNNNNN`, listado y detalle según el rol, y marca de vencidas.

Quedan fuera: transiciones de estado (`workflow`, Angela), comentarios y evidencias (`collaboration`) y satisfacción (Lita). `requests` solo ofrece el puerto `AllowedTransitions` y las tablas que esos módulos usan.

## 2. Estructura

Arquitectura hexagonal, verificada por `ArchitectureTest`.

```
catalog/   domain · application (CatalogLookup, GetCatalogs) · infrastructure (persistence, web)
requests/  domain · application · infrastructure (persistence, web, RequestsConfig)
```

| Pieza | Papel |
|---|---|
| `ServiceRequest`, `RequestCode`, `Sla`, `RequestStatus` | Reglas de dominio: descripción de 20–2000 caracteres (sin contar espacios de los bordes), formato del código, vencimiento y vencida |
| `CreateRequest` | Valida categoría, ubicación (debe ser un ambiente) y prioridad; deriva el área de la categoría; calcula el vencimiento |
| `GetRequest` | Aplica `AccessPolicy.canViewRequest`; si no es visible lanza `RequestNotFoundException` (`404`) |
| `ListRequests` | Acota la paginación, valida el rango de fechas y delega en `RequestQueries` |
| `RequestQueries` (`JdbcRequestQueries`) | Lado de lectura: una consulta con joins por página o detalle |
| `RequestRepository` (`JpaRequestRepository`) | Lado de escritura con entidades JPA y `@Version` |
| `RequestCodeGenerator` (`JdbcRequestCodeGenerator`) | Código correlativo atómico |
| `RequestFilter` | Filtros opcionales del listado |

## 3. Endpoints

| Endpoint | Acceso |
|---|---|
| `GET /api/v1/catalogs` | Token |
| `POST /api/v1/requests` | Rol `SOLICITANTE` |
| `GET /api/v1/requests` | Token, filtrado por rol |
| `GET /api/v1/requests/{codigo}` | Token con visibilidad |

Los errores siguen RFC 7807 (`RequestsExceptionHandler`); los de validación incluyen `errors` con un mensaje por campo.

## 4. Decisiones de diseño

- **Código correlativo.** Un único `INSERT … ON DUPLICATE KEY UPDATE` con `LAST_INSERT_ID(expr)` sobre `request_code_sequences` toma el bloqueo de la fila del año y suma. El bloqueo dura hasta el commit, así que las creaciones concurrentes se serializan y un rollback devuelve el número (los códigos quedan consecutivos). Exige transacción (`Propagation.MANDATORY`) porque `LAST_INSERT_ID` es por conexión.
- **Año del código y días de los filtros: hora de Lima** (`America/Lima`), no UTC. El 31 de diciembre a las 23:30 en Lima sigue siendo el año que termina.
- **Fechas en UTC.** Las columnas guardan la hora UTC (`hibernate.jdbc.time_zone=UTC`). En las consultas JDBC las fechas se enlazan como `LocalDateTime` en UTC, para no depender de la zona de la JVM.
- **Lectura con SQL, escritura con JPA.** El listado y el detalle ya traen los nombres con joins, sin cargar entidades ni relaciones.
- **Visibilidad.** `RequestScope` replica en SQL la regla de `AccessPolicy`: solicitante las propias, técnico las asignadas, supervisor las de su área, administrador todas. Un supervisor sin área o un rol desconocido no ven nada. Los filtros se suman con `AND`, así que nunca amplían lo visible.
- **Paginación acotada, no rechazada:** `size` entre 1 y 500, `page` desde 0. El frontend pide 500 para mostrar contadores por estado sin esperas.
- **Vencida:** no está en `RESUELTA` ni `CERRADA` y `dueAt` ya pasó. La misma regla existe en `Sla.isOverdue` (Java) y en el filtro `overdue` de `JdbcRequestQueries` (SQL, constante `FINISHED`): si cambia una, hay que cambiar la otra.

## 5. Filtros del listado

`GET /requests?status=&from=&to=&areaId=&overdue=&page=&size=` (detalle en el contrato).

- `from` y `to` son días inclusive en hora de Lima. `RequestFilter` los convierte a un intervalo semiabierto de instantes: `created_at >= inicio(from)` y `created_at < inicio(to + 1 día)`.
- `to` anterior a `from` lanza `InvalidRequestException` (`400`, campo `to`). Una fecha mal formada o un estado inexistente también dan `400`, por la conversión de parámetros de Spring.
- `overdue=true` deja las vencidas; `overdue=false`, las demás.

## 6. Frontend (`features/requests`)

- **`api.ts`:** `useRequestRows(server)` pide el listado completo (500) con `from`, `to` y `areaId`; la clave de caché incluye esos filtros. `useRequestDetail` deriva las acciones de la interfaz desde `allowedTransitions` si el servidor no envía `allowedActions`.
- **`containers/`:** `RequestListPage`, `NewRequestPage`, `RequestDetailPage`, además de `HomePage` y `CalendarPage` (extras del diseño). `useListParams` guarda el estado del listado en la URL.
- **Filtros:** los de fecha y área (`ServerFilters`) los resuelve el servidor; estado, categoría, prioridad, técnico, texto, vistas guardadas y "vencidas" se aplican en el cliente (`lib/filters.ts`) sobre ese resultado, así los contadores se calculan sin otra petición. El área solo la ofrece la interfaz al administrador.
- **Formulario:** validación con zod en `schemas/newRequest.ts` (misma regla de 20–2000 caracteres que el backend). Ubicación en tres listas encadenadas; el front resuelve el `locationId` del ambiente.
- **Mocks (MSW, `src/mocks`):** replican visibilidad y filtros del backend; `VITE_USE_MOCKS=true` los activa. Los datos de los mocks no son los de la semilla real.

## 7. Pruebas

| Nivel | Dónde | Qué cubre |
|---|---|---|
| Dominio | `RequestCodeTest`, `ServiceRequestTest`, `SlaTest` | Código, descripción y SLA |
| Casos de uso | `CreateRequestTest`, `GetAndListRequestsTest` | Validaciones, visibilidad, paginación y filtros (con dobles) |
| Integración (MySQL 8.4 con Testcontainers) | `CatalogApiTests`, `RequestsApiTests` | Lista de pruebas obligatorias del plan, incluidas diez creaciones concurrentes y los filtros por fecha, área y vencidas |
| Arquitectura | `ArchitectureTest` | Dependencias entre capas |
| Frontend (Vitest) | `new-request.test.tsx`, `filters.test.ts`, `mocks/handlers.test.ts` | Crear una solicitud, filtros del cliente y reglas de los mocks |

Las pruebas de integración necesitan Docker en marcha.

## 8. Cómo ejecutar

- Backend: JDK 25 (`JAVA_HOME` apuntando al 25) y `./mvnw verify` desde `backend/`.
- Frontend: `pnpm test`, `pnpm lint`, `pnpm typecheck` y `pnpm build` desde `frontend/`.
- Entorno completo: [`docs/guia-levantamiento.md`](../guia-levantamiento.md).
