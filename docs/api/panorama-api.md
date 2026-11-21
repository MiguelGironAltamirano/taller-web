# Panorama de la API — Gestión de Solicitudes UNMSM

**Autor:** Lita Huanca · 2026-11-20

---

## 1. Diseño REST

### Base y convenciones

- **Base URL:** `/api/v1`
- **Formato:** JSON (UTF-8). Fechas ISO-8601 en UTC, salvo `from`/`to` de filtros de solicitudes (días de Lima).
- **Errores:** `application/problem+json` (RFC 7807) con `type`, `title`, `status`, `detail`. Errores de validación agregan `errors: { campo: mensaje }`.

### Recursos principales

| Módulo | Recurso | Rutas clave |
|---|---|---|
| Auth | Sesión | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /me` |
| Auth | Registro y contraseña | `POST /auth/register`, `PUT /auth/password` |
| Auth | Passkeys (WebAuthn) | `POST /auth/passkeys/login/options`, `POST /auth/passkeys/login`, `GET/POST /me/passkeys`, `POST /me/passkeys/registration/options`, `PATCH/DELETE /me/passkeys/{id}` |
| Solicitudes | Solicitudes | `GET /requests`, `POST /requests`, `GET /requests/{code}`, `POST /requests/{code}/transitions`, `PATCH /requests/{code}/priority`, `POST /requests/{code}/rating` |
| Solicitudes | Contadores | `GET /requests/counts` |
| Solicitudes | Borrador | `GET /me/request-draft`, `PUT /me/request-draft`, `DELETE /me/request-draft` |
| Colaboración | Evidencias y comentarios | `GET/POST /requests/{code}/evidences`, `GET /requests/{code}/evidences/{id}` (descarga), `GET/POST /requests/{code}/comments` |
| Notificaciones | Bandeja del usuario | `GET /notifications`, `PATCH /notifications/{id}/read`, `POST /notifications/read-all` |
| Triage | Sugerencias y reglas | `POST /triage/suggestions`, `GET/POST/PUT/DELETE /admin/triage-rules` |
| Dashboard | Indicadores y carga | `GET /dashboard/indicators`, `GET /dashboard/team` |
| Administración | Usuarios, catálogos, auditoría, config | `GET/POST /admin/users`, `PUT /admin/users/{id}`, `PATCH /admin/users/{id}/active`, `GET /admin/states`, `POST/DELETE /admin/areas`, `POST/DELETE /admin/categories`, `POST/DELETE /admin/locations`, `PUT /admin/priorities/{id}`, `GET /admin/audit`, `GET/PUT /admin/config` |
| Catálogos | Datos de referencia | `GET /catalogs`, `GET /users/technicians` |
| Sistema | Info de salud | `GET /system/info` |

### Paginación y filtros

`GET /requests` acepta todos los parámetros en paralelo (combinación AND); la regla de visibilidad del rol siempre se aplica primero:

| Parámetro | Tipo | Semántica |
|---|---|---|
| `status` | enum repetible | Estado(s) a incluir |
| `areaId`, `categoryId`, `priorityId`, `technicianId` | entero | Igualdad exacta |
| `unassigned` | bool | Sin técnico asignado |
| `overdue` | bool | Vencidas según el SLA |
| `q` | texto ≤100 | Búsqueda en código, descripción, solicitante, categoría y ambiente |
| `from`, `to` | fecha `AAAA-MM-DD` | Rango de registro en hora de Lima |
| `sort` | `newest` \| `oldest` \| `due` \| `priority` | Criterio de orden |
| `page` / `size` | int (0 / 1–500) | Paginación; respuesta: `{ items, page, size, total, totalPages }` |

Un valor inválido en cualquier parámetro responde `400` con `errors` para todos los campos problemáticos a la vez.

### Códigos HTTP utilizados

| Código | Cuándo |
|---|---|
| `200` | Consulta o actualización exitosa |
| `201` | Recurso creado |
| `204` | Éxito sin cuerpo (logout, borrado) |
| `400` | Validación fallida, parámetro inválido o cuerpo malformado |
| `401` | Sin sesión, sesión vencida o credenciales inválidas |
| `403` | Rol sin permiso para la operación |
| `404` | Recurso inexistente o no visible para el usuario |
| `405` | Método HTTP no admitido |
| `409` | Conflicto de negocio: correo ya registrado, transición inválida, etc. |
| `413` | Cuerpo excede 6 MB (límite de nginx); archivos entre 5 MB y 6 MB los rechaza el backend con `400` |
| `415` | Content-Type no admitido |
| `429` | Demasiados intentos de login; cabecera `Retry-After` con segundos |
| `500` | Error inesperado (causa solo en logs del servidor) |
| `503` | Backend no disponible (nginx) |

### Formato de error (RFC 7807)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "La solicitud contiene datos inválidos",
  "errors": {
    "email": "El correo debe terminar en @unmsm.edu.pe",
    "password": "La contraseña debe tener al menos 8 caracteres"
  }
}
```

`detail` siempre en español, escrito para la persona. `title` es técnico y el frontend no lo muestra. Los errores de lógica de negocio nunca revelan información interna (p. ej. credenciales inválidas, mismo mensaje si el correo no existe o si la contraseña es incorrecta).

**Documentación interactiva:** Swagger UI, generado por springdoc a partir de los controladores y configurado en `OpenApiConfig` (título, versión, esquema `bearerAuth` y servidor relativo `/`). Para llamar rutas protegidas: `POST /api/v1/auth/login`, copiar el `accessToken`, pegarlo en **Authorize** y usar «Try it out».
- Desarrollo local: `http://localhost:8088/swagger-ui.html` (verificado el 2026-11-20 con una llamada autenticada a `GET /api/v1/me`)
- Staging: `https://<dominio>/swagger-ui.html` (tras redesplegar desde `dev`)
- Producción: desactivado con `APP_API_DOCS_ENABLED=false`; `/v3/api-docs` y `/swagger-ui.html` responden `404`. El contrato sigue publicado en `docs/api/`.

