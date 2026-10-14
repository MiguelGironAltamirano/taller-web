# Sistema Web Inteligente para la Gestión Integral de Servicios Universitarios — Diseño

- **Fecha:** 2026-10-14
- **Estado:** borrador para revisión
- **Fuente:** `Proyecto - Taller de construccion de software web.md` (enunciado del curso)

## 1. Objetivo

Construir una aplicación web full-stack que permita registrar, priorizar, asignar, atender y monitorear solicitudes de servicios universitarios (mantenimiento, infraestructura, soporte informático y solicitudes administrativas), con trazabilidad completa y medición de la calidad del servicio.

El éxito se mide en la sustentación final: demostrar el ciclo completo Especificar → Diseñar → Construir → Integrar → Probar → Asegurar → Desplegar → Monitorear → Mejorar, no solo una aplicación que funcione.

## 2. Alcance

### Incluido
- Los cinco módulos mínimos del enunciado (autenticación, registro de solicitudes, gestión de solicitudes, dashboard, administración).
- Tres mejoras: notificaciones, triage automático por reglas y encuesta de satisfacción.
- Despliegue en entornos de staging y producción, CI/CD, seguridad OWASP, pruebas automatizadas y observabilidad.

### Excluido
- Reservas de ambientes (dominio independiente con calendarios y disponibilidad).
- Aplicación móvil nativa.
- Integración con sistemas institucionales existentes (SUM, correo institucional vía SSO).

### Supuestos
- Hoy es la semana 8 de 16. Las semanas van de lunes a domingo: semana 8 = 12–18 oct, semana 10 = 26 oct–1 nov, semana 16 = 7–13 dic de 2026.
- Desarrollo individual al inicio; el grupo se incorporará después con el mismo flujo de ramas.

## 3. Arquitectura

### 3.1 Repositorio (monorepo)

```text
/
├── backend/            Spring Boot (Maven)
├── frontend/           React + Vite + TypeScript
├── infra/              docker-compose, configuración de despliegue, pruebas de carga y DAST
├── docs/               specs, documentos de fase, informes y manuales
├── odd/tasks/          seguimiento de features
└── .github/workflows/  CI
```

### 3.2 Backend

- **Plataforma:** Java 25 LTS y la última versión estable de Spring Boot compatible con Java 25 al momento del scaffolding (versión fijada en `pom.xml`).
- **Estilo:** monolito modular organizado por feature. Paquete raíz `pe.edu.unmsm.servicios`, con un paquete por módulo: `auth`, `requests`, `workflow`, `dashboard`, `admin`, `notifications`, `triage`, `satisfaction` y `shared`.
- **Capas dentro de cada módulo (hexagonal liviano):**
  - `domain`: entidades, value objects y reglas; sin dependencias de Spring.
  - `application`: casos de uso y puertos.
  - `infrastructure`: adaptadores REST (controladores y DTOs), JPA y otros adaptadores de salida.
- **Reglas de dependencia:** `domain` no depende de nada externo; los módulos se comunican solo a través de su capa `application`. ArchUnit verifica estas reglas en CI.
- **Persistencia:** MySQL 8 con Spring Data JPA; esquema versionado con Flyway.
- **API:** REST bajo `/api/v1`, documentada con OpenAPI (springdoc) y Swagger UI.
- **Errores:** formato único RFC 7807 (`application/problem+json`) con un manejador global.
- **Librerías:** Spring Web, Spring Security, Spring Data JPA, Bean Validation, Flyway, springdoc-openapi, Actuator + Micrometer, OpenTelemetry, Testcontainers y ArchUnit.

### 3.3 Frontend

- **Plataforma:** React + Vite + TypeScript (modo estricto).
- **Estructura:** carpetas por feature (`features/auth`, `features/requests`, …); patrón container-presentational; componentes compartidos con atomic design en `shared/ui`.
- **Librerías:** React Router, TanStack Query (estado de servidor), React Hook Form + Zod (formularios y validación), Tailwind CSS (diseño responsive), Recharts (dashboard), MSW (API simulada para desarrollo y pruebas), Vitest + Testing Library.
- **Sesión:** el token de acceso se guarda en memoria; el token de renovación viaja en una cookie `httpOnly`, `Secure`, `SameSite=Strict`.

### 3.4 Seguridad

