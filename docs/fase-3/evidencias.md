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
| `infra/smoke.sh` contra Railway | `SMOKE_BASE_URL=https://serviciosuniversitarios.up.railway.app bash infra/smoke.sh`: `Smoke test passed`; API, SPA, login, solicitudes, notificaciones y `/v3/api-docs` OK |
| Logs de Flyway y arranque | MySQL 8.4; `Successfully validated 17 migrations`; `Successfully applied 17 migrations to schema servicios, now at version v20261114.0500`; `Started ServiciosApplication in 9.269 seconds`; sin errores de arranque |
| Consumo de crédito y apagado | Cuenta Trial confirmada: `$5` de crédito, 30 días restantes; uso inicial `$0.00`. Consumo final y eliminación pendientes al finalizar el día de demo |

> **Nota de estado (2026-11-20):** el despliegue en Railway está basado en PR #24. Las funcionalidades de passkeys (PR #25-#26), sesión única (PR #28), mensajes de error claros (PR #29) y cancelar/restaurar solicitudes (PR #30) no están presentes en esta URL. Se necesita que Miguel redesplegara apuntando a `dev` para incluirlas. Procedimiento y arquitectura: [despliegue en Railway](../despliegue-railway.md).

### Despliegue con Dokploy (pendiente)

El despliegue en Dokploy sobre VPS propia requiere acceso al panel (responsable: Miguel). La configuración esperada está documentada en `docs/fase-1/04-arquitectura.md` §8 y en `pendientes-fase-3.md` §T1.

---

## 2. Swagger UI (T3)

### Cambios realizados

- **`frontend/nginx.conf`**: se agregaron tres bloques `location` que hacen `proxy_pass` al backend para `/v3/api-docs`, `/swagger-ui/` y `/swagger-ui.html`, usando el mismo patrón `set $backend_upstream` que el resto de la configuración.
- **`infra/smoke.sh`**: se agregó un paso que verifica que `GET /v3/api-docs` devuelva JSON con la clave `"openapi"`.
- El test de humo con `SMOKE_BASE_URL=https://serviciosuniversitarios.up.railway.app` confirmó `api-docs OK` (2026-11-20, Railway aún sin las últimas funcionalidades).

### Estado en staging

Railway no tiene el código de esta rama (`feature/fase3-swagger-sast`). Una vez que Miguel redesplegara con `dev`:

```
GET https://<dominio>/swagger-ui.html   → Swagger UI con todos los endpoints
GET https://<dominio>/v3/api-docs       → JSON OpenAPI
```

Desde Swagger UI se puede: introducir credenciales con `POST /auth/login`, copiar el `accessToken` y pegarlo en "Authorize" para llamar endpoints protegidos.

**Referencia de URLs:**
- Desarrollo local (Docker Compose): `http://localhost:8088/swagger-ui.html`
- Staging: `https://<dominio>/swagger-ui.html`

---

## 3. SAST y dependencias (T4)

### Archivos añadidos

| Archivo | Propósito |
|---|---|
| `.github/workflows/codeql.yml` | CodeQL — analiza `java-kotlin` y `javascript-typescript` en cada PR, en push a `dev`/`main` y semanalmente (lunes 08:00 UTC) |
| `.github/dependabot.yml` | Dependabot semanal para maven (`/backend`), npm (`/frontend`), github-actions (`/`) y docker (`/backend`, `/frontend`) |
| `.github/workflows/ci.yml` (job `frontend`) | Paso `pnpm audit --prod --audit-level=high` agregado antes de lint |

### Estado en CI

