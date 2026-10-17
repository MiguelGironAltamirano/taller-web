# Contrato — Colaboración y notificaciones (módulo AngelCarrasco)

> Complementa [`contrato-v1.md`](contrato-v1.md) y resuelve los pendientes de [`ui-extensions.md`](ui-extensions.md).
> Fecha de acuerdo inicial: 2026-10-17.
> Estado: **acordado — implementación en progreso**.

## 1. Reglas generales

- Todos los endpoints exigen token de acceso (`Authorization: Bearer <token>`). Sin token ⇒ `401`.
- La convención "no visible ⇒ `404`" se aplica a comentarios y evidencias: si la solicitud no existe **o** el usuario no tiene visibilidad sobre ella, la respuesta es siempre `404`.
- Visibilidad reutiliza `AccessPolicy.canViewRequest` del módulo `shared`.
- Errores: `application/problem+json` con `type`, `title`, `status`, `detail` y, en validación, `errors: { campo: mensaje }`.

---

## 2. Cambio de contraseña

### `PUT /api/v1/auth/password`

**Acceso:** token de acceso (cualquier rol). Sin token ⇒ `401`.

> ⚠️ **Trampa de seguridad:** `SecurityConfig` declara `PUBLIC_PATHS = { "/api/v1/auth/**", … }` con `permitAll`. Se agrega una regla específica más restrictiva **antes** de esa regla para que `PUT /api/v1/auth/password` exija autenticación. Ver sección de implementación en `odd/tasks/auth-change-password.md`.

**Cuerpo:**

```json
{ "currentPassword": "Demo12345!", "newPassword": "NuevaClave9!" }
```

**Reglas de validación:**
- `newPassword`: 8–72 caracteres, al menos una letra y un dígito (mismas reglas que el registro). Violación ⇒ `400` con `errors: { newPassword: "..." }`.
- `currentPassword` incorrecto ⇒ `400` con `errors: { currentPassword: "La contraseña actual no es correcta." }`.
- Contraseña nueva igual a la actual ⇒ **aceptada** (no se rechaza; permite al usuario reconfirmar).
- Datos ausentes o en blanco ⇒ `400` con `errors` por campo.

**Efectos:**
1. Actualiza el hash BCrypt de la contraseña del usuario autenticado.
2. Revoca **todos** los tokens de renovación vigentes del usuario con `RefreshTokenRepository.revokeAllForUser` (incluido el actual; el usuario deberá iniciar sesión de nuevo).
3. El token de acceso actual caduca a los 15 minutos de su emisión (máximo).

**Respuestas:**

| Código | Significado |
|---|---|
| `204` | Contraseña cambiada y sesiones de renovación revocadas |
| `400` | Validación fallida; `errors` indica el campo |
| `401` | Sin token o token inválido |

**Frontend (`ChangePasswordModal.tsx`):** ya existe y no necesita reescritura. Mapea `fieldErrors.currentPassword` al campo `current` y `fieldErrors.newPassword` al campo `next`. Si el servidor responde `204`, cierra el modal. No cierra sesión activamente: el token de acceso actual sigue válido hasta que caduque.

---

## 3. Comentarios

### `GET /api/v1/requests/{code}/comments`

**Acceso:** token con visibilidad sobre la solicitud.

**Respuesta `200`:**

```json
[
  {
    "id": 1,
    "authorName": "Ana García",
    "text": "¿Ya revisaron el equipo?",
    "createdAt": "2026-10-17T12:00:00Z"
  }
]
```

Lista vacía si no hay comentarios. Ordenada por `createdAt` ascendente.

**Errores:** `404` si la solicitud no existe o no es visible.

---

### `POST /api/v1/requests/{code}/comments`

**Acceso:** token con visibilidad sobre la solicitud.

**Cuerpo:**

```json
{ "text": "El problema persiste en el tercer piso." }
```

**Reglas de validación:**
- `text`: 1–1000 caracteres (sin contar espacios sobrantes al inicio y al final). Vacío o solo espacios ⇒ `400` con `errors: { text: "El comentario no puede estar vacío." }`. Más de 1000 ⇒ `400` con `errors: { text: "El comentario no puede superar los 1000 caracteres." }`.

**Respuesta `201`:** el `RequestDetail` actualizado (igual que `GET /requests/{code}`), con `comments` ya incluyendo el nuevo comentario.

> **Decisión de diseño (discrepancia resuelta):** el frontend actual consume `POST /comments` y espera un `RequestDetail` como respuesta (ver `collaboration/api.ts`). Para no tocar el módulo de Sevan (`requests`), el backend de colaboración devuelve la misma forma de `RequestDetail` leyendo la solicitud tras el commit. El campo `comments` se embebe en `RequestDetail` como campo opcional (ya es `comments?: CommentDto[]` en `types.ts`). Los endpoints de listado (`GET /comments`) también existen independientemente para acceso programático.

