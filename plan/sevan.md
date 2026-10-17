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
