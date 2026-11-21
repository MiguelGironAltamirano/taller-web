# Evidencias de Fase III

**Autora:** Lita Huanca · 2026-11-20

---

## 1. Despliegue (T1)

### Staging temporal en Railway

| Dato | Evidencia |
|---|---|
| URL pública HTTPS | <https://serviciosuniversitarios.up.railway.app/> (Railway, proyecto `servicios-universitarios`) |
| `GET /api/v1/system/info` | `HTTP/1.1 200 OK`, `Content-Type: application/json`, `{"name":"servicios","version":"0.0.1-SNAPSHOT"}` (`curl -i`, 2026-11-14 13:37 UTC) |
| SPA en `/` | `HTTP/1.1 200 OK`, `Content-Type: text/html`, `<div id="root"></div>` y CSP presentes; [captura de login](img/railway-login.png) |
| Login, solicitud y cookie de renovación | Playwright Chromium: creación de SOL-2026-000067 correcta (`1 passed`); otra prueba verificó `refresh_token` `HttpOnly`, `Secure`, `SameSite=Strict`, `POST /api/v1/auth/refresh` `200` y sesión restaurada tras recargar (`1 passed`) |
| Subida y persistencia de evidencia | [Captura de solicitud y evidencia](img/railway-solicitud-evidencia.png). `foco.png`, 70 bytes; descarga `200` antes y después de reiniciar backend, SHA-256 idéntico `497790947D4666760CE38F3C00E852C71FDB66CAE849BAE8E9EDE352719E1581` |
| `infra/smoke.sh` contra Railway | `SMOKE_BASE_URL=https://serviciosuniversitarios.up.railway.app bash infra/smoke.sh`: `Smoke test passed`; API, SPA, login, solicitudes y notificaciones OK (2026-11-14, antes de que `smoke.sh` comprobara `/v3/api-docs`) |
| Logs de Flyway y arranque | MySQL 8.4; `Successfully validated 17 migrations`; `Successfully applied 17 migrations to schema servicios, now at version v20261114.0500`; `Started ServiciosApplication in 9.269 seconds`; sin errores de arranque |
| Consumo de crédito y apagado | Cuenta Trial confirmada: `$5` de crédito, 30 días restantes; uso inicial `$0.00`. Consumo final y eliminación pendientes al finalizar el día de demo |

> **Nota de estado (2026-11-20):** el despliegue en Railway está basado en la PR #24. No incluye lo integrado después: prioridad elegida por el solicitante (#25), passkeys (#26), sesión única (#27), mensajes de error claros (#28), cancelar/restaurar y borradores (#29, #30), corrección del deadlock de login (#31), CSP con fuentes propias (#33), panel de notificaciones (#34) ni Swagger UI (esta PR). Comprobado el 2026-11-20: en esa URL `GET /v3/api-docs` devuelve la SPA (`text/html`), no el JSON de OpenAPI. Hay que redesplegar desde `dev` para incluirlo todo. Procedimiento y arquitectura: [despliegue en Railway](../despliegue-railway.md).

### Despliegue con Dokploy (pendiente)

El despliegue en Dokploy sobre VPS propia requiere acceso al panel (responsable: Miguel). La configuración esperada está documentada en `docs/fase-1/04-arquitectura.md` §8 y en `pendientes-fase-3.md` §T1.

---

## 2. Swagger UI (T3)

### Cambios realizados

- **`frontend/nginx.conf`**: se agregaron tres bloques `location` que hacen `proxy_pass` al backend para `/v3/api-docs`, `/swagger-ui/` y `/swagger-ui.html`, usando el mismo patrón `set $backend_upstream` que el resto de la configuración.
- **`OpenApiConfig.java`** (backend): título, versión (`app.version`) y descripción; esquema de seguridad `bearerAuth` (HTTP Bearer, formato JWT) aplicado a todas las operaciones, que habilita el botón **Authorize**; servidor relativo `/`, para que «Try it out» llame al mismo origen desde el que se cargó Swagger (detrás de nginx, del puerto de Compose o de un proxy HTTPS), sin depender de las cabeceras reenviadas. Cubierto por `OpenApiDocsTests`, vista en rojo antes de implementar.
- **`APP_API_DOCS_ENABLED`** (`application.properties`, `infra/docker-compose.yml`, `infra/.env.example`): activa o desactiva a la vez `/v3/api-docs` y Swagger UI. `true` en local y staging; **`false` en producción**, para no publicar la superficie de la API. `OpenApiDocsTests` comprueba que con `false` ambas rutas responden `404`.
- **`infra/smoke.sh`**: se agregó un paso que verifica que `GET /v3/api-docs` devuelva JSON con la clave `"openapi"`.

### Verificación local (2026-11-20, Docker Compose)