- Autenticación JWT: token de acceso de 15 minutos y token de renovación de 7 días con rotación.
- Contraseñas con BCrypt.
- Autorización por rol en cada endpoint y verificación de pertenencia (un solicitante solo ve sus solicitudes; un técnico solo las que tiene asignadas).
- Validación de entradas en el backend (Bean Validation) y en el frontend (Zod).
- Secretos solo en variables de entorno; ningún valor sensible en el repositorio.
- Cabeceras de seguridad, CORS restringido al dominio del frontend y límite de intentos de login.
- Revisión contra OWASP Top 10 en la Fase III.

### 3.5 Observabilidad

- Logs estructurados en JSON con identificador de traza.
- Métricas con Actuator y Micrometer (endpoint Prometheus).
- Trazas con OpenTelemetry.
- Paneles y alertas en la Fase IV (ver decisión abierta D3).

## 4. Roles

| Rol | Descripción |
|-----|-------------|
| Solicitante | Estudiante, docente o personal administrativo (atributo `tipo`). Registra y sigue sus solicitudes. |
| Técnico | Atiende las solicitudes asignadas. Pertenece a un área responsable. |
| Supervisor | Evalúa, prioriza, asigna y supervisa las solicitudes de su área. |
| Administrador | Gestiona usuarios, catálogos, reglas de triage y configuración. |

El autorregistro solo crea cuentas de Solicitante y exige un correo del dominio `unmsm.edu.pe`. Las cuentas de Técnico, Supervisor y Administrador las crea un Administrador.

## 5. Módulos y features

Prioridad: **M** = MVP (semana 10), **S** = Release Candidate (semana 12), **C** = opcional si hay tiempo.

| Módulo | Feature | Prioridad |
|--------|---------|-----------|
| auth | Registro de solicitante, login, renovación y cierre de sesión con JWT | M |
| auth | Autorización por rol | M |
| auth | Cambio de contraseña | S |
| auth | Recuperación de contraseña por correo | C |
| requests | Crear solicitud con código, categoría, ubicación, descripción y prioridad | M |
| requests | Listar mis solicitudes y ver el detalle con su historial | M |
| requests | Comentarios | S |
| requests | Evidencias adjuntas | S |
| workflow | Máquina de estados con transiciones por rol | M |
| workflow | Asignación y reasignación a técnico | M |
| workflow | Historial de cambios (quién, cuándo, estado anterior y nuevo, motivo) | M |
| workflow | SLA por prioridad y marca de solicitudes vencidas | S |
| dashboard | Nueve indicadores del enunciado, con filtros por rango de fechas y área, y vista por rol | S |
| admin | Catálogos y usuarios precargados (seed) | M |
| admin | Pantallas de gestión de usuarios, categorías, prioridades, áreas y ubicaciones | S |
| admin | Consulta (solo lectura) de los estados | S |
| notifications | Notificaciones dentro de la aplicación por cambio de estado y asignación | S |
| notifications | Notificaciones por correo | C |
| triage | Sugerencia de categoría y prioridad según reglas de palabras clave | S |
| triage | Gestión de reglas desde admin | S |
| satisfaction | Confirmación y calificación (1–5 y comentario) al resolver | S |

### 5.1 Reglas de negocio

- **Código de solicitud:** `SOL-AAAA-NNNNNN`, correlativo por año, único e inmutable.
- **Campos obligatorios al registrar:** categoría, ubicación, descripción (20 a 2000 caracteres) y prioridad sugerida. El supervisor puede cambiar la prioridad durante la evaluación.
- **Ubicación:** catálogo jerárquico Facultad → Pabellón → Ambiente.
- **Área de la solicitud:** es siempre el área por defecto de la categoría elegida, y define qué supervisor la recibe.
- **SLA:** cada prioridad define horas de atención. Valores iniciales: Crítica 8 h, Alta 24 h, Media 72 h, Baja 120 h. La fecha de vencimiento es la fecha de registro más las horas de la prioridad, y se recalcula si el supervisor cambia la prioridad durante la evaluación. Una solicitud está vencida si no llegó a "Resuelta" antes de esa fecha.
- **Evidencias:** hasta 5 archivos por solicitud, máximo 5 MB cada uno, solo JPG, PNG o PDF. Se guardan en un volumen persistente y se sirven solo a usuarios autorizados.
- **Triage:** una regla relaciona palabras clave con una categoría y una prioridad. Al escribir la descripción, el sistema sugiere los valores de la regla con más coincidencias; el solicitante puede aceptarlos o cambiarlos. Sin coincidencias, no hay sugerencia.
- **Satisfacción:** al pasar a "Resuelta", el solicitante confirma y califica, y la solicitud pasa a "Cerrada". Si no responde en 72 horas, se cierra automáticamente sin calificación. Si rechaza la solución, vuelve a "En atención".

