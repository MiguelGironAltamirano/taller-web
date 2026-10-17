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
- [ ] 2 - **Seguridad (SecurityConfig):** añadir regla específica `requestMatchers(HttpMethod.PUT, "/api/v1/auth/password").authenticated()` **antes** del `permitAll` existente para `PUBLIC_PATHS`. Cambio mínimo y aditivo en módulo de Miguel. Cubrir con prueba `401` sin token.
- [ ] 3 - **Caso de uso `ChangePassword`** en `auth.application`: recibe `(long userId, String currentPassword, String newPassword)`, verifica la contraseña actual con `PasswordHasher.matches`, actualiza el hash con `PasswordHasher.hash` vía `UserRepository.updatePassword` (nuevo método del puerto), revoca con `RefreshTokenRepository.revokeAllForUser`. Dominio sin Spring. TDD: prueba rojo primero.
- [ ] 4 - **Puerto `UserRepository.updatePassword(long id, String newHash)`** (adición mínima a la interfaz existente).
- [ ] 5 - **Adaptador:** implementación JDBC del nuevo método en `auth.infrastructure.persistence`.
- [ ] 6 - **Controlador:** método `PUT /auth/password` en `AuthController` (o un controlador separado en el paquete `auth.infrastructure.web`). Inyecta `ChangePassword`. Requiere `@AuthenticationPrincipal Jwt jwt` para extraer el `userId`.
- [ ] 7 - **Manejador de excepciones:** si la contraseña actual es incorrecta, `AuthExceptionHandler` responde `400` con `errors: { currentPassword: "La contraseña actual no es correcta." }`. Si la nueva contraseña falla validación de Bean Validation, `errors: { newPassword: "..." }`.

### Pruebas obligatorias (backend)
- [ ] 8 - `AuthApiTests.changePasswordWithCorrectCurrentPasswordReturns204AndRevokesRefreshTokens` — flujo completo: cambiar contraseña → nuevo login OK → login anterior `401` → refresh anterior `401`.
- [ ] 9 - `AuthApiTests.changePasswordWithWrongCurrentPasswordReturns400` — `errors.currentPassword` presente.
- [ ] 10 - `AuthApiTests.changePasswordWithInvalidNewPasswordReturns400` — `errors.newPassword` presente.
- [ ] 11 - `AuthApiTests.changePasswordWithoutTokenReturns401` — `401` sin cabecera `Authorization`.

### Pruebas obligatorias (frontend)
- [ ] 12 - `ChangePasswordModal.test.tsx` (o equivalente): error de contraseña actual incorrecta aparece en el campo `current`; éxito cierra el modal.

## Evidencia rojo → verde
*(se completa al implementar)*

| Prueba | Rojo (antes) | Verde (después) |
|---|---|---|
| `changePasswordWithCorrectCurrentPasswordReturns204AndRevokesRefreshTokens` | — | — |
| `changePasswordWithWrongCurrentPasswordReturns400` | — | — |
| `changePasswordWithInvalidNewPasswordReturns400` | — | — |
| `changePasswordWithoutTokenReturns401` | — | — |

## Verificación
*(se completa al terminar la fase B)*

## Desviaciones
- `PUT /api/v1/auth/password` vive en el módulo `auth` (de Miguel), no en `collaboration`. El caso de uso `ChangePassword` se agrega en `auth.application` como cambio aditivo.
- Contraseña nueva igual a la actual: **aceptada** (decisión documentada en el contrato; simplifica la implementación y evita revelar si la contraseña cambió).

## Puntos a acordar con Miguel (en el PR)
1. Regla adicional en `SecurityConfig` (línea específica para `PUT /auth/password`).
2. Método `updatePassword` en `UserRepository`.
3. Nombre y ubicación del controlador/método del endpoint.

## Pendientes
- [ ] Implementar (Fase B).
- [ ] Prueba de humo manual: cambiar contraseña → volver a entrar.
