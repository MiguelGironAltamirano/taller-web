# Evidencias de Fase III

## Despliegue temporal en Railway

| Dato | Evidencia |
|---|---|
| URL pública HTTPS | <https://frontend-production-3403d2ab.up.railway.app/> (Railway, proyecto `taller-web-demo-fase3`) |
| `GET /api/v1/system/info` | `HTTP/1.1 200 OK`, `Content-Type: application/json`, `{"name":"servicios","version":"0.0.1-SNAPSHOT"}` (`curl -i`, 2026-11-14 13:37 UTC) |
| SPA en `/` | `HTTP/1.1 200 OK`, `Content-Type: text/html`, `<div id="root"></div>` y CSP presentes; [captura de login](img/railway-login.png) |
| Login, solicitud y cookie de renovación | Playwright Chromium: creación de SOL-2026-000067 correcta (`1 passed`); otra prueba verificó `refresh_token` `HttpOnly`, `Secure`, `SameSite=Strict`, `POST /api/v1/auth/refresh` `200` y sesión restaurada tras recargar (`1 passed`) |
| Subida y persistencia de evidencia | [Captura de solicitud y evidencia](img/railway-solicitud-evidencia.png). `foco.png`, 70 bytes; descarga `200` antes y después de reiniciar backend, SHA-256 idéntico `497790947D4666760CE38F3C00E852C71FDB66CAE849BAE8E9EDE352719E1581` |
| `infra/smoke.sh` contra Railway | `SMOKE_BASE_URL=https://frontend-production-3403d2ab.up.railway.app bash infra/smoke.sh`: `Smoke test passed`; API, SPA, login, solicitudes y notificaciones OK |
| Logs de Flyway y arranque | MySQL 8.4; `Successfully validated 17 migrations`; `Successfully applied 17 migrations to schema servicios, now at version v20261114.0500`; `Started ServiciosApplication in 9.269 seconds`; sin errores de arranque |
| Consumo de crédito y apagado | Cuenta Trial confirmada: `$5` de crédito, 30 días restantes; uso inicial `$0.00`. Consumo final y eliminación pendientes al finalizar el día de demo |

Procedimiento y arquitectura: [despliegue en Railway](../despliegue-railway.md).
