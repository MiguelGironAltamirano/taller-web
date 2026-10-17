# Plan de Sevan — Catálogos y gestión de solicitudes

Plan general: [`README.md`](README.md).

## 1. Objetivo del módulo

Permitir que un solicitante registre una solicitud con código único, categoría, ubicación, descripción y prioridad, y que cada rol consulte las solicitudes que le corresponden, con la marca de vencidas según el SLA.

Este módulo se construye sobre la autenticación de Miguel (token, roles y `GET /api/v1/me`) y sobre el contrato de la API de la Fase A en `docs/api/`.

## 2. Alcance

**Backend** (paquetes `catalog` y `requests`)
- Catálogos de consulta: áreas, categorías, prioridades y ubicaciones (Facultad → Pabellón → Ambiente).
- Registro de solicitudes con código `SOL-AAAA-NNNNNN` correlativo por año, único e inmutable.
- Área derivada de la categoría; vencimiento = fecha de registro + horas de SLA de la prioridad.
- Consulta según rol: solicitante (las propias), supervisor (su área), técnico (las asignadas), administrador (todas).
- Marca de vencida: no llegó a "Resuelta" antes de la fecha de vencimiento.

**Frontend** (`features/requests`)
- "Mis solicitudes", "Nueva solicitud" y detalle de la solicitud con su historial.

**Documentación**
- Contrato de catálogos y solicitudes en `docs/api/`; notas del manual de usuario sobre el registro de solicitudes.

## 3. Endpoints y pantallas

| Endpoint | Acceso | Notas |
|---|---|---|
| `GET /api/v1/catalogs` | Token | Áreas, categorías, prioridades y ubicaciones |
| `POST /api/v1/requests` | Solicitante | `201` con el detalle; `400` por validación |
| `GET /api/v1/requests` | Token | Filtrado según el rol; filtro por estado y paginación |
| `GET /api/v1/requests/{codigo}` | Token con visibilidad | `404` si no existe o no es visible para el usuario |
| Filtros avanzados del listado (fechas, área, vencidas) | Token | Por definir en `docs/api/` antes de implementar |

Pantallas: "Mis solicitudes" (lista filtrada por estado), "Nueva solicitud" (formulario con catálogos y validación de 20 a 2000 caracteres) y detalle de la solicitud. La sugerencia de triage en el formulario usa `POST /api/v1/triage/suggestions` de AngelCruz.

## 4. Entregables por semana

| Semanas | Entregable |
|---|---|
| 8–10 | Catálogos; registro de solicitudes con código y SLA; listado y detalle según rol; pantallas "Mis solicitudes", "Nueva solicitud" y detalle |
| 10 | **Presentación parcial** (todos) |
| 10–12 | Marca de vencidas; filtros avanzados; integración con la sugerencia de triage |
| 13–14 | Pruebas de su módulo y correcciones derivadas de las pruebas de aceptación |
| 15 | Manual técnico de su módulo |
| 16 | **Sustentación final** (todos) |

## 5. Pruebas obligatorias

- Descripción de 19 caracteres → `400`; de 20 a 2000 caracteres → aceptada; de 2001 → `400`.
- Solicitud válida → `201`, código `SOL-AAAA-000001` y vencimiento igual al registro más las horas de SLA de la prioridad (Crítica 8 h, Alta 24 h, Media 72 h, Baja 120 h).
- Diez creaciones concurrentes → diez códigos distintos y consecutivos.
- El área de la solicitud es siempre el área por defecto de la categoría elegida.
- Un solicitante que pide por código una solicitud ajena → `404`.
- El listado devuelve solo lo visible para cada rol (solicitante, supervisor del área, técnico asignado, administrador).
- Una solicitud sin resolver pasada su fecha de vencimiento se marca vencida; una resuelta a tiempo, no.
- `GET /catalogs` sin token → `401`; con token → cuatro áreas, siete categorías, cuatro prioridades y ocho ubicaciones.
- Frontend: crear una solicitud muestra el código generado y aparece en la lista; el formulario no se envía con la descripción fuera de rango.