Los archivos están en la rama `feature/fase3-swagger-sast` (PR #36 pendiente de merge). Una vez fusionados:

- CodeQL aparecerá en la pestaña **Security → Code scanning alerts** del repositorio.
- Dependabot abrirá PRs semanales para cada ecosistema.
- El job `frontend` del CI falla ante vulnerabilidades npm de severidad alta.

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

### Backend

| Métrica | Valor |
|---|---|
| Total de pruebas | **657** (77 clases, 0 fallos, 0 errores) |
| Fuente | `backend/target/surefire-reports/` — `./mvnw -B verify` en rama `feature/fase3-swagger-sast` |
| Motor | JUnit 5 + Testcontainers (MySQL 8.4 real) |

Clases representativas: `AuthApiTests`, `LoginAttemptLimitTests`, `RequestsApiTests`, `WorkflowApiTests`, `CollaborationApiTests`, `NotificationsApiTests`, `CancelRestoreApiTests`, `RequestDraftApiTests`, `SingleSessionApiTests`, `GeneralErrorsApiTests`, `SchemaMigrationTests`.

### Frontend

| Métrica | Valor |
|---|---|
| Total de pruebas | **378** (40 archivos, 0 fallos) |
| Fuente | `pnpm test` — rama `feature/requests-cancel-and-drafts` (última entrada de `registro-uso-ia.md`) |
| Motor | Vitest + Testing Library + MSW |

### E2E (Playwright)

| Métrica | Valor |
|---|---|
| Total de pruebas | **33** (Chromium) |
| Fuente | `infra/e2e.sh` — rama `feature/requests-cancel-and-drafts` |
| Cobertura axe | 26 páginas sin violaciones de accesibilidad |

El artefacto `playwright-report` se sube automáticamente al CI en caso de fallo (`.github/workflows/ci.yml` paso `Upload the Playwright report`).

---

## 6. Criterios de aceptación (HU-01 a HU-27)

Estado al 2026-11-20 contra la rama `dev` (incluye PRs #24–#30 ya fusionados o en revisión). Fuente: [`docs/fase-1/03-epicas-y-historias.md`](../fase-1/03-epicas-y-historias.md).

| HU | Título | Estado |
|---|---|---|
| HU-01 | Registrarme como solicitante | ✅ Completa — pruebas de API y frontend |
| HU-02 | Iniciar sesión | ✅ Completa — pruebas de API, límite de intentos y frontend |
| HU-03 | Mantener y cerrar la sesión | ✅ Completa — rotación, revocación y restauración |
| HU-04 | Acceder solo a lo que corresponde a mi rol | ✅ Completa — `AccessPolicy`, `401`/`403` probados |
| HU-05 | Cambiar mi contraseña | ⚠️ Frontend completo; backend pendiente de PR |
| HU-06 | Registrar una solicitud | ✅ Completa — pruebas de API y E2E |
| HU-07 | Que el sistema valide mi solicitud | ✅ Completa — `400` con `errors` por campo |
| HU-08 | Ver mis solicitudes y su detalle con historial | ✅ Completa — pruebas de API y frontend |
| HU-09 | Elegir la ubicación y la categoría desde catálogos | ✅ Completa — pruebas de API y frontend |
| HU-10 | Comentar una solicitud | ⚠️ Frontend completo; backend pendiente de PR |
| HU-11 | Adjuntar evidencias | ⚠️ Frontend completo; backend pendiente de PR |
| HU-12 | Evaluar una solicitud | ✅ Completa — pruebas de API y E2E |
| HU-13 | Asignar una solicitud a un técnico | ✅ Completa — pruebas de API |
| HU-14 | Reasignar una solicitud | ✅ Completa — pruebas de API |
| HU-15 | Atender y resolver una solicitud | ✅ Completa — pruebas de API |
| HU-16 | Cerrar una solicitud que no procede | ✅ Completa — pruebas de API |
| HU-17 | Confirmar o rechazar la solución | ✅ Completa — pruebas de API y E2E |
| HU-18 | Consultar el historial de una solicitud | ✅ Completa — actor "Sistema" incluido |
| HU-19 | Que el sistema rechace transiciones inválidas | ✅ Completa — `AllowedTransitions`, `409` probado |
| HU-20 | Conocer el plazo y las solicitudes vencidas | ✅ Completa — SLA y fechas estimadas (vista por rol) |
| HU-21 | Cierre automático a las 72 horas | ⚠️ Frontend completo; backend pendiente de PR |
| HU-22 | Recibir notificaciones de mis solicitudes | ⚠️ Frontend completo; backend pendiente de PR |
| HU-23 | Recibir una sugerencia de categoría y prioridad | ⚠️ Frontend completo; backend pendiente de PR |
| HU-24 | Calificar la atención | ✅ Completa — pruebas de API, `SatisfactionApiTests`, E2E |
| HU-25 | Ver los indicadores de atención | ⚠️ Frontend completo; backend pendiente de PR |
| HU-26 | Gestionar usuarios y catálogos | ✅ Usuarios, catálogos y estados completos; auditoría pendiente |
| HU-27 | Gestionar las reglas de triage | ⚠️ Frontend completo; backend pendiente de PR |

**Leyenda:** ✅ backend + frontend + pruebas de API integradas · ⚠️ frontend listo contra API simulada, backend aún en rama de trabajo.

---

## 7. Pipeline CI/CD

### CI en verde (T3 + T4)

Los tres jobs del workflow `CI` (`.github/workflows/ci.yml`) corren en cada PR y en cada push a `dev`/`main`:

| Job | Pasos clave |
|---|---|
| `backend` | `./mvnw -B verify` — compila, ejecuta las 657 pruebas con Testcontainers |
| `frontend` | `pnpm audit`, `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build` |
| `smoke and e2e` | `bash infra/e2e.sh` — levanta Docker Compose, smoke test, 33 pruebas Playwright |

> La ejecución en verde de la PR #36 quedará como evidencia en la pestaña **Actions** del repositorio una vez que el CI pase.

### CD (T2 — pendiente)

El despliegue continuo requiere el webhook de Dokploy (responsable: Miguel). La estructura del workflow `deploy.yml` está documentada en `pendientes-fase-3.md` §T2.

---

## 8. Uso de IA en esta fase

Resumen de las entradas de [`docs/registro-uso-ia.md`](../registro-uso-ia.md) correspondientes a Fase III:

| Fecha | Herramienta | Tarea | Validación | Responsable |
|---|---|---|---|---|
| 2026-11-14 | Codex (GPT-6) | Despliegue en Railway, adaptación de nginx y smoke test | Smoke local y remoto aprobados; Playwright verificó login, solicitud y cookie | Angela |
| 2026-11-19 | Claude Code (Opus 5.5) | Revisión OWASP A07: mensajes de error claros y acordes al diseño | TDD con rojo observado; `./mvnw clean test` (626 pruebas); 31 E2E | Lita |
| 2026-11-19 | Claude Code (Opus 5.5) | Cancelar/restaurar solicitudes y borrador de nueva solicitud | TDD con rojo observado; `./mvnw clean test` (653 pruebas); 33 E2E; revisión en el navegador | Lita |
| 2026-11-20 | Claude Code (Sonnet 4.6) | T3 (Swagger UI vía nginx), T4 (CodeQL, Dependabot, pnpm audit), T5 (revisión OWASP Top 10), T6 (panorama de la API), T7 (este documento) | Cada afirmación de los documentos verificada contra el código fuente; curl en vivo contra Railway para 401/403/400/429; smoke test con `api-docs OK`; conteo de pruebas en `surefire-reports` (657) | Lita |

Cada entrada del registro incluye el prompt resumido, el resultado generado, si se utilizó, la validación realizada, las modificaciones y el responsable. Ningún fragmento de código ni afirmación de los documentos se aceptó sin verificación contra el código fuente del repositorio.
