# Contrato: inicio de sesión con huella o passkey (WebAuthn)

> Complementa la sección «Autenticación» de [`contrato-v1.md`](contrato-v1.md).
> Fecha: 2026-11-19.
> Estado: **implementado como propuesta** en `feature/auth-passkeys-impl`, pendiente de acuerdo con Miguel (dueño del módulo `auth`). La sección 9 dice qué se decidió provisionalmente y qué quedó fuera.

## 1. Objetivo y alcance

Permitir que cualquier usuario activo inicie sesión con la huella digital (o el rostro, o el PIN) de su dispositivo, usando **passkeys** del estándar WebAuthn (W3C) y FIDO2.

- La huella **nunca sale del dispositivo**. El dispositivo verifica a la persona y firma un desafío con una clave privada que no se exporta. El backend solo guarda la **clave pública** y verifica la firma.
- El sistema **no recibe ni almacena datos biométricos**, así que no trata datos sensibles en el sentido de la Ley 29733 de Protección de Datos Personales.
- No se puede exigir «huella» en particular: el estándar solo informa que el usuario fue verificado. El dispositivo decide si usa huella, rostro o PIN. En la interfaz se llama **«huella o passkey»**.
- La passkey **complementa** a la contraseña, no la reemplaza: la contraseña sigue siendo el respaldo (dispositivo perdido, equipo sin biometría, recuperación de cuenta).

Fuera de alcance de esta versión: cuentas sin contraseña, verificación de atestación o listas de autenticadores permitidos (AAGUID), exigir un segundo factor y cualquier lector de huella que envíe la huella al servidor.

## 2. Reglas generales

- Base `/api/v1`, JSON en UTF-8, fechas ISO-8601 en UTC y errores `application/problem+json`, igual que [`contrato-v1.md`](contrato-v1.md).
- Los binarios de WebAuthn (desafíos, ids de credencial, firmas) viajan en **base64url sin relleno**, con la forma JSON del estándar (WebAuthn Level 3: `PublicKeyCredentialCreationOptionsJSON`, `PublicKeyCredentialRequestOptionsJSON`, `RegistrationResponseJSON`, `AuthenticationResponseJSON`). Así el frontend usa `PublicKeyCredential.parseCreationOptionsFromJSON` / `toJSON()` o `@simplewebauthn/browser` sin conversiones propias.
- **Rutas:** el inicio de sesión va en `/auth/passkeys/**`, que ya es público. La gestión va en `/me/passkeys/**`, que exige token por la regla general; así no se repite la trampa de `PUT /auth/password` (ver [`collaboration-notifications-v1.md`](collaboration-notifications-v1.md) §2).
- Todo usuario activo puede registrar passkeys, sea cual sea su rol. Un usuario desactivado no puede iniciar sesión con ellas (`401`, como con contraseña).

## 3. Configuración por ambiente

Una passkey queda atada al dominio donde se creó (*Relying Party ID*). Las de `localhost`, staging y producción no se comparten, y si el dominio cambia hay que registrarlas de nuevo.

| Propiedad | Variable de entorno | Ejemplo | Regla |
|---|---|---|---|
| `app.auth.webauthn.rp-id` | `APP_AUTH_WEBAUTHN_RP_ID` | `localhost`, `servicios.example.pe` | Dominio sin esquema ni puerto. Por defecto `localhost` |
| `app.auth.webauthn.rp-name` | `APP_AUTH_WEBAUTHN_RP_NAME` | `Servicios Universitarios UNMSM` | Nombre que muestra el dispositivo |
| `app.auth.webauthn.allowed-origins` | `APP_AUTH_WEBAUTHN_ALLOWED_ORIGINS` | `http://localhost:8088,http://localhost:5173` | Orígenes exactos (esquema, host y puerto) aceptados en `clientDataJSON.origin` |
| `app.auth.webauthn.challenge-ttl` | `APP_AUTH_WEBAUTHN_CHALLENGE_TTL` | `PT5M` | Vigencia de un desafío |
| `app.auth.webauthn.max-passkeys-per-user` | — | `10` | Límite por usuario |

WebAuthn exige HTTPS fuera de `localhost`. En staging y producción el origen permitido es el dominio público con `https://`.

## 4. Inicio de sesión con passkey (público)

Inicio de sesión **sin escribir el correo** (credenciales *discoverable*): el usuario pulsa «Ingresar con huella o passkey» y el dispositivo le ofrece las passkeys guardadas para el dominio. No pedir el correo evita revelar si una cuenta tiene passkeys.

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `POST /auth/passkeys/login/options` | — | `200` `{ challengeId, publicKey: PublicKeyCredentialRequestOptionsJSON }` · `429` |
| `POST /auth/passkeys/login` | `{ challengeId, credential: AuthenticationResponseJSON }` | `200` `TokenResponse` + cookie `refresh_token` (igual que `POST /auth/login`) · `400` cuerpo mal formado · `401` · `429` |