## 6. Dependencias con otros integrantes

| Qué | Quién |
|---|---|
| Recibe: token, roles, `/api/v1/me`, esquema base | Miguel |
| Recibe: transiciones de estado e historial para el detalle | Angela |
| Recibe: sugerencia de categoría y prioridad | AngelCruz |
| Recibe: campos, botones, tablas y estados de carga/vacío/error | Lita |
| Provee: solicitudes, catálogos y visibilidad por rol | Angela, AngelCarrasco, AngelCruz, Lita |

## 7. Archivos y paquetes que le pertenecen

- Backend: `catalog`, `requests` (capas `domain`, `application`, `infrastructure`) y sus migraciones.
- Frontend: `frontend/src/features/requests/`.
- Documentación: contrato de catálogos y solicitudes en `docs/api/`.

## 8. Definición de terminado

Ver la [definición de terminado general](README.md#5-definición-de-terminado).

## 9. Funcionalidades del diseño no previstas en el planning

El diseño final (Claude Design, proyecto "Servicios Universitarios UNMSM") trae pantallas y comportamientos que el spec y los planes individuales no mencionan. Ya están construidos en el frontend con datos simulados (MSW) en la rama `feature/ui-design-system`; este plan los adopta para que no queden huérfanos. Cada uno necesita, antes de pasar a producción, su contrato en `docs/api/` (ver [`docs/api/ui-extensions.md`](../docs/api/ui-extensions.md)) y su backend.

Prioridad sugerida: **S** = Release Candidate (semana 12), **C** = opcional si hay tiempo, **D** = solo interfaz (sin backend).

| # | Funcionalidad | Dónde está en el front | Falta | Prioridad |
|---|---|---|---|---|
| 1 | Página de inicio del solicitante (resumen, "requieren tu atención", actividad reciente) | `features/requests` (`HomePage`) | Sin backend propio: se arma con `GET /requests` y `GET /notifications` | S |
| 2 | Calendario de vencimientos del técnico | `features/requests` (`CalendarPage`) | Sin backend propio: usa `GET /requests` | C |
| 3 | Vista kanban con arrastre de tarjetas (solo transiciones válidas) | `features/workflow` (`KanbanBoard`) | Sin backend propio: reutiliza `/transitions` | C |
| 4 | Vistas guardadas, orden, filtros en la URL, densidad y exportación CSV del listado | `features/requests` (`RequestListPage`) | Filtros del servidor (fechas, área, vencidas) ya previstos en la sección 3; el CSV se genera en el cliente | S |
| 5 | Equipo: carga por técnico del área | `features/dashboard` (`TeamPage`) | `GET /dashboard/team` | C |
| 6 | Reportes descargables (por estado, SLA, carga, vencidas) | `features/dashboard` (`ReportsPage`) | CSV en el cliente; exportación a PDF sin implementar | C |
| 7 | Auditoría: registro de cambios sobre solicitudes | `features/admin` (`AuditPage`) | `GET /admin/audit` (se deriva del historial) | C |
| 8 | Configuración: horas de cierre automático, correo y sugerencia de triage | `features/admin` (`ConfigPage`) | `GET/PUT /admin/config` y que el backend la respete | S |
| 9 | Dashboard ampliado: variación vs. periodo anterior, mini-gráficos, medidor de SLA, "por vencer en 24 h" | `features/dashboard` (`DashboardPage`) | `GET /dashboard/indicators` (ver la extensión del contrato) | S |
| 10 | Paleta de comandos (Ctrl+K), menú lateral colapsable, modo oscuro y navegación móvil inferior | `app/layout` | Ninguno: solo interfaz | D |
| 11 | Comentarios en la solicitud (con aviso a la otra parte) | `features/collaboration` | Ya previsto para AngelCarrasco; el front lo consume | S |
| 12 | "Recuperar contraseña" en el login | `features/auth` | Hoy solo muestra un aviso; depende del correo (opcional del spec) | C |

### Diferencias entre el diseño y el spec que hay que decidir

- **Calificación al cerrar:** el diseño exige 1–5 estrellas para "Confirmar y cerrar"; el spec (§6) dice "calificación opcional" y que el cierre automático a las 72 h es sin calificación. El front sigue el diseño; si se prefiere el spec, basta quitar la validación en `ResolvedCard`.
- **Triage por mayor número de coincidencias:** el spec (§5.1) pide la regla con más coincidencias; el prototipo del diseño tomaba la primera que coincidía. El front y los mocks siguen el spec.
- **Registro de solicitudes:** el contrato del MVP envía el `locationId` del ambiente; el formulario del diseño elige Facultad → Pabellón → Ambiente y el front resuelve ese id.
- **Cierre automático a las 72 h:** el front solo lo informa; hay que implementarlo en el backend (satisfacción, plan de Lita).

## 10. Estado de avance (17 de octubre de 2026)

Resumen: **el frontend de tu módulo está hecho contra la API simulada; el backend (`catalog` y `requests`) todavía no existe.** El PR #7 (`feature/ui-design-system`) ya está en `dev`, con los checks de CI en verde y sin revisión de otro integrante.

### Backend (`catalog`, `requests`)

- [x] Esquema y semillas de catálogos en `dev` (`V2__initial_schema.sql`, `V3__catalog_seed.sql`, de Miguel).
- [ ] `GET /api/v1/catalogs`
- [ ] `POST /api/v1/requests` con código `SOL-AAAA-NNNNNN` correlativo por año, validación de 20–2000 caracteres, área derivada de la categoría y vencimiento = registro + SLA
- [ ] `GET /api/v1/requests` filtrado por rol y por estado, con paginación
- [ ] `GET /api/v1/requests/{codigo}` con `404` si no es visible
- [ ] Marca de vencidas calculada en el servidor
- [ ] Filtros avanzados del listado (fechas, área, vencidas): definirlos antes en `docs/api/`
- [ ] Pruebas de integración con MySQL real de la lista de la sección 5 (la prueba de diez creaciones concurrentes incluida)

Los escenarios de la sección 5 ya están cubiertos **contra los mocks** (`frontend/src/mocks/handlers.test.ts`); sirven de guía, pero no sustituyen las pruebas del backend.

### Frontend (`features/requests`)

- [x] "Mis solicitudes": lista por rol con contadores por estado, búsqueda, filtros, vistas guardadas (incluye "vencidas"), orden, paginación y exportación CSV
- [x] "Nueva solicitud": validación en vivo de 20–2000 caracteres, ubicación Facultad → Pabellón → Ambiente, prioridad con su SLA, evidencias (hasta 5, 5 MB, JPG/PNG/PDF) y resumen lateral
- [x] Detalle de la solicitud con historial, SLA, comentarios y evidencias
- [x] Integración con la sugerencia de triage (aceptar o ignorar)
- [x] Marca de "Vencida" en lista, kanban, detalle y calendario
- [x] Prueba obligatoria: crear una solicitud muestra el código y aparece en la lista; el formulario no se envía con la descripción fuera de rango (`frontend/src/features/requests/new-request.test.tsx`)
- [ ] Filtros avanzados por **rango de fechas y área**: hoy se filtra en el cliente por estado, categoría, prioridad, técnico, texto y "vencidas"
- [ ] Conectar con el backend real cuando existan los endpoints (apagar los mocks y revisar las diferencias de datos; ver la nota siguiente)

### Documentación

- [x] Contrato de catálogos y solicitudes: `docs/api/contrato-v1.md` y `docs/api/ui-extensions.md`
- [ ] Notas del manual de usuario sobre el registro de solicitudes
- [ ] Manual técnico del módulo (semana 15)

### Avisos

- **Los datos de los mocks no son los de la semilla real:** los mocks usan los datos del diseño (3 áreas, 8 categorías, 6 facultades) y la semilla del backend define 4 áreas, 7 categorías y 8 ubicaciones. Al conectar con el backend la interfaz funcionará igual, pero las listas serán otras.
- **Pruebas de aceptación y revisión de otro integrante:** pendientes (semanas 13–14).