**Notificación generada:** al crear un comentario, se notifica a los **demás participantes visibles** de la solicitud (excluyendo al autor del comentario):
- Solicitante dueño (si el autor no es el solicitante).
- Supervisor(es) activo(s) del área (si el autor no es supervisor de ese área).
- Técnico asignado (si lo hay y el autor no es ese técnico).

Texto de la notificación (en español): `"<Nombre del autor> comentó en la solicitud <SOL-AAAA-NNNNNN>."`.

**Errores:** `400` validación, `401` sin token, `404` solicitud no visible.

---

## 4. Evidencias

### Límites globales

| Regla | Valor |
|---|---|
| Máximo de archivos por solicitud | 5 |
| Tamaño máximo por archivo | 5 MB (5 242 880 bytes) |
| Tipos permitidos | JPEG (`.jpg`, `.jpeg`), PNG (`.png`), PDF (`.pdf`) |
| Validación de tipo | Por **magic bytes** (cabecera binaria), no por extensión ni `Content-Type` |
| Almacenamiento | Volumen persistente; nombre en disco = UUID generado; nombre original solo para mostrar |
| Seguridad | `X-Content-Type-Options: nosniff`; `Content-Disposition: attachment; filename*=UTF-8''<nombre-saneado>`; path traversal bloqueado |

**Magic bytes a verificar:**

| Tipo | Primeros bytes |
|---|---|
| JPEG | `FF D8 FF` |
| PNG | `89 50 4E 47 0D 0A 1A 0A` |
| PDF | `25 50 44 46` (`%PDF`) |

**Configuración de Spring:**
```yaml
spring.servlet.multipart.max-file-size: 6MB
spring.servlet.multipart.max-request-size: 6MB
```
`MaxUploadSizeExceededException` se mapea a `400` (no `413` ni `500`) en el manejador de excepciones del módulo.

**Límite Nginx:** `client_max_body_size 6m;` en el bloque `location /api/` de `frontend/nginx.conf`.

**Volumen:** configurable con la propiedad `app.evidences.storage-path` (variable de entorno `APP_EVIDENCES_STORAGE_PATH`). Documentada en `infra/.env.example` y en `docs/guia-levantamiento.md`. En pruebas se usa `@TempDir` de JUnit 5.

---

### `GET /api/v1/requests/{code}/evidences`

**Acceso:** token con visibilidad sobre la solicitud.

**Respuesta `200`:**

```json
[
  {
    "id": 42,
    "fileName": "foto-fuga.jpg",
    "kind": "IMG",
    "sizeBytes": 1048576
  }
]
```

`kind`: `"IMG"` para JPEG y PNG; `"PDF"` para PDF.

Lista vacía si no hay evidencias.

**Errores:** `404` solicitud no visible.

---

### `POST /api/v1/requests/{code}/evidences`

**Acceso:** token con visibilidad sobre la solicitud.

**Cuerpo:** `multipart/form-data` con campo `file` (el archivo binario).

**Validaciones (todas responden `400` con `errors: { file: "<mensaje>" }`):**
- Archivo vacío o campo `file` ausente.
- Tipo no permitido (validado por magic bytes).
- Tamaño > 5 MB.
- Solicitud ya con 5 evidencias (se incluye la nueva en el conteo antes de guardar).

**Respuesta `201`:** el `EvidenceDto` del archivo subido:

```json
{
  "id": 43,
  "fileName": "plano.pdf",
  "kind": "PDF",
  "sizeBytes": 2097152
}
```

**Errores:** `400` validación, `401` sin token, `404` solicitud no visible.

**Frontend:** subida con `fetch` + `FormData` (no con `apiPost` de JSON); un helper en `shared/api/client.ts` (cambio mínimo y aditivo, coordinado con Lita/Miguel).

---

### `GET /api/v1/requests/{code}/evidences/{id}`

**Acceso:** token con visibilidad sobre la solicitud.

**Descripción:** descarga el contenido binario de la evidencia. Se verifica que la evidencia pertenezca a la solicitud indicada por `{code}` **antes** de leer el disco; si no pertenece o no es visible, responde `404`.

**Respuesta `200`:** contenido binario con las cabeceras:

| Cabecera | Valor |
|---|---|
| `Content-Type` | `image/jpeg`, `image/png` o `application/pdf` según magic bytes validados al subir |
| `Content-Disposition` | `attachment; filename*=UTF-8''<nombre-original-saneado>` |
| `Content-Length` | tamaño en bytes |
| `X-Content-Type-Options` | `nosniff` |

**Frontend:** descarga autenticada con `fetch` + cabecera `Authorization` + `Blob`/`URL.createObjectURL`. Nunca el token en la URL.

**Errores:** `401` sin token, `404` si la evidencia no existe, no pertenece a la solicitud indicada, o la solicitud no es visible.

---

## 5. Notificaciones

### Tipos de evento y destinatarios