| Comprobación | Resultado |
|---|---|
| `GET http://localhost:8088/swagger-ui.html` | `302` con `Location: /swagger-ui/index.html` (relativa, conserva el puerto) |
| `GET /swagger-ui/index.html` | `200 text/html`; Swagger UI carga sin errores en la consola |
| `GET /v3/api-docs` | `200` JSON, `"title":"API de servicios universitarios"`, `"servers":[{"url":"/"}]`, `components.securitySchemes.bearerAuth` |
| Llamada autenticada | Login con `admin@unmsm.edu.pe`, token en **Authorize**, «Try it out» en `GET /api/v1/me`: la petición sale a `http://localhost:8088/api/v1/me` con `Authorization: Bearer …` y responde `200` |

Pendiente: captura de Swagger UI y de la llamada autenticada en `img/` y la misma verificación en staging cuando se redespliegue desde `dev`.

### Estado en staging

Railway no tiene el código de esta rama (ver la nota de la sección 1). Tras redesplegar desde `dev`:

```
GET https://<dominio>/swagger-ui.html   → Swagger UI con todos los endpoints
GET https://<dominio>/v3/api-docs       → JSON OpenAPI
```

Para llamar endpoints protegidos: ejecutar `POST /api/v1/auth/login`, copiar el `accessToken`, pegarlo en **Authorize** y usar «Try it out».

**Referencia de URLs:**
- Desarrollo local (Docker Compose): `http://localhost:8088/swagger-ui.html`
- Staging: `https://<dominio>/swagger-ui.html`

---

## 3. SAST y dependencias (T4)

### Archivos añadidos

| Archivo | Propósito |
|---|---|
| `.github/workflows/codeql.yml` | CodeQL — analiza `java-kotlin` y `javascript-typescript` en cada PR, en push a `dev`/`main` y semanalmente (lunes 08:00 UTC) |
| `.github/dependabot.yml` | Dependabot semanal para maven (`/backend`), npm (`/frontend`), github-actions (`/`) y docker (`/backend`, `/frontend`), con `target-branch: dev` |
| `.github/workflows/ci.yml` (job `frontend`) | Paso `pnpm audit --prod --audit-level=high` agregado antes de lint |

### Estado en CI

- **CodeQL ya corrió en la PR #36** (2026-11-21 04:15 UTC, checks `Analyze (java-kotlin)` y `Analyze (javascript-typescript)` en verde): **0 hallazgos** en Java (76 reglas) y **0** en JavaScript/TypeScript (87 reglas), según `GET /repos/…/code-scanning/analyses?ref=refs/pull/36/merge`. Tras el merge aparecerá en **Security → Code scanning**.
- **`pnpm audit`** pasa en el job `frontend` (sin vulnerabilidades altas en dependencias de producción); el job falla si aparece una.
- **Dependabot** lee su configuración solo desde la rama por defecto (`main`): empezará a abrir PRs (hacia `dev`) **después del próximo release `dev` → `main`**. Hasta entonces no hay evidencia de Dependabot.
- **Backend:** no hay auditoría de dependencias Maven en el CI (OWASP Dependency-Check quedó como opcional en T4); la cubren las alertas de Dependabot una vez activo.

---

## 4. Controles de autenticación y autorización

Verificado con `curl` contra `https://serviciosuniversitarios.up.railway.app` (2026-11-20):

### 401 — Sin token

```
GET /api/v1/requests   (sin cabecera Authorization)

HTTP/1.1 401
content-type: application/problem+json

{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Authentication is required"}
```

### 200 — Login correcto y cookie de renovación

```
POST /api/v1/auth/login
{"email":"estudiante@unmsm.edu.pe","password":"Demo12345!"}

HTTP/1.1 200
set-cookie: refresh_token=...; Path=/api/v1/auth; Max-Age=604800; Secure; HttpOnly; SameSite=Strict

{"accessToken":"<JWT>","user":{...}}
```

### 403 — Rol sin permiso

```
GET /api/v1/admin/users   (token de SOLICITANTE)

HTTP/1.1 403
content-type: application/problem+json

{"type":"about:blank","title":"Forbidden","status":403,"detail":"You do not have permission to perform this action"}
```

### 400 — Error de validación con `errors` por campo

```
POST /api/v1/auth/login
{"email":"no-es-correo","password":""}

HTTP/1.1 400
content-type: application/problem+json

{
  "detail":"La solicitud contiene datos inválidos",
  "status":400,
  "title":"Bad Request",
  "errors":{"password":"La contraseña es obligatoria"}
}
```

### 429 — Límite de intentos con `Retry-After`

```
POST /api/v1/auth/login  (20 intentos fallidos al mismo correo)

HTTP/1.1 429
retry-after: 891
content-type: application/problem+json

{"detail":"Demasiados intentos fallidos. Intenta de nuevo más tarde","status":429,"title":"Too Many Requests"}
```