---

## 2. ¿Por qué REST y no GraphQL, gRPC o Webhooks?

### GraphQL

**Ventajas:** un único endpoint; el cliente pide exactamente los campos que necesita, eliminando over-fetching y under-fetching; ideal cuando hay muchas relaciones y vistas distintas.

**Por qué no:** la API tiene recursos bien definidos y vistas estables (lista, detalle, dashboard). Añadir GraphQL en un monolito con cuatro roles y reglas de visibilidad complejas eleva la superficie de ataque —la autorización debe aplicarse campo a campo, no solo por ruta—, dificulta el caché HTTP estándar y requiere herramientas adicionales para pruebas y documentación. La ganancia no justifica ese costo en este contexto.

### gRPC

**Ventajas:** contratos binarios estrictos (Protocol Buffers), alto rendimiento, streaming bidireccional; ideal para comunicación entre microservicios en backend.

**Por qué no:** el sistema es un monolito; no hay comunicación service-to-service que aproveche las ventajas de gRPC. Los navegadores no pueden hablar gRPC nativo (requieren gRPC-Web y un proxy). Si en el futuro el monolito se separa en servicios, gRPC sería una opción natural para la capa interna.

### Webhooks

**Ventajas:** notifica a sistemas externos de forma asíncrona cuando ocurre un evento (p. ej. cambio de estado de una solicitud), sin que el receptor tenga que hacer polling.

**Por qué no (hoy):** no hay sistemas externos que consuman eventos del sistema. Las notificaciones internas se sirven por `GET /api/v1/notifications` (polling desde el frontend). En una extensión futura —integración con un sistema de mesa de ayuda externo o con el correo institucional— los webhooks serían la pieza correcta: firmar el payload con HMAC-SHA256, implementar reintentos con back-off y exponer un endpoint de confirmación de entrega.

### REST: la elección correcta para este contexto

- Recursos, verbos y códigos HTTP se alinean naturalmente con las operaciones del dominio (crear, listar, transicionar solicitudes).
- Caché HTTP estándar (`ETag`, `Cache-Control`) funciona de fábrica para `GET`.
- Tooling maduro: Spring MVC, springdoc/OpenAPI, Bean Validation, Testcontainers.
- Curva de aprendizaje baja para un equipo universitario; fácil de probar con `curl` o Swagger.
- El contrato es estable y bien documentado en [`contrato-v1.md`](contrato-v1.md).

---

## 3. API Gateway

### Estado actual: nginx como punto de entrada único

nginx actúa hoy como gateway liviano para todo el tráfico:

```
cliente
  └── nginx :80 (o :443 vía proxy de Dokploy/Railway)
        ├── /api/           → proxy_pass al backend (Spring Boot)
        ├── /v3/api-docs    → proxy_pass al backend (OpenAPI)
        ├── /swagger-ui/    → proxy_pass al backend (Swagger UI)
        └── /               → SPA estática (React)
```

Además de enrutar, nginx aplica las cabeceras de seguridad HTTP (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, CSP) y limita el tamaño de cuerpo a 6 MB para evidencias.

**Rate limiting de autenticación en nginx (opcional):** el `LoginAttemptLimiter` del backend ya protege contra fuerza bruta por email+IP. Como defensa en profundidad se puede añadir en nginx:

```nginx
# limit_req_zone va en el bloque http {} del nginx.conf padre, no dentro de server {}
limit_req_zone $binary_remote_addr zone=login:10m rate=10r/m;

location /api/v1/auth/login {
  limit_req zone=login burst=5 nodelay;
  set $backend_upstream ${BACKEND_UPSTREAM};
  proxy_pass $backend_upstream;
  proxy_set_header Host $host;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
}
```

Esto descarta a nivel de proxy peticiones que superen 10 por minuto por IP antes de que lleguen al backend, reduciendo carga en situaciones de ataque volumétrico.

### Qué aportaría un gateway dedicado

Un API Gateway dedicado (Kong, Traefik con plugins, AWS API Gateway) añadiría sin tocar el código de la aplicación:

| Capacidad | Valor concreto en este sistema |
|---|---|
| **Rate limiting centralizado** | Configurado por ruta y por cliente sin recompilar; límites distintos para `/auth/login` vs. el resto de la API |
| **Autenticación en el gateway** | Validación del JWT antes de que la petición llegue al backend; el backend podría confiar en el token sin reverificarlo |
| **Versionado de API** | Enrutar `/api/v1/` y `/api/v2/` a instancias distintas durante una migración gradual |
| **Observabilidad centralizada** | Métricas de latencia, tasa de errores y volumen por ruta en un único dashboard, sin instrumentar cada microservicio |
| **Transformación de peticiones** | Añadir/quitar cabeceras, normalizar rutas, enmascarar campos sensibles en las respuestas |
| **Circuit breaker** | Dejar de enviar tráfico al backend si supera un umbral de errores, dando tiempo a recuperarse |

**Cuándo tiene sentido para este proyecto:** con el stack actual (monolito + nginx), el gateway sería sobre-ingeniería. Cobra valor si el sistema evoluciona a múltiples servicios, si se necesita exponer la API a terceros con planes de consumo distintos, o si el equipo de operaciones requiere observabilidad centralizada sin instrumentar el código.

---

*Última actualización: 2026-11-20. Ver también [`contrato-v1.md`](contrato-v1.md) para el contrato completo y [`../seguridad/revision-owasp.md`](../seguridad/revision-owasp.md) para los controles de seguridad.*
