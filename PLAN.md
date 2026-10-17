# Plan de trabajo del equipo

Sistema Web Inteligente para la Gestión Integral de Servicios Universitarios.

- **Spec (fuente de verdad):** `docs/specs/2026-10-14-servicios-universitarios-design.md`
- **Contrato de la API:** `docs/api/` (se actualiza antes de implementar cada endpoint nuevo)
- **Base técnica disponible en `dev`:** monorepo, backend Spring Boot + MySQL + Flyway, frontend React + TypeScript, Docker Compose y CI en GitHub Actions.

## 1. Organización

Cada integrante es dueño de **módulos completos**: backend, frontend, pruebas y documentación de su parte. Así cada uno avanza sin depender del ritmo de los demás y las integraciones se reducen a los contratos de la API.

| Integrante | Módulo | Paquetes backend | Carpetas frontend |
|---|---|---|---|
| Miguel | Plataforma, seguridad y autenticación | `shared`, `auth` | `app/`, `features/auth` |
| Sevan | Catálogos y gestión de solicitudes | `catalog`, `requests` | `features/requests` |
| Angela | Flujo de atención y documentación de análisis | `workflow` | `features/workflow` |
| AngelCarrasco | Colaboración y notificaciones | `collaboration`, `notifications` | `features/collaboration`, `features/notifications` |
| AngelCruz | Administración, triage e indicadores | `admin`, `triage`, `dashboard` | `features/admin`, `features/dashboard` |
| Lita | Satisfacción, calidad y operación | `satisfaction` | `features/satisfaction` |

## 2. Responsabilidades por integrante

### Miguel — Plataforma, seguridad y autenticación
- Seguridad JWT: token de acceso de 15 minutos y renovación de 7 días con rotación en cookie segura.
- Registro de solicitantes (correo `@unmsm.edu.pe`), inicio y cierre de sesión, autorización por rol.
- Esquema base de la base de datos y datos de demostración.
- Frontend: enrutado, layout, navegación por rol, páginas de login y registro, manejo de sesión.
- CI/CD, despliegue en staging y producción con Dokploy, integración de ramas en `dev`.
- **Spec:** 3.4, 4, 5 (auth), 10.

### Sevan — Catálogos y gestión de solicitudes
- Catálogos de consulta: áreas, categorías, prioridades y ubicaciones.
- Registro de solicitudes: código `SOL-AAAA-NNNNNN`, validaciones (descripción de 20 a 2000 caracteres), área derivada de la categoría, vencimiento según el SLA de la prioridad.
- Consulta según rol: listado con filtros y detalle; marca de solicitudes vencidas.
- Frontend: "Mis solicitudes", "Nueva solicitud" y detalle de la solicitud.
- **Spec:** 5 (requests), 5.1, 7, 8.

### Angela — Flujo de atención y documentación de análisis
- Máquina de estados: las transiciones de la sección 6 del spec, con permisos por rol y `409` ante transiciones inválidas.
- Asignación y reasignación a técnicos; historial de cada cambio de estado.
- Frontend: bandeja del supervisor (evaluar, asignar, cerrar con motivo) y bandeja del técnico (atender, resolver).
- Documentación de análisis: problema, actores, alcance, requerimientos, épicas, historias de usuario con escenarios BDD y wireframes (`docs/fase-1/`).
- **Spec:** 4, 5 (workflow), 6.

### AngelCarrasco — Colaboración y notificaciones
- Comentarios en las solicitudes.
- Evidencias adjuntas: hasta 5 archivos JPG, PNG o PDF de 5 MB cada uno, servidos solo a usuarios autorizados.
- Notificaciones dentro de la aplicación ante cambios de estado y asignaciones; notificaciones por correo.
- Cambio de contraseña y recuperación por correo.
- **Spec:** 5 (requests: comentarios y evidencias; notifications; auth: contraseña), 5.1.

