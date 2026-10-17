# Feature: admin-users

## Objetivo
Primera fase del módulo de administración (responsable: AngelCruz): gestión de usuarios y consulta de estados desde el backend real, conectada a la pantalla de usuarios ya existente en el frontend.

## Fuentes
- `plan/angel-cruz.md` (alcance y pruebas obligatorias), `docs/api/contrato-v1.md` (sección "Administración: usuarios y estados"), `docs/api/ui-extensions.md`.

## Alcance
Dentro: `GET/POST /admin/users`, `PUT /admin/users/{id}`, `PATCH /admin/users/{id}/active`, `GET /admin/states`; contraseña temporal; ajuste del mock y de la pantalla de usuarios. Fuera: CRUD de catálogos (fase 2), triage, indicadores, auditoría y configuración.

## Tareas
- [x] 1 - Contrato de usuarios y estados en `contrato-v1.md`: validación, contraseña temporal devuelta una sola vez, `409` por correo repetido o autogestión (desactivarse o cambiar el propio rol), efectos de desactivar una cuenta. Nota en `ui-extensions.md` para no duplicar la fuente.
- [x] 2 - Dominio y casos de uso (`admin.domain`, `admin.application`), TDD. Evidencia: `UserAccountRulesTest` 6/6, `AdminUserUseCasesTest` 14/14, `TemporaryPasswordGeneratorTest` 51/51 (50 repeticiones). RED = fallo de compilación antes de implementar.
- [x] 3 - Persistencia y API (`admin.infrastructure`), TDD. Evidencia: `AdminUsersApiTests` 12/12 contra MySQL 8.4. RED observado: 11 de 12 fallaban sin los controladores (la restante, `missingUsersAreNotFound`, pasa también sin ellos porque una ruta inexistente da `404`).
- [x] 4 - Revocación de sesiones al desactivar: nuevo `RefreshTokenRepository.revokeAllForUser` en el módulo `auth` (cambio aditivo). Evidencia: `AdminUsersApiTests.deactivationBlocksLoginAndEndsSessionsUntilReactivated` (login y refresh `401`, cero tokens activos en la base).
- [x] 5 - Frontend: tipo `AdminUser.temporaryPassword`, mock alineado al contrato (`409` por correo repetido, área rechazada para solicitante/administrador, autogestión `409`, contraseña temporal), pantalla de usuarios muestra la contraseña en un diálogo y avisa los errores al activar/desactivar; el formulario envía `areaId: null` para roles sin área y valida el nombre de 3 a 150. Evidencia: `users.test.tsx` 2/2, `handlers.test.ts` (4 pruebas de administración nuevas o ajustadas), `forms.test.ts`; RED observado: 7 fallos antes de implementar.

## Verificación (2026-10-17)
- Backend: todas las clases de prueba en verde contra MySQL 8.4.11 local, cada clase con base de datos limpia: 58 pruebas previas (sin regresiones en `auth`, seguridad, migraciones y convenciones) + 83 ejecuciones nuevas. ArchUnit en verde.
- Frontend: 145 pruebas, `lint`, `typecheck` y `build` en verde.
- Prueba de humo contra el backend real (`spring-boot:run` + `db/seed-dev`): el administrador crea un técnico → el técnico ingresa con la contraseña temporal → correo repetido `409` → técnico sin área `400` → desactivado: login y refresh `401` → el administrador no puede desactivarse (`409`) → reactivado: login `200`.

## Desviaciones
- En esta máquina no hay Docker: las pruebas de integración se ejecutaron contra un MySQL 8.4.11 portable, desactivando temporalmente el contenedor de `TestcontainersConfiguration` (cambio revertido, no forma parte del commit). El CI las vuelve a ejecutar con Testcontainers.
- `admin` mapea la tabla `users` con su propia entidad (`AdminUserEntity`) en lugar de usar la de `auth`, que es privada de su paquete; ambas entidades tienen las mismas columnas.
- `AdminAreaEntity` lee `areas` solo para validar el área y mostrar su nombre; se revisará en la fase 2 al acordar con Sevan dónde viven las entidades de catálogos.
- El token de acceso de una cuenta desactivada sigue siendo válido hasta su vencimiento (máximo 15 minutos), según el contrato.

## Pendientes
- Revisión del PR por Miguel (cambio en `RefreshTokenRepository`) y por otro integrante.
- Fase 2: CRUD de catálogos; acordar con Sevan la propiedad de las entidades y `GET /catalogs`.
- Recorrido manual de la pantalla de usuarios en el navegador contra el backend real (verificado por API y por pruebas de interfaz, no en navegador).
