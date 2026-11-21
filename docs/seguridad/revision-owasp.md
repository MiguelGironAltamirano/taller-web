# Revisión OWASP Top 10 — 2021

**Sistema:** Gestión de solicitudes de servicios universitarios (UNMSM)  
**Revisado contra:** rama `dev`, 2026-11-20  
**Autor:** Lita Huanca

---

## Tabla de controles

| # | Categoría | Control implementado | Evidencia | Riesgo residual |
|---|---|---|---|---|
| A01 | Control de acceso deficiente | `AccessPolicy` define 4 roles (SOLICITANTE, TÉCNICO, SUPERVISOR, ADMINISTRADOR) con reglas framework-free; `canViewRequest` verifica propietario, área del supervisor o técnico asignado. `SecurityConfig` con `@EnableMethodSecurity` requiere autenticación en todas las rutas salvo las públicas declaradas. | `AccessPolicy.java`, `SecurityConfig.java` (l. 46–47), `AuthorizationFailureTests.java` | Bajo |
| A02 | Fallas criptográficas | Contraseñas con `BCryptPasswordEncoder`. JWT firmado con HS256; clave mínima de 32 bytes, validada en arranque (`SecurityConfig.java` l. 93–96). Token de acceso en memoria (15 min); token de renovación en cookie `HttpOnly`, `Secure`, `SameSite=Strict`. HTTPS en staging/producción (Railway, Let's Encrypt). | `SecurityConfig.java`, `JwtProperties.java`, `infra/.env.example`, `docs/fase-3/evidencias.md` | Bajo |
| A03 | Inyección | Acceso a datos vía Spring Data JPA / JDBC con consultas parametrizadas por diseño del framework. Bean Validation en todos los DTOs de entrada. Archivos subidos validados por magic bytes (`FileTypeValidator.java`): acepta solo JPG (`FF D8 FF`), PNG (`89 50 4E 47…`) y PDF (`25 50 44 46`); rechaza todo lo demás con `400`. | `FileTypeValidator.java`, repositorios `SpringData*Repository.java`, tests `*ApiTests.java` | Bajo |
| A04 | Diseño inseguro | `LoginAttemptLimiter` aplica límite deslizante por IP+email y por email solo, con cálculo atómico (`synchronized`) antes de verificar la contraseña; usa `DUMMY_HASH` para igualar el tiempo de respuesta cuando el email no existe (mitiga timing attack). `AllowedTransitions` impone la máquina de estados de solicitudes. | `LoginAttemptLimiter.java`, `Login.java` (l. 15, 35), `AllowedTransitions.java`, `LoginAttemptLimitTests.java` | Bajo |
| A05 | Configuración de seguridad incorrecta | Cabeceras en nginx: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, CSP estricta (`default-src 'self'`). CORS solo permite orígenes explícitos; arranca con excepción si se configura un comodín (`SecurityConfig.java` l. 76–79). Sesión sin estado (stateless JWT). Datos demo nunca en producción (`APP_FLYWAY_LOCATIONS`). | `frontend/nginx.conf`, `SecurityConfig.java`, `SecurityHeadersAndCorsTests.java`, `infra/.env.example` | Bajo |
| A06 | Componentes vulnerables y desactualizados | Pipeline con `pnpm audit --prod --audit-level=high` en el job `frontend` (CI). CodeQL analiza `java-kotlin` y `javascript-typescript` en cada PR y semanalmente. Dependabot activa PRs semanales para maven, npm, github-actions y docker. | `.github/workflows/codeql.yml`, `.github/dependabot.yml`, `.github/workflows/ci.yml` (l. 46) | Medio — el análisis inicial aún no ha corrido; los hallazgos se revisarán en la primera ejecución de CodeQL tras el merge. |
| A07 | Fallas de identificación y autenticación | Login devuelve siempre `"Credenciales inválidas"` sin importar si el correo no existe, la contraseña es incorrecta o la cuenta está inactiva (no hay enumeración de usuarios). Timing equalizado con `DUMMY_HASH`. Límite de intentos responde `429` con `Retry-After`. Sesión única por usuario (PR #27). Cierre de sesión invalida el token de renovación. Cambio de contraseña requiere la contraseña actual. | `Login.java`, `AuthExceptionHandler.java` (l. 56–59), `LoginAttemptLimiter.java`, `LoginAttemptLimitTests.java` | Bajo |
| A08 | Fallas de integridad de software y datos | Todo cambio entra por PR con revisión; CI obligatorio (`backend`, `frontend`, `smoke and e2e`) antes de fusionar. Nada entra directo a `dev` ni a `main`. Commits con Conventional Commits. Pipeline firma implícitamente cada release al crear el tag desde `main`. | `.github/workflows/ci.yml`, `docs/fase-1/04-arquitectura.md`, rama y política de PR documentada en `plan/README.md` | Bajo |
| A09 | Registro y monitoreo insuficientes | Spring Boot registra en stdout (nivel INFO por defecto); Docker Compose redirige al daemon. `ListAuditEvents` expone el historial de cambios de solicitudes al administrador (`/api/v1/admin/audit`). No hay centralización de logs ni alertas en tiempo real aún (previsto para Fase IV). | `ListAuditEvents.java`, `AuditEvent.java`, `AdminSettingsController.java` | Medio — sin alertas automáticas ante intentos masivos o errores inesperados; el administrador debe revisar el historial manualmente. Se planifica observabilidad en Fase IV. |
| A10 | SSRF | El backend no realiza peticiones HTTP a URLs proporcionadas por el usuario. No se detectan usos de `RestTemplate`, `WebClient`, `HttpURLConnection` ni `URL.openConnection()` en código de producción. Los archivos de evidencia se almacenan en el sistema de archivos local (`APP_EVIDENCES_STORAGE_PATH`). | Revisión de código (`grep` sobre `src/main`), `EvidencesController.java`, `UploadEvidence.java` | Bajo |

---

## Notas

- **A06 / A09:** los riesgos residuales medios están acotados al período inicial antes de que CodeQL produzca su primer informe y antes de que se implemente observabilidad centralizada (Fase IV).
- La revisión se realizó con asistencia de IA (Claude Sonnet 4.6, 2026-11-20). Cada afirmación fue verificada contra el código fuente antes de registrarla. Ver `docs/registro-uso-ia.md`.
