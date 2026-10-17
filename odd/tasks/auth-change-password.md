# Feature: auth-change-password

## Objetivo
Implementar `PUT /api/v1/auth/password` (cambio de contraseña con verificación de la actual y revocación de sesiones de renovación) sobre el módulo `auth` de Miguel.

## Fuentes
- `plan/angel-carrasco.md` §2 y §5 (alcance, pruebas obligatorias)
- `docs/api/collaboration-notifications-v1.md` §2 (contrato acordado)
- `docs/fase-1/03-epicas-y-historias.md` HU-05
- Código de referencia: `auth/application/{UserRepository,PasswordHasher,RefreshTokenRepository}`, `auth/infrastructure/web/AuthController`, `shared/infrastructure/security/SecurityConfig`

## Alcance
**Dentro:** caso de uso `ChangePassword` en `auth.application`, endpoint `PUT /api/v1/auth/password`, corrección en `SecurityConfig`, pruebas de integración.  
**Fuera:** recuperación de contraseña por correo (Fase G, solo con confirmación explícita).

## Tareas

### Contrato
- [x] 1 - Contrato documentado en `docs/api/collaboration-notifications-v1.md` §2 (2026-10-17).

### Backend
- [x] 2 - **Seguridad (SecurityConfig):** añadir regla específica `requestMatchers(HttpMethod.PUT, "/api/v1/auth/password").authenticated()` **antes** del `permitAll` existente para `PUBLIC_PATHS`. Cambio mínimo y aditivo en módulo de Miguel.
- [x] 3 - **Caso de uso `ChangePassword`** en `auth.application`: recibe `(long userId, String currentPassword, String newPassword)`, verifica la contraseña actual con `PasswordHasher.matches`, actualiza el hash con `PasswordHasher.hash` vía `UserRepository.updatePassword`, revoca con `RefreshTokenRepository.revokeAllForUser`. Dominio y aplicación puros. TDD: `ChangePasswordTest` (rojo observado en compilación inicial, verde tras implementar).
- [x] 4 - **Puerto `UserRepository.updatePassword(long id, String newHash)`** (adición mínima a la interfaz existente).
- [x] 5 - **Adaptador:** implementación JPA del nuevo método en `auth.infrastructure.persistence.JpaUserRepository`.
- [x] 6 - **Controlador:** método `PUT /password` en `AuthController`. Inyecta `ChangePassword`. Requiere `@AuthenticationPrincipal Jwt jwt` y extrae usuario con `CurrentUser.from(jwt)`.
- [x] 7 - **Manejador de excepciones:** `AuthExceptionHandler` responde `400` con `errors: { currentPassword: "La contraseña actual no es correcta." }` para `InvalidCurrentPasswordException`. `MethodArgumentNotValidException` ya valida la nueva contraseña con `errors: { newPassword: "..." }`.

### Pruebas obligatorias (backend)
- [ ] 8 - `AuthApiTests.changePasswordWithCorrectCurrentPasswordReturns204AndRevokesRefreshTokens` — escrita en `AuthApiTests.java`, pendiente de ejecutar en CI (sin Docker local).
- [ ] 9 - `AuthApiTests.changePasswordWithWrongCurrentPasswordReturns400` — escrita en `AuthApiTests.java`, pendiente de ejecutar en CI.
- [ ] 10 - `AuthApiTests.changePasswordWithInvalidNewPasswordReturns400` — escrita en `AuthApiTests.java`, pendiente de ejecutar en CI.
- [ ] 11 - `AuthApiTests.changePasswordWithoutTokenReturns401` — escrita en `AuthApiTests.java`, pendiente de ejecutar en CI.

### Pruebas obligatorias (frontend)
- [x] 12 - `ChangePasswordModal.test.tsx`: error de contraseña actual incorrecta aparece en el campo `current`; éxito cierra el modal (2 pruebas en verde con Vitest).

## Evidencia rojo → verde

| Prueba | Rojo (antes) | Verde (después) |
|---|---|---|
| `ChangePasswordTest.wrongCurrentPasswordThrowsInvalidCurrentPasswordException` | Fallo de compilación inicial (método y caso de uso no existían) | Pasa (0.146 s) |
| `ChangePasswordTest.correctPasswordUpdatesPasswordHashAndRevokesAllRefreshTokens` | Fallo de compilación inicial | Pasa (0.146 s) |
| `ChangePasswordModal.test.tsx` (2 tests: error 400 y éxito 204) | Archivo de prueba nuevo | Pasa (163 tests frontend en verde) |
| `AuthApiTests.*` (4 pruebas de integración) | Escritas en `AuthApiTests.java` | Pendientes de ejecutar en CI (sin Docker local) |

## Verificación
- Backend compila: `./mvnw -q -DskipTests compile` exit code 0.
- Pruebas unitarias backend: `./mvnw test -Dtest="*Test"`: 200 pruebas ejecutadas, 0 fallos.
- Frontend: `pnpm typecheck` (0 errores), `pnpm lint` (0 advertencias), `pnpm test` (163 pruebas ejecutadas, 0 fallos).

## Desviaciones
- `PUT /api/v1/auth/password` vive en el módulo `auth` (de Miguel), no en `collaboration`. El caso de uso `ChangePassword` se agrega en `auth.application` como cambio aditivo.
- Contraseña nueva igual a la actual: **aceptada** (decisión documentada en el contrato; simplifica la implementación y evita revelar si la contraseña cambió).

## Puntos a acordar con Miguel (en el PR)
1. Regla adicional en `SecurityConfig` (`.requestMatchers(HttpMethod.PUT, "/api/v1/auth/password").authenticated()`).
2. Método `updatePassword` en `UserRepository` y `JpaUserRepository`.
3. Método `changePassword` en `AuthController`.

## Pendientes
- [ ] Ejecución de pruebas de integración con Testcontainers en CI.
- [ ] Prueba de humo manual de cambio de contraseña con el stack completo.
