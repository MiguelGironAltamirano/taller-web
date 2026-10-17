# Plan de AngelCruz — Administración, triage e indicadores

Plan general: [`README.md`](README.md).

## 1. Objetivo del módulo

Dar al administrador las pantallas para gestionar usuarios y catálogos, sugerir categoría y prioridad con reglas de triage y ofrecer el dashboard con los indicadores del enunciado según el rol.

Este módulo se construye sobre la autenticación de Miguel (token, roles y `GET /api/v1/me`) y sobre el contrato de la API de la Fase A en `docs/api/`.

## 2. Alcance

**Backend** (paquetes `admin`, `triage` y `dashboard`)
- CRUD de usuarios (las cuentas de técnico, supervisor y administrador solo las crea un administrador), áreas, categorías, prioridades (con horas de SLA) y ubicaciones.
- Consulta de estados en solo lectura.
- Reglas de triage: palabras clave que relacionan una categoría y una prioridad; la regla con más coincidencias define la sugerencia, y sin coincidencias no hay sugerencia.
- Indicadores del dashboard con filtros por rango de fechas y área, y vista según el rol.

**Frontend** (`features/admin`, `features/dashboard`)
- Pantallas de administración, gestión de reglas de triage y dashboard con gráficos (Recharts).

**Documentación**
- Contratos de administración, triage e indicadores en `docs/api/`; notas del manual de usuario.

## 3. Endpoints y pantallas

| Endpoint | Acceso | Notas |
|---|---|---|
| `/api/v1/admin/*` | Administrador | CRUD de usuarios, áreas, categorías, prioridades, ubicaciones y reglas; `GET` de estados |
| `POST /api/v1/triage/suggestions` | Solicitante | Recibe la descripción y devuelve categoría y prioridad sugeridas, o ninguna |
| `GET /api/v1/dashboard/indicators` | Token, según el rol | Filtros: rango de fechas y área |
| Rutas concretas de cada recurso de `/admin/*` y forma de cada indicador | — | Por definir en `docs/api/` antes de implementar |

Pantallas: administración de usuarios, áreas, categorías, prioridades, ubicaciones y reglas de triage; consulta de estados; dashboard con filtros.

## 4. Entregables por semana

| Semanas | Entregable |
|---|---|
| 8–10 | Backend de administración de catálogos y usuarios; contrato de triage e indicadores |
| 10 | **Presentación parcial** (todos) |
| 10–12 | Pantallas de administración, triage con gestión de reglas y dashboard |
| 13–14 | Pruebas de su módulo y correcciones derivadas de las pruebas de aceptación |
| 15 | Manual técnico de su módulo |
| 16 | **Sustentación final** (todos) |

## 5. Pruebas obligatorias

- Solo el administrador accede a `/admin/*`; cualquier otro rol → `403`.
- Crear un usuario técnico o supervisor exige un área; un correo repetido → `409`.
- Cambiar las horas de SLA de una prioridad afecta a las solicitudes nuevas, no reescribe las ya registradas.
- Triage: con una descripción que coincide con las palabras clave de una regla se sugiere su categoría y prioridad; con coincidencias en varias reglas gana la de más coincidencias; sin coincidencias no hay sugerencia.
- Una regla inactiva no sugiere.
- Dashboard: los indicadores respetan el rango de fechas y el área; un supervisor solo ve su área; un solicitante no accede a indicadores globales.
- Frontend: el formulario de prioridad valida las horas de SLA; el dashboard muestra el estado vacío cuando no hay datos en el rango.

## 6. Dependencias con otros integrantes

| Qué | Quién |
|---|---|
| Recibe: token, roles, esquema base | Miguel |
| Recibe: solicitudes y catálogos existentes | Sevan |
| Recibe: estados e historial para los indicadores | Angela |
| Recibe: calificaciones para los indicadores de satisfacción | Lita |
| Recibe: tablas, formularios, estados vacío/carga/error y toasts | Lita |
| Provee: gestión de catálogos y la sugerencia de triage | Sevan |

## 7. Archivos y paquetes que le pertenecen

- Backend: `admin`, `triage`, `dashboard` y sus migraciones.
- Frontend: `frontend/src/features/admin/`, `frontend/src/features/dashboard/`.
- Documentación: contratos de administración, triage e indicadores en `docs/api/`.

## 8. Definición de terminado

Ver la [definición de terminado general](README.md#5-definición-de-terminado).