> **Decisión documentada:** una falla al generar notificaciones es **no bloqueante** (el cambio de estado se confirma aunque la notificación falle). Se implementa con `@TransactionalEventListener(phase = AFTER_COMMIT)` para que el evento de notificación se procese fuera de la transacción de negocio.

| Evento | Quién notificar | Excluir |
|---|---|---|
| Cambio de estado (cualquier transición) | Solicitante, técnico asignado (si hay), supervisores activos del área | El actor de la transición |
| Asignación o reasignación | Técnico asignado (el nuevo) | El actor (supervisor) |
| Comentario nuevo | Solicitante, técnico asignado (si hay), supervisores activos del área | El autor del comentario |

**Texto de notificación (en español):**
- Cambio de estado: `"La solicitud <SOL-AAAA-NNNNNN> pasó a <estado-en-español>."` (ej. `"pasó a En atención."`).
- Asignación: `"Se te asignó la solicitud <SOL-AAAA-NNNNNN>."`.
- Reasignación: `"Se te reasignó la solicitud <SOL-AAAA-NNNNNN>."`.
- Comentario: `"<Nombre del autor> comentó en la solicitud <SOL-AAAA-NNNNNN>."`.

**Cierre automático (72 h, módulo de Lita):** el mismo mecanismo de notificación queda previsto mediante un puerto `NotificationService.notify(List<Long> recipientIds, String text, String requestCode)`. Lita lo invoca desde su módulo `satisfaction` sin depender de la implementación concreta.

---

### `GET /api/v1/notifications`

**Acceso:** token (cualquier rol).

**Respuesta `200`:**

```json
[
  {
    "id": 7,
    "text": "La solicitud SOL-2026-000001 pasó a En atención.",
    "createdAt": "2026-10-17T14:30:00Z",
    "read": false,
    "requestCode": "SOL-2026-000001"
  }
]
```

Solo las del usuario autenticado, ordenadas por `createdAt` descendente (más recientes primero).

---

### `PATCH /api/v1/notifications/{id}/read`

**Acceso:** token. La notificación debe pertenecer al usuario autenticado. Si no pertenece o no existe ⇒ `404`.

**Cuerpo:** vacío.

**Respuesta `204`.**

---

### `POST /api/v1/notifications/read-all`

**Acceso:** token.

**Descripción:** marca como leídas **solo** las notificaciones del usuario autenticado.

**Respuesta `204`.**

---

## 6. Tipos TypeScript (adición a `shared/api/types.ts`)

Los tipos `CommentDto`, `EvidenceDto` y `NotificationDto` **ya están definidos** en `shared/api/types.ts`. No se duplican. Solo se agrega la familia de evidencias para la subida:

```ts
// Nuevos o a confirmar con el backend real:
export type CreateEvidenceResponse = EvidenceDto  // 201 de POST /evidences
```

`EvidenceDto.kind` permanece `'IMG' | 'PDF'` tal como lo consume el frontend.

---

## 7. Integración con módulos ajenos

| Módulo | Qué necesita `collaboration`/`notifications` | Cómo |
|---|---|---|
| `requests` (Sevan) | Visibilidad de la solicitud (requesterId, areaId, technicianId, code) | Puerto `RequestAccess` en `collaboration.application`; adaptador en `collaboration.infrastructure` que usa `requests.application` |
| `workflow` (Angela) | Evento de transición para generar notificaciones | `RequestTransitionedEvent` publicado con `ApplicationEventPublisher` dentro de `TransitionRequest.execute`; consumido por `notifications` con `@TransactionalEventListener(AFTER_COMMIT)` |
| `auth` (Miguel) | `PasswordHasher`, `UserRepository.findById`, `RefreshTokenRepository.revokeAllForUser` | Inyectados en el caso de uso `ChangePassword` de `auth.application` |

---

## 8. Opcionales (por definir antes de implementar — Fase G)

> No se implementan hasta confirmación explícita del responsable del módulo.

### `POST /api/v1/auth/password/reset-request` (público)

```json
{ "email": "usuario@unmsm.edu.pe" }
```

Envía correo con enlace de restablecimiento. Responde siempre `204` (no revela si el correo existe).

### `POST /api/v1/auth/password/reset` (público)

```json
{ "token": "<token-de-restablecimiento>", "newPassword": "NuevaClave9!" }
```

Responde `204` si tiene éxito; `400` si el token expiró o la contraseña es inválida.

### Notificaciones por correo

Al generar una notificación interna, si `app.notifications.email.enabled=true`, se envía también un correo (configurable; simulable en pruebas con un `MailSender` falso). Sin credenciales en el repositorio.

---

## 9. Migraciones planeadas

| Archivo | Contenido |
|---|---|
| `V20261017_2100__create_comments.sql` | Tabla `comments` |
| `V20261017_2110__create_evidences.sql` | Tabla `evidences` |
| `V20261017_2120__create_notifications.sql` | Tabla `notifications` |

Esquema detallado en cada archivo SQL.