### AngelCruz — Administración, triage e indicadores
- Pantallas de administración: usuarios, áreas, categorías, prioridades (con horas de SLA) y ubicaciones; consulta de estados en solo lectura.
- Triage automático: reglas de palabras clave que sugieren categoría y prioridad, y su gestión desde administración.
- Dashboard con los nueve indicadores del enunciado, filtros por fechas y área, y vista según el rol.
- **Spec:** 5 (admin, triage, dashboard), 5.1.

### Lita — Satisfacción, calidad y operación
- Encuesta de satisfacción: confirmación y calificación (1 a 5) al resolver; cierre automático a las 72 horas.
- Estrategia e informe de pruebas: pruebas funcionales y de aceptación a partir de los escenarios BDD.
- Seguridad: revisión OWASP Top 10, análisis estático (SAST) en CI y escaneo dinámico (DAST) sobre staging.
- Pruebas de carga y estrés con k6.
- Observabilidad: monitoreo con Uptime Kuma, logs estructurados, métricas y alertas.
- Consolidación del manual de usuario y del manual técnico.
- **Spec:** 3.5, 5 (satisfaction), 9, 11 (Sprint 3).

## 3. Cronograma

| Semanas | Miguel | Sevan | Angela | AngelCarrasco | AngelCruz | Lita |
|---|---|---|---|---|---|---|
| 8–10 | Seguridad, autenticación, layout | Catálogos, registro y consulta de solicitudes | Flujo de estados, bandejas, documentación de análisis | Diseño del contrato de comentarios, evidencias y notificaciones | Backend de administración de catálogos y usuarios | Plan de pruebas, entorno de staging y monitoreo base |
| 10 | **Presentación parcial: todos** | | | | | |
| 10–12 | Despliegue en producción, revisión de seguridad de la autenticación | Vencidas y filtros avanzados | Reasignación, rechazo de solución, mejoras de bandejas | Comentarios, evidencias, notificaciones, contraseña | Pantallas de administración, triage, dashboard | Satisfacción y cierre automático, SAST en CI |
| 13–14 | Soporte de integración y despliegue | Pruebas de su módulo y correcciones | Pruebas de su módulo y correcciones | Pruebas de su módulo y correcciones | Pruebas de su módulo y correcciones | DAST, pruebas de carga, observabilidad, informe de pruebas |
| 15 | Manual técnico de su módulo | Manual técnico de su módulo | Manual técnico de su módulo | Manual técnico de su módulo | Manual técnico de su módulo | Consolidación de manuales e informe final |
| 16 | **Sustentación final: todos** | | | | | |

## 4. Forma de trabajo

- **Ramas:** `feature/<modulo>-<descripcion>` desde `dev`. Ningún cambio entra directo a `dev` ni a `main`.
- **Pull requests a `dev`:** CI en verde y la revisión de al menos un integrante de otro módulo. `main` solo recibe releases desde `dev`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`, `ci:`).
- **Pruebas:** se escribe primero la prueba que falla, luego el código. Cada endpoint tiene pruebas de integración con MySQL real (Testcontainers).
- **Contratos:** antes de implementar un endpoint nuevo se documenta en `docs/api/`. Si un cambio afecta el módulo de otro integrante, se acuerda en el pull request.
- **Migraciones de base de datos:** versión con fecha y hora para evitar choques entre ramas, por ejemplo `V20261020_1530__create_comments.sql`. Nunca se edita una migración ya integrada en `dev`.
- **Secretos:** solo en variables de entorno; `infra/.env` no se versiona.
- **Seguimiento:** cada funcionalidad tiene su documento en `odd/tasks/<funcionalidad>.md` con tareas, evidencias de pruebas y pendientes.

## 5. Definición de terminado

Una funcionalidad está terminada cuando:
1. Cumple las reglas del spec y el contrato de `docs/api/`.
2. Tiene pruebas automatizadas de backend y frontend en verde.
3. Pasa el CI y fue revisada por otro integrante.
4. Está integrada en `dev` y funciona en staging.
5. Su documentación (contrato, escenarios BDD cubiertos, notas del manual) está actualizada.
