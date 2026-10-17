# Feature: admin-catalogs

## Objetivo
Segunda fase del módulo de administración (responsable: AngelCruz), última entrega antes de la presentación parcial: gestión de catálogos desde el backend real, contrato de triage e indicadores, y actualización del documento de la Fase I.

## Fuentes
- `plan/angel-cruz.md` (entregables de las semanas 8 a 10 y pruebas obligatorias), `docs/api/contrato-v1.md`, `docs/api/ui-extensions.md`, `odd/tasks/admin-users.md`.

## Alcance
Dentro: `POST/DELETE /admin/areas`, `POST/DELETE /admin/categories`, `PUT /admin/priorities/{id}`, `POST/DELETE /admin/locations`; contrato de `/triage/*`, `/admin/triage-rules*`, `/dashboard/*`; mock del frontend alineado; HU-26 y evidencia en `docs/fase-1/`. Fuera: backend de triage, dashboard y auditoría (semanas 10 a 12).

## Tareas
- [x] 1 - Contrato de catálogos en `contrato-v1.md`: áreas y categorías se desactivan en lugar de borrarse (las solicitudes y usuarios conservan la referencia) y se reactivan al recrear el nombre; SLA de 1 a 8760 h sin recalcular solicitudes existentes; ubicaciones con creación de niveles faltantes y poda al eliminar el último ambiente.
- [x] 2 - Caso de uso `ManageCatalogs` con el puerto `CatalogAdminStore`, TDD. Evidencia: `ManageCatalogsTest` 17/17; RED = fallo de compilación antes de implementar.
- [x] 3 - API y persistencia JDBC (`JdbcCatalogAdminStore`, mismo estilo que el lado de lectura de `catalog`), TDD. Evidencia: `AdminCatalogsApiTests` 12/12 con Testcontainers; RED observado: 12 de 12 fallaban sin el controlador. Incluye la prueba obligatoria del plan "cambiar el SLA no reescribe las solicitudes registradas" usando la API de solicitudes de Sevan.
- [x] 4 - `ApiClient` (apoyo de pruebas de Sevan): método genérico `send` para PUT, PATCH y DELETE (cambio aditivo).
- [x] 5 - Frontend: el mock modela la desactivación (`active` en áreas y categorías; `GET /catalogs` solo devuelve las activas, como el backend), `409` por nombres repetidos y catálogos en uso, `404` por entradas inexistentes, SLA hasta 8760 en el mock y en el formulario. Evidencia: 4 pruebas nuevas del mock y 1 del formulario; RED observado: 5 fallos antes de implementar.
- [x] 6 - Contrato de triage (algoritmo de coincidencias, desempate por regla más antigua, validación de reglas) y de indicadores (definición de cada uno de los nueve indicadores del enunciado, periodo en hora de Lima, alcance por rol, `deltaPct`, `series`, `dueSoon`, carga del equipo).
- [x] 7 - Documento de la Fase I: HU-26 con sus escenarios y pruebas, estado de las historias, evidencia y PR integrados.

## Verificación (2026-10-17)
- Backend `./mvnw verify` con Testcontainers: 311 pruebas, 0 fallos (incluye las de `auth`, `catalog`, `requests` y `workflow`, y ArchUnit).
- Frontend: 161 pruebas; lint, typecheck y build en verde.
- Prueba de humo con Docker Compose a través de nginx (base limpia, `db/seed-dev`): crear técnico y entrar con la temporal; desactivarlo (`401`); estudiante en `/admin/areas` `403`; área repetida `409`; área con categoría activa `409`; categoría visible en `GET /catalogs` y oculta al desactivarla; área desactivada fuera de `GET /catalogs`; ubicación nueva y poda al eliminar su ambiente; SLA Baja a 6 h sin cambiar el vencimiento de la solicitud existente y aplicado a la nueva; estados en orden.

## Desviaciones
- Una categoría se puede desactivar aunque tenga solicitudes (el borrador de `ui-extensions.md` decía `409`): al ser desactivación y no borrado, las solicitudes conservan su categoría y no hay motivo para bloquearla.
- Los nombres repetidos responden `409` (el mock respondía `400`), igual que el correo repetido de usuarios.
- El área inactiva no se vuelve a ofrecer, pero `AdminAreaEntity` (fase 1) sigue resolviendo el nombre del área de los usuarios aunque esté inactiva, a propósito.

## Pendientes
- Backend de triage y dashboard según el contrato (semanas 10 a 12); al implementar el triage, restringir el mock de `/triage/suggestions` al rol `SOLICITANTE`.
- Auditoría y configuración (HU-26, prioridad C): sin contrato todavía.
- Revisión del PR por Sevan (cambios en `ApiClient`, en el mock de `/catalogs` y en `docs/fase-1/`).
