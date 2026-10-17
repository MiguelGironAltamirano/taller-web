# Plan de Miguel — Plataforma, seguridad y autenticación

Plan general: [`README.md`](README.md).

## 1. Objetivo del módulo

Dejar la base sobre la que trabaja el resto del equipo: esquema de base de datos, seguridad JWT, autenticación, estructura del frontend y despliegue continuo; y, en la etapa de calidad, asegurar y observar el sistema (SAST, DAST y observabilidad).

**Estado actual:** el trabajo de plataforma y autenticación está en curso en las ramas `feature/auth-platform-api` (esquema y usuarios de demostración, seguridad JWT, registro, inicio de sesión, renovación y cierre de sesión) y `feature/auth-platform-web` (estructura del frontend con login y registro). Los demás integrantes parten de esa base una vez integrada en `dev`.

## 2. Alcance

**Backend**
- Esquema base (Flyway) y datos de demostración en la ubicación `db/seed-dev`, solo para desarrollo y staging.
- Seguridad JWT: token de acceso de 15 minutos; token de renovación de 7 días con rotación, en cookie `HttpOnly`, `Secure`, `SameSite=Strict`; contraseñas con BCrypt.
- Autorización por rol y verificación de pertenencia como utilidades reutilizables (`AuthenticatedUser`).
- Cabeceras de seguridad, CORS restringido y límite de intentos de login.
- Formato de errores RFC 7807 con manejador global y reglas ArchUnit (paquete `shared`).

**Frontend**
- Enrutado, layout, navegación por rol, rutas protegidas, páginas de login y registro, sesión con el token de acceso en memoria y renovación automática (`app/`, `features/auth`).

**Infraestructura y documentación**
- CI/CD en GitHub Actions y despliegue en Dokploy (staging desde `dev`, producción desde `main`).
- SAST en CI, DAST sobre staging y revisión OWASP Top 10.
- Observabilidad: Uptime Kuma, logs estructurados con identificador de traza, métricas (Actuator/Micrometer) y alertas.
- Manual técnico de plataforma, seguridad, despliegue y observabilidad.

## 3. Endpoints y pantallas

| Endpoint | Acceso | Notas |
|---|---|---|
| `POST /api/v1/auth/register` | Público | Solo crea `SOLICITANTE`; correo `@unmsm.edu.pe`; `400` por validación, `409` si el correo existe |
| `POST /api/v1/auth/login` | Público | `200` con token y cookie de renovación; `401` con el mismo mensaje para correo inexistente o contraseña incorrecta |
| `POST /api/v1/auth/refresh` | Cookie de renovación | Rota el token; reutilizar uno anterior responde `401` |
| `POST /api/v1/auth/logout` | Cookie de renovación | `204`, revoca el token y elimina la cookie |
| `GET /api/v1/me` | Token | Datos del usuario autenticado y su rol |
| `GET /api/v1/system/info` | Público | Comprobación de salud del despliegue |
| Endpoints de observabilidad (`/actuator/*`, métricas Prometheus) | Restringido | Por definir en `docs/api/` antes de implementar |

Pantallas: inicio de sesión, registro de solicitante, layout con navegación por rol, redirección según rol tras el login. El cambio de contraseña (`PUT /api/v1/auth/password`) lo implementa AngelCarrasco sobre esta base.

## 4. Entregables por semana

| Semanas | Entregable |
|---|---|
| 8–10 | Esquema y usuarios de demostración; seguridad JWT; registro, login, refresh y logout; `GET /me`; shell del frontend con login y registro; integración de las ramas de autenticación en `dev` |
| 10 | **Presentación parcial** (todos) |
| 10–12 | Entorno de producción en Dokploy; SAST y análisis de dependencias en CI; revisión OWASP de la autenticación |
| 13–14 | DAST (OWASP ZAP) sobre staging; Uptime Kuma, logs estructurados, métricas y alertas; soporte de integración y despliegue |
| 15 | Manual técnico de plataforma, seguridad, despliegue y observabilidad |
| 16 | **Sustentación final** (todos) |

## 5. Pruebas obligatorias

- Registro válido → `201`, rol `SOLICITANTE` y contraseña guardada como hash BCrypt.
- Registro con correo fuera de `unmsm.edu.pe` → `400` con el campo `email` señalado; correo repetido → `409`.
- Login con un usuario de demostración → `200`, el token abre `/me` y la cookie tiene `HttpOnly`, `Secure`, `SameSite=Strict`.
- Login con contraseña incorrecta y con correo inexistente → `401` con el mismo mensaje.
- Refresh rota el token: el anterior queda revocado y reutilizarlo → `401`.
- Logout revoca el token y elimina la cookie.
- Token ausente, vencido o alterado → `401` con `application/problem+json`.
- Un rol sin permiso sobre un endpoint → `403`.
- Los usuarios de demostración no se crean cuando `APP_FLYWAY_LOCATIONS` no incluye `db/seed-dev`.
- Frontend: login correcto redirige según rol; login incorrecto muestra "Credenciales inválidas" sin limpiar el correo; una respuesta `401` dispara una sola renovación y, si falla, vuelve al login; una ruta de supervisor no es accesible para un solicitante.
- CI: el pipeline falla ante una prueba rota o un hallazgo SAST de severidad alta.

## 6. Dependencias con otros integrantes

| Qué | Quién |
|---|---|
| Recibe: componentes de `shared/ui` (campos, botones, estados, toasts) para las páginas de login y registro | Lita |
| Recibe: tablas de catálogos y reglas de negocio para las migraciones posteriores | Sevan, Angela, AngelCarrasco, AngelCruz |
| Provee: token, roles, `/api/v1/me`, `AuthenticatedUser`, manejador de errores y layout | Todos |
| Provee: pipeline de CI, entornos de staging y producción, observabilidad | Todos |
| Provee: informes SAST y DAST para la revisión de seguridad | Lita (informe de pruebas) |

## 7. Archivos y paquetes que le pertenecen

- Backend: `shared`, `auth`, migraciones base y `db/seed-dev`.
- Frontend: `frontend/src/app/`, `frontend/src/features/auth/`, `frontend/src/shared/api/`.
- Infraestructura: `.github/workflows/`, `infra/`.
- Documentación: contrato de autenticación en `docs/api/`, manual técnico de su parte.

## 8. Definición de terminado

Ver la [definición de terminado general](README.md#5-definición-de-terminado). Además: ningún secreto real en el repositorio, `APP_JWT_SECRET` configurado por entorno y despliegue verificado en staging.