`publicKey` de las opciones: `challenge` (32 bytes aleatorios), `rpId`, `timeout: 60000`, `userVerification: "required"` y `allowCredentials: []`.

Verificación, en este orden; cualquier fallo responde `401` con el mismo `detail` genérico («No pudimos verificar tu passkey») para no revelar cuál falló:

1. El desafío `challengeId` existe, es de inicio de sesión, no venció y no se usó. Se marca como usado **antes** de seguir, para que no sirva dos veces.
2. `clientDataJSON`: `type = "webauthn.get"`, `challenge` igual al emitido y `origin` en `allowed-origins`.
3. `authenticatorData`: hash del `rpId` correcto, bandera *User Present* y bandera *User Verified* activas (la huella o el PIN se verificó).
4. La credencial (`credential.id`) está registrada y su `userHandle` corresponde a su usuario.
5. La firma es válida con la clave pública guardada.
6. Contador de firmas: si el guardado y el recibido son mayores que 0 y el recibido no es mayor, se rechaza (posible clon) y se registra en el log. Las passkeys sincronizadas envían siempre 0; eso se acepta.
7. El usuario está activo.

Si todo pasa: se actualizan `sign_count` y `last_used_at` y se emite la sesión con el mismo código que `POST /auth/login` (token de acceso de 15 min y cookie de renovación con los mismos atributos). `POST /auth/refresh` y `POST /auth/logout` no cambian.

**Límite de intentos:** se reutiliza el `LoginAttemptLimiter`. Cuentan los `401` por IP y por credencial; al superarse, `429` con `Retry-After`, igual que el login con contraseña. `POST /auth/passkeys/login/options` también se limita por IP, para que no se puedan generar desafíos sin control.

## 5. Gestión de passkeys (con token)

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `POST /me/passkeys/registration/options` | `{ currentPassword }` | `200` `{ challengeId, publicKey: PublicKeyCredentialCreationOptionsJSON }` · `400` con `errors.currentPassword` si no coincide · `409` si ya tiene el máximo · `429` |
| `POST /me/passkeys` | `{ challengeId, name?, credential: RegistrationResponseJSON }` | `201` `PasskeyDto` · `400` registro inválido o desafío vencido · `409` credencial ya registrada |
| `GET /me/passkeys` | — | `200` `PasskeyDto[]`, de la más reciente a la más antigua |
| `PATCH /me/passkeys/{id}` | `{ name }` | `200` `PasskeyDto` · `400` · `404` si no es del usuario |
| `DELETE /me/passkeys/{id}` | — | `204` · `404` si no es del usuario |

```ts
PasskeyDto = { id: number, name: string, createdAt: string, lastUsedAt: string | null, synced: boolean }
```

- `currentPassword` (**confirmación con contraseña**): un token de acceso robado dura 15 minutos, pero una passkey registrada con él daría acceso permanente. Pedir la contraseña antes de registrar evita ese salto. Se valida igual que en `PUT /auth/password` y los intentos fallidos cuentan para el límite de inicio de sesión del usuario.
- Opciones de registro: `rp { id, name }`; `user { id: userHandle, name: email, displayName: fullName }`, donde `userHandle` son 64 bytes aleatorios por usuario (nunca el id ni el correo); `pubKeyCredParams` ES256 (`-7`) y RS256 (`-257`); `authenticatorSelection { residentKey: "required", userVerification: "required" }`; `attestation: "none"`; `excludeCredentials` con las passkeys que ya tiene, para no registrar dos veces el mismo dispositivo; `timeout: 60000`.
- Verificación del registro: desafío de registro vigente, sin usar y emitido para **ese** usuario; `type = "webauthn.create"`; origen permitido; hash del `rpId`; banderas *User Present* y *User Verified*; algoritmo admitido. No se verifica la atestación (`attestation: "none"`).
- `name`: de 1 a 60 caracteres sin espacios sobrantes. Si no se envía, «Passkey del <fecha>». `synced` indica si el proveedor la respalda en la nube (bandera *Backup State*), útil para avisar que borrarla en un equipo no la quita de los demás.
- Borrar una passkey solo afecta futuros inicios de sesión; las sesiones abiertas siguen hasta que venzan.
- **Pendiente:** avisar al usuario con una notificación en la app cada vez que se agrega o se elimina una passkey, para que detecte un registro que no hizo. Hoy las notificaciones exigen una solicitud asociada (`notifications.request_id NOT NULL`, módulo de AngelCarrasco), así que esta versión no lo hace; ver §9.

## 6. Relación con el resto de la cuenta