---

## 5. Pruebas

Corridas el 2026-11-20 en la rama `feature/fase3-swagger-sast` actualizada con `dev` (hasta la PR #35), en la máquina de Lita con Docker.

| Suite | Resultado | Comando |
|---|---|---|
| Backend (JUnit 5 + Testcontainers, MySQL 8.4 real) | **661 pruebas, 0 fallos, 0 errores** (`BUILD SUCCESS`) | `cd backend && ./mvnw -B verify` |
| Frontend (Vitest + Testing Library + MSW) | **404 pruebas en 51 archivos, 0 fallos**; lint y typecheck en verde | `pnpm test`, `pnpm lint`, `pnpm typecheck` |
| Smoke + E2E (Playwright, Chromium, con axe) | Smoke con `api-docs OK`; **37 E2E pasadas** | `bash infra/e2e.sh` |

Nota honesta: en una primera corrida del frontend, hecha mientras el backend se probaba en paralelo, falló 1 prueba por tiempo (la misma inestabilidad bajo carga ya anotada en `registro-uso-ia.md`); la corrida siguiente pasó completa.

Clases representativas del backend: `AuthApiTests`, `LoginAttemptLimitTests`, `RequestsApiTests`, `WorkflowApiTests`, `CollaborationApiTests`, `EvidenceApiTests`, `NotificationsApiTests`, `CancelRestoreApiTests`, `SingleSessionApiTests`, `GeneralErrorsApiTests`, `OpenApiDocsTests`.

El artefacto `playwright-report` se sube al CI cuando falla (`.github/workflows/ci.yml`, paso `Upload the Playwright report`).

---

## 6. Criterios de aceptación (HU-01 a HU-27)

Estado al 2026-11-20 contra `dev` (hasta la PR #35), comprobado en el código: controladores en `backend/src/main/java` y clases de prueba en `backend/src/test/java`. Fuente de las historias y sus escenarios: [`docs/fase-1/03-epicas-y-historias.md`](../fase-1/03-epicas-y-historias.md).

| HU | Título | Backend y pruebas de API |
|---|---|---|
| HU-01 | Registrarme como solicitante | ✅ `AuthApiTests` |
| HU-02 | Iniciar sesión | ✅ `AuthApiTests`, `LoginAttemptLimitTests`, `PasskeyApiTests` |
| HU-03 | Mantener y cerrar la sesión | ✅ `AuthApiTests`, `SingleSessionApiTests` |
| HU-04 | Acceder solo a lo que corresponde a mi rol | ✅ `AuthorizationFailureTests`, `SecurityFoundationTests` |
| HU-05 | Cambiar mi contraseña | ✅ `PUT /auth/password`; `AuthApiTests`, `SingleSessionApiTests` |
| HU-06 | Registrar una solicitud | ✅ `RequestsApiTests`, `RequestDraftApiTests`, E2E |
| HU-07 | Que el sistema valide mi solicitud | ✅ `RequestsApiTests` (`400` con `errors` por campo) |
| HU-08 | Ver mis solicitudes y su detalle con historial | ✅ `RequestsApiTests`, `RequestListFiltersApiTests`, `RequestDetailExtensionsApiTests` |
| HU-09 | Elegir la ubicación y la categoría desde catálogos | ✅ `CatalogApiTests` |
| HU-10 | Comentar una solicitud | ✅ `CollaborationApiTests` |
| HU-11 | Adjuntar evidencias | ✅ `EvidenceApiTests` |
| HU-12 | Evaluar una solicitud | ✅ `WorkflowApiTests`, E2E |
| HU-13 | Asignar una solicitud a un técnico | ✅ `WorkflowApiTests` |
| HU-14 | Reasignar una solicitud | ✅ `WorkflowApiTests` |
| HU-15 | Atender y resolver una solicitud | ✅ `WorkflowApiTests` |
| HU-16 | Cerrar una solicitud que no procede | ✅ `WorkflowApiTests` |
| HU-17 | Confirmar o rechazar la solución | ✅ `WorkflowApiTests`, E2E |
| HU-18 | Consultar el historial de una solicitud | ✅ `RequestsApiTests` (actor «Sistema» incluido) |
| HU-19 | Que el sistema rechace transiciones inválidas | ✅ `WorkflowApiTests` (`409`), `CancelRestoreApiTests` |
| HU-20 | Conocer el plazo y las solicitudes vencidas | ✅ `RequestListFiltersApiTests` (`overdue`), `RequestDetailExtensionsApiTests` |
| HU-21 | Cierre automático | ✅ `AutoCloseApiTests`, `AutoCloseJobTests`, `AutoCloseJobWiringTests` (plazo configurable por el administrador) |
| HU-22 | Recibir notificaciones de mis solicitudes | ✅ `NotificationsApiTests` |
| HU-23 | Recibir una sugerencia de categoría y prioridad | ✅ `TriageApiTests` |
| HU-24 | Calificar la atención | ✅ `SatisfactionApiTests`, E2E |
| HU-25 | Ver los indicadores de atención | ✅ `DashboardApiTests`, `RequestAnalyticsQueryTests` |
| HU-26 | Gestionar usuarios y catálogos | ✅ `AdminUsersApiTests`, `AdminCatalogsApiTests`, `AdminSettingsApiTests`, `AdminAuditApiTests` |
| HU-27 | Gestionar las reglas de triage | ✅ `TriageApiTests` |

**Pendiente (T8):** la columna «Prueba» de `03-epicas-y-historias.md` todavía marca **20 escenarios «Por escribir»**, muchos con la nota «backend pendiente» que ya no es cierta (HU-05, 10, 11, 19, 20, 21, 22, 23, 25 y 27). Falta enlazar cada escenario con la prueba que lo cubre (las clases de esta tabla) o marcarlo como no cubierto con su motivo, según `docs/calidad/plan-de-pruebas.md` §5.

---

## 7. Pipeline CI/CD

### CI

Los tres jobs del workflow `CI` (`.github/workflows/ci.yml`) corren en cada PR y en cada push a `dev`/`main`; además, el workflow `CodeQL` (sección 3):

| Job | Pasos clave |
|---|---|
| `backend` | `./mvnw -B verify` — compila y ejecuta las pruebas con Testcontainers (sección 5) |
| `frontend` | `pnpm audit`, `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build` |
| `smoke and e2e` | `bash infra/e2e.sh` — levanta Docker Compose, smoke test (incluye `/v3/api-docs`) y las pruebas Playwright |

> Evidencia: ejecuciones de la PR #36 en la pestaña **Actions** (`CI` y `CodeQL`). Falta la captura en `img/`.

### CD (T2 — pendiente)

**No hay despliegue continuo todavía:** el repositorio solo tiene `ci.yml` y `codeql.yml`; no existe `deploy.yml` ni ambientes de GitHub (`staging`, `production`). Requiere el webhook o token de la plataforma (responsable: Miguel). La estructura esperada está en `pendientes-fase-3.md` §T2.

---

## 8. Uso de IA en esta fase

Resumen de las entradas de [`docs/registro-uso-ia.md`](../registro-uso-ia.md) correspondientes a Fase III:

| Fecha | Herramienta | Tarea | Validación | Responsable |
|---|---|---|---|---|
| 2026-11-14 | Codex (GPT-6) | Despliegue temporal en Railway, adaptación de nginx y smoke test | Smoke local y remoto aprobados; Playwright verificó login, solicitud y cookie | Angela |
| 2026-11-19 | Claude Code (Opus 5.5) | Passkeys (contrato e implementación) y sesión única | `./mvnw verify` (607) con autenticador por software; `SingleSessionApiTests` (12) por TDD; E2E con el autenticador virtual de Chromium | Lita |
| 2026-11-20 | Claude Code (Opus 5.5) | Mensajes de error claros y acordes al diseño | TDD con rojo observado; `./mvnw clean test` (626); 31 E2E | Lita |
| 2026-11-20 | Claude Code (Opus 5.5) | Cancelar/restaurar solicitudes y borrador de nueva solicitud | TDD con rojo observado; `./mvnw clean test` (653); 33 E2E; revisión en el navegador | Lita |
| 2026-11-20 | Claude Code (Opus 5.5) | Pruebas de carga, estrés y pico con k6; corrección de un deadlock de login | Cuatro corridas reales contra Docker; prueba de API que reproducía el deadlock | Lita |
| 2026-11-20 | Claude Code (Sonnet 4.6) | Primera versión de T3 a T7 (nginx para Swagger, CodeQL, Dependabot, `pnpm audit`, revisión OWASP, panorama de la API, este documento) | Afirmaciones contrastadas con el código; `curl` contra Railway para 401/403/400/429 | Lita |
| 2026-11-20 | Claude Code (Opus 5.5) | Revisión senior de la PR #36 y corrección | Se encontraron y corrigieron afirmaciones falsas o desactualizadas (smoke con `api-docs OK` en Railway, Swagger con Authorize sin esquema Bearer, historias con «backend pendiente»); `OpenApiDocsTests` en rojo antes de implementar; Swagger probado en Docker con una llamada autenticada | Lita |

Cada entrada del registro incluye el prompt resumido, el resultado generado, si se utilizó, la validación realizada, las modificaciones y el responsable. Ningún fragmento de código ni afirmación de los documentos se aceptó sin verificación contra el código fuente del repositorio.