## 6. Máquina de estados

```text
Registrada → En evaluación → Asignada → En atención → Resuelta → Cerrada
                  │                                       │
                  └──► Cerrada (no procede / duplicada)   └──► En atención (solución rechazada)
```

| Desde | Hacia | Quién | Condición |
|-------|-------|-------|-----------|
| Registrada | En evaluación | Supervisor del área | — |
| En evaluación | Asignada | Supervisor del área | Requiere técnico del área |
| Asignada | Asignada | Supervisor del área | Reasignación; requiere motivo |
| Asignada | En atención | Técnico asignado | — |
| En atención | Resuelta | Técnico asignado | Requiere comentario de solución |
| Resuelta | Cerrada | Solicitante, o el sistema a las 72 h | Calificación opcional |
| Resuelta | En atención | Solicitante | Requiere motivo |
| Registrada o En evaluación | Cerrada | Supervisor del área | Requiere motivo: "No procede" o "Duplicada" |

Toda transición genera un registro de historial y una notificación a los interesados. Cualquier transición no listada se rechaza con `409 Conflict`.

## 7. Modelo de datos (principal)

| Entidad | Campos clave |
|---------|--------------|
| Usuario | id, nombre, correo, hash de contraseña, rol, tipo (solo solicitante), área (técnico y supervisor), activo |
| Area | id, nombre, activo |
| Categoria | id, nombre, área por defecto, activo |
| Prioridad | id, nombre, horas de SLA, orden |
| Ubicacion | id, nombre, nivel (facultad, pabellón, ambiente), padre |
| Solicitud | id, código, solicitante, categoría, ubicación, descripción, prioridad, estado, área, técnico, fecha de registro, fecha de vencimiento, motivo de cierre |
| HistorialEstado | id, solicitud, estado anterior, estado nuevo, usuario, fecha, motivo |
| Comentario | id, solicitud, autor, texto, fecha |
| Evidencia | id, solicitud, nombre de archivo, tipo, tamaño, ruta, fecha |
| Notificacion | id, destinatario, solicitud, mensaje, leída, fecha |
| ReglaTriage | id, palabras clave, categoría, prioridad, activa |
| Calificacion | id, solicitud, puntaje (1–5), comentario, fecha |

## 8. API (recursos principales)

| Recurso | Operaciones |
|---------|-------------|
| `/api/v1/auth` | `POST /register`, `POST /login`, `POST /refresh`, `POST /logout`, `PUT /password` |
| `/api/v1/requests` | `POST`, `GET` (filtrado según el rol), `GET /{codigo}` |
| `/api/v1/requests/{codigo}/transitions` | `POST` (estado destino, motivo, técnico) |
| `/api/v1/requests/{codigo}/comments` | `GET`, `POST` |
| `/api/v1/requests/{codigo}/evidences` | `GET`, `POST`, `GET /{id}` |
| `/api/v1/requests/{codigo}/rating` | `POST` |
| `/api/v1/triage/suggestions` | `POST` (descripción → sugerencia) |
| `/api/v1/dashboard/indicators` | `GET` (rango de fechas, área) |
| `/api/v1/notifications` | `GET`, `PATCH /{id}/read` |
| `/api/v1/admin/*` | CRUD de usuarios, áreas, categorías, prioridades, ubicaciones y reglas; `GET` de estados |

Códigos HTTP: `201` al crear, `400` por validación, `401` sin autenticación, `403` sin permiso, `404` no encontrado, `409` por transición inválida.

## 9. Estrategia de pruebas