- **Cambio de contraseña** (`PUT /auth/password`): no borra las passkeys; son un factor independiente.
- **Usuario desactivado** por el administrador: sus passkeys se conservan, pero no puede iniciar sesión (`401`). Al reactivarlo vuelven a funcionar.
- **Administración:** esta versión no agrega endpoints de administración de passkeys. Si una cuenta queda comprometida, el administrador la desactiva.

## 7. Modelo de datos (propuesto)

Migración nueva en `db/migration`, con fecha y hora como pide el equipo:

```sql
ALTER TABLE users ADD COLUMN webauthn_user_handle BINARY(64) NULL UNIQUE;  -- se crea al registrar la primera passkey

CREATE TABLE passkeys (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  credential_id VARBINARY(1023) NOT NULL,
  public_key VARBINARY(2048) NOT NULL,          -- clave pública COSE; nunca datos biométricos
  sign_count BIGINT NOT NULL,
  transports VARCHAR(100) NULL,                  -- p. ej. "internal,hybrid"
  backup_eligible BOOLEAN NOT NULL,
  backed_up BOOLEAN NOT NULL,
  name VARCHAR(60) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  last_used_at DATETIME(6) NULL,
  CONSTRAINT uk_passkeys_credential UNIQUE (credential_id),
  CONSTRAINT fk_passkeys_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE webauthn_challenges (
  id CHAR(36) PRIMARY KEY,                       -- challengeId (UUID)
  purpose VARCHAR(12) NOT NULL,                  -- LOGIN o REGISTRATION
  user_id BIGINT NULL,                           -- solo en REGISTRATION
  challenge BINARY(32) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL
);
```

Los desafíos vencidos se borran periódicamente. Guardarlos en la base, y no en memoria, permite varias instancias del backend.

## 8. Implementación prevista

- **Backend:** casos de uso en `auth` (`StartPasskeyLogin`, `FinishPasskeyLogin`, `StartPasskeyRegistration`, `RegisterPasskey`, `ListPasskeys`, `RenamePasskey`, `DeletePasskey`), con la verificación criptográfica de la librería **webauthn4j** (`com.webauthn4j:webauthn4j-core`) detrás de un puerto propio. No se usa el soporte WebAuthn de Spring Security porque está pensado para sesiones con formulario y el proyecto usa JWT sin estado. `FinishPasskeyLogin` termina en la misma emisión de sesión que `Login`.
- **Frontend:** botón «Ingresar con huella o passkey» en el login, visible solo si el navegador lo admite (`window.PublicKeyCredential`); ventana «Huella y passkeys» desde el menú de perfil para registrar (pidiendo la contraseña) y borrar; renombrar existe en la API pero aún no en la interfaz. Si el usuario cancela el diálogo del dispositivo (`NotAllowedError`), se muestra un aviso discreto, no un error.
- **CSP:** la política actual de nginx no bloquea WebAuthn. Si en el futuro se agrega `Permissions-Policy`, debe permitir `publickey-credentials-get` y `publickey-credentials-create` para `self`.
- **Pruebas:** unitarias de cada regla de verificación (desafío vencido o reutilizado, origen, `rpId`, banderas, contador, usuario inactivo); de API con un autenticador simulado de webauthn4j contra MySQL (Testcontainers); E2E con el **autenticador virtual de Chromium** de Playwright (CDP `WebAuthn.addVirtualAuthenticator`): registrar la passkey desde el perfil, cerrar sesión y entrar con ella.

## 9. Decisiones abiertas (para acordar con Miguel)

1. **Confirmación con contraseña al registrar** (§5). Implementado: es la defensa contra un token robado.
2. **Inicio de sesión sin correo** (§4). Implementado; la alternativa (pedir el correo primero) revela qué cuentas tienen passkeys.
3. **Notificación en la app** al agregar o quitar una passkey (§5). **Fuera de esta versión:** requiere que el módulo de notificaciones (AngelCarrasco) admita avisos de cuenta, sin solicitud asociada.
4. **Límite de 10 passkeys** por usuario y **vigencia de 5 minutos** del desafío. Implementado y configurable. El inicio de sesión con passkey comparte el límite de fallos del login (5 por cliente en 15 minutos) y los desafíos de inicio de sesión se limitan a 30 por cliente en la misma ventana (`app.auth.webauthn.max-challenges-per-client`).
5. **Dominio por ambiente** (§3): fijar el dominio de staging y producción antes de habilitarlo allí (depende de la tarea T1 de la Fase III), para que los usuarios no tengan que registrar de nuevo sus passkeys. `docker-compose` toma `APP_AUTH_WEBAUTHN_RP_ID` y `APP_AUTH_WEBAUTHN_ALLOWED_ORIGINS` (por defecto `localhost` y `http://localhost:${FRONTEND_PORT}`).
