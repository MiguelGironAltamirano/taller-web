# Feature: catalog-requests-backend

## Objective
Implementar el backend de los módulos `catalog` y `requests` del plan de Sevan: catálogos de consulta y registro, listado y detalle de solicitudes con código, SLA y visibilidad por rol.

## Problem / Why
El frontend ya existía contra una API simulada. El esquema y la semilla estaban en `dev` (Miguel), pero no había endpoints de catálogos ni de solicitudes, que son la base de los módulos de Angela (flujo), AngelCarrasco (colaboración), AngelCruz (dashboard) y Lita (satisfacción).

## Sources
- Spec: `docs/specs/2026-10-14-servicios-universitarios-design.md` §5.1, §6, §7, §8
- Plan: `plan/sevan.md` §2, §3 y §5
- Contrato: `docs/api/contrato-v1.md` y `docs/api/ui-extensions.md`

## Scope
In: `GET /api/v1/catalogs`, `POST /api/v1/requests`, `GET /api/v1/requests`, `GET /api/v1/requests/{codigo}`.
Out: transiciones de estado (Angela), comentarios y evidencias (AngelCarrasco), técnicos (`GET /users/technicians`). Los filtros avanzados del servidor se añadieron después (FL7).

## Tasks
- [x] `catalog`: dominio, `CatalogLookup` (puerta para otros módulos), `GetCatalogs` y `GET /catalogs` — solo áreas y categorías activas
- [x] `requests` dominio: `ServiceRequest` (registro, descripción de 20–2000 caracteres sin contar espacios de los bordes), `RequestCode`, `Sla` (vencimiento y vencida), excepciones
- [x] `requests` aplicación: `CreateRequest`, `GetRequest` (regla de visibilidad con `AccessPolicy`, `404` si no es visible), `ListRequests` (paginación acotada), puerto `AllowedTransitions`
- [x] `requests` infraestructura: entidades JPA con `@Version`, código correlativo atómico (`request_code_sequences`), consultas de lectura con joins, controlador, DTOs y manejador de errores RFC 7807
- [x] Pruebas unitarias (dominio y casos de uso con dobles) y de integración con MySQL real (Testcontainers)
- [x] Verificación manual de punta a punta: backend real + frontend sin mocks en Chrome
- [ ] Revisión de otro integrante y PR a `dev`
- [x] Filtros avanzados, orden y paginación del listado en el servidor (FL7, rama `feature/fl-filters`): `RequestListCriteria` valida `status` múltiple, `areaId`, `categoryId`, `priorityId`, `technicianId`, `unassigned`, `overdue`, `q`, `from`/`to`, `sort` (`newest`, `oldest`, `due`, `priority`), `page` y `size` (1–500) con `400` por campo; `GET /requests/counts`; índices `V20261026_0500`. Evidencia: `RequestListCriteriaTest` (10), `GetAndListRequestsTest` (11), `RequestListFiltersApiTests` (13) y `RequestsApiTests` (22) en verde; frontend migrado (bandeja paginada en el servidor, contadores, mocks MSW con la misma semántica)
- [ ] Sustituir `NoAllowedTransitions` cuando exista el módulo de workflow

## Decisions
- **Código correlativo:** un solo `INSERT … ON DUPLICATE KEY UPDATE` con `LAST_INSERT_ID(expr)` toma el bloqueo de la fila del año y suma; el bloqueo se mantiene hasta el commit, así que las creaciones concurrentes se serializan y un rollback devuelve el número (los códigos quedan consecutivos). Exige estar dentro de una transacción (`Propagation.MANDATORY`), porque `LAST_INSERT_ID` es por conexión.
- **Año del código:** zona horaria de Lima, no UTC (el 31 de diciembre a las 23:30 en Lima sigue siendo el año que termina).
- **Lectura con SQL y escritura con JPA:** el listado y el detalle son una consulta con joins que ya trae los nombres; el catálogo es de solo lectura. Las escrituras de solicitudes usan entidades JPA con control de concurrencia optimista para los cambios de estado futuros.
- **Visibilidad del listado:** `RequestScope` replica en SQL la regla `AccessPolicy.canViewRequest`; un supervisor sin área o un rol desconocido no ven nada.
- **Paginación:** `size` entre 1 y 500 y `page` desde 0. Inicialmente se acotaban en silencio; desde FL7 un valor fuera de rango responde `400` con `errors` por parámetro, porque ajustar en silencio ocultaba errores del cliente y el resto de filtros ya se valida así.
- **Filtros del listado (FL7):** se conserva el sobre `Page` (`items`, `page`, `size`, `total`) y se añade `totalPages`, un cambio aditivo que no rompe a los consumidores; los parámetros llegan como texto y `RequestListCriteria.parse` los valida juntos para informar todos los errores en un solo `400`. Cada filtro es un fragmento SQL fijo con `?`; el orden sale de una lista cerrada (`RequestSort`) con desempate por `id`; `%` y `_` de la búsqueda son literales (escape `!`). Los filtros se suman al alcance del rol con AND, así que nunca amplían la visibilidad. Los contadores de la bandeja viven en `GET /requests/counts` porque dejan de poder calcularse con una sola página.
- **Vencida:** una solicitud abierta (hasta `EN_ATENCION`) pasada su fecha de vencimiento; las `RESUELTA` y `CERRADA` nunca.
- **`allowedTransitions`:** puerto vacío por ahora; el frontend solo muestra botones para lo que el servidor devuelva.

## Acceptance criteria
- `./mvnw verify` pasa con JDK 25 (backend completo).
- Las pruebas obligatorias del plan de Sevan (sección 5) están cubiertas por pruebas de integración con MySQL real.
- Con el backend y el frontend sin mocks se puede iniciar sesión, crear una solicitud y verla en el listado y en el detalle.

## Evidence
- `./mvnw -B verify` (JDK 25, MySQL 8.4 en Testcontainers): **116 pruebas, 0 fallos**, de las cuales 58 son nuevas: 30 unitarias (`RequestCodeTest` 2, `ServiceRequestTest` 9, `SlaTest` 4, `CreateRequestTest` 9, `GetAndListRequestsTest` 6) y 28 de integración (`CatalogApiTests` 6, `RequestsApiTests` 22). `ArchitectureTest` sigue en verde.
- Una prueba de integración detectó un fallo real: `wasNull()` se consultaba después de leer otras columnas y las facultades devolvían `parentId: 0` en lugar de `null`; corregido.
- Navegador (Chrome) con backend real y frontend sin mocks: inicio de sesión como `estudiante@unmsm.edu.pe`; catálogos reales (4 áreas, 7 categorías, 2 facultades); `POST` creó `SOL-2026-000002` consecutivo a la creada antes por `curl`; el contador del menú pasó de 1 a 2; el detalle muestra área "Soporte Informático", vencimiento a 24 h y primer historial.
- Frontend: `pnpm test` 147 pruebas en 22 archivos, `lint`, `typecheck` y `build` limpios.