- **TDD** en dominio y casos de uso: primero la prueba que falla, luego la implementación y después el refactor.
- **Dominio:** JUnit 5 sin Spring (máquina de estados, SLA, código, triage).
- **Integración:** `@SpringBootTest` con Testcontainers (MySQL real) para repositorios y endpoints.
- **Arquitectura:** ArchUnit para las reglas de dependencia.
- **Frontend:** Vitest + Testing Library con MSW.
- **Aceptación:** los escenarios BDD de las historias de usuario se traducen a pruebas de integración de la API.
- **Fase IV:** pruebas de carga y estrés con k6, DAST con OWASP ZAP y SAST en CI.

## 10. Flujo de trabajo y despliegue

### 10.1 Ramas

- `main`: producción. Solo recibe merges desde `dev` (releases).
- `dev`: integración y staging.
- `feature/<nombre>`: una por feature, sale de `dev` y vuelve por pull request.
- Commits con Conventional Commits.

### 10.2 CI (GitHub Actions)

En cada pull request y push a `dev` o `main`:
- Backend: `mvn verify` (pruebas unitarias, de integración y ArchUnit).
- Frontend: lint, verificación de tipos, pruebas y build.
- SAST y análisis de dependencias.

### 10.3 CD (Dokploy en VPS propia)

- Dos aplicaciones Docker Compose en Dokploy: **staging** (rama `dev`) y **producción** (rama `main`), con despliegue automático por push.
- Servicios por entorno: backend, frontend (build estático servido por Nginx) y MySQL con volumen persistente.
- Variables y secretos configurados en Dokploy, nunca en el repositorio.
- HTTPS gestionado por Dokploy.

## 11. Roadmap

| Etapa | Semanas | Entregable | Contenido |
|-------|---------|------------|-----------|
| Sprint 0: Base | 8 | Repositorio y despliegue funcionando | Monorepo, `main` y `dev`, CI básico, docker-compose, Flyway, primer despliegue en Dokploy (staging) |
| Sprint 1: MVP | 8–9 | MVP de punta a punta | auth, requests (crear, listar, detalle), workflow (estados, asignación, historial), seed de catálogos. Pantallas: login, mis solicitudes, nueva solicitud, bandeja del supervisor y bandeja del técnico |
| Fase I (en paralelo) | 8–9 | Documento de especificación | Problema, justificación, actores, alcance, requerimientos, épicas, historias de usuario con escenarios BDD, arquitectura, wireframes y prototipo (frontend contra MSW) |
| Presentación parcial | 10 | Informe y demo | Fase I más evidencia del MVP |
| Sprint 2: Release Candidate | 10–12 | RC en producción | Comentarios, evidencias, SLA, notificaciones en la aplicación, dashboard, pantallas de admin, triage, satisfacción, entorno de producción, OpenAPI, revisión OWASP, pruebas de integración, SAST en CI |
| Sprint 3: Calidad | 13–14 | Informe de pruebas | Pruebas funcionales y de aceptación, DAST, carga y estrés, observabilidad (paneles y alertas); opcionales: correo y recuperación de contraseña |
| Cierre | 15 | Versión final congelada | Revisión de arquitectura y código, manual de usuario, manual técnico, informe final |
| Sustentación | 16 | Demostración en vivo | Secuencia de 14 pasos del enunciado |

### Recortes si el plazo aprieta (en este orden)
1. Notificaciones por correo.
2. Recuperación de contraseña.
3. Reglas de triage fijas en seed en lugar de gestionarlas desde admin.

## 12. Riesgos

| Riesgo | Mitigación |
|--------|------------|
| Dos semanas para Fase I y MVP | MVP recortado al flujo central; documentación de Fase I en paralelo |
| Problemas de despliegue descubiertos tarde | Desplegar un esqueleto en Dokploy en el Sprint 0 |
| Recursos limitados de la VPS para staging, producción y observabilidad | Medir consumo en el Sprint 0; ajustar la decisión D3 |
| Incorporación del grupo a mitad del proyecto | Arquitectura por feature, ramas por feature, documentación en `docs/` |

## 13. Decisiones abiertas

- **D1 — Repositorio remoto:** cuenta de GitHub, visibilidad del repositorio y credenciales para hacer push con la identidad UNMSM.
- **D2 — Dominios:** subdominios de staging y producción en la VPS.
- **D3 — Observabilidad:** Prometheus + Grafana en la misma VPS o un servicio externo, según los recursos medidos en el Sprint 0.
- **D4 — Fechas exactas:** confirmar las fechas de la semana 10 y la semana 16.
