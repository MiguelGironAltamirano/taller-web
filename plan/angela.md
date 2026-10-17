# Plan de Angela — Flujo de atención y documentación de análisis

Plan general: [`README.md`](README.md).

## 1. Objetivo del módulo

Implementar el ciclo de vida de las solicitudes (máquina de estados con permisos por rol, asignación e historial) y las bandejas de supervisor y técnico; además, documentar el análisis del proyecto (Fase I).

Este módulo se construye sobre la autenticación de Miguel (token, roles y `GET /api/v1/me`) y sobre el contrato de la API de la Fase A en `docs/api/`.

## 2. Alcance

**Backend** (paquete `workflow`)
- Máquina de estados como objeto de dominio puro, con las transiciones de la sección 6 del spec.
- Asignación y reasignación a técnicos del área (la reasignación exige motivo).
- Historial de cada cambio: quién, cuándo, estado anterior y nuevo, motivo.
- `409 Conflict` ante cualquier transición no listada; `403` si el rol no corresponde.
- Cálculo de las transiciones permitidas para cada solicitud según el usuario.

**Frontend** (`features/workflow`)
- Bandeja del supervisor: evaluar, asignar, reasignar y cerrar con motivo.
- Bandeja del técnico: atender y resolver con comentario de solución.
- Acciones del solicitante sobre una solicitud resuelta: confirmar o rechazar la solución (la calificación la aporta Lita).

**Documentación** (`docs/fase-1/`)
- Problema y justificación, actores y alcance, requerimientos, épicas e historias de usuario con escenarios BDD, arquitectura y wireframes.

## 3. Endpoints y pantallas

| Endpoint | Acceso | Notas |
|---|---|---|
| `POST /api/v1/requests/{codigo}/transitions` | Según la tabla de transiciones | Cuerpo: estado destino, motivo y técnico; `200`, `400` falta dato, `403` rol no autorizado, `409` transición inválida |
| `GET /api/v1/users/technicians?areaId=` | Supervisor | Técnicos activos del área |

Reglas de la tabla de transiciones (spec 6):

| Desde | Hacia | Quién | Condición |
|---|---|---|---|
| Registrada | En evaluación | Supervisor del área | — |
| En evaluación | Asignada | Supervisor del área | Requiere técnico del área |
| Asignada | Asignada | Supervisor del área | Reasignación; requiere motivo |
| Asignada | En atención | Técnico asignado | — |
| En atención | Resuelta | Técnico asignado | Requiere comentario de solución |
| Resuelta | Cerrada | Solicitante, o el sistema a las 72 h | Calificación opcional |
| Resuelta | En atención | Solicitante | Requiere motivo |
| Registrada o En evaluación | Cerrada | Supervisor del área | Motivo: "No procede" o "Duplicada" |

Pantallas: bandeja del supervisor, bandeja del técnico y los botones de transición dentro del detalle de la solicitud (según las transiciones permitidas).

## 4. Entregables por semana

| Semanas | Entregable |
|---|---|
| 8–10 | Máquina de estados, asignación e historial; bandejas de supervisor y técnico; documentación de análisis en `docs/fase-1/` |
| 10 | **Presentación parcial** (todos) |
| 10–12 | Reasignación; rechazo de solución por el solicitante; mejoras de las bandejas |
| 13–14 | Pruebas de su módulo y correcciones derivadas de las pruebas de aceptación |
| 15 | Manual técnico de su módulo |
| 16 | **Sustentación final** (todos) |

## 5. Pruebas obligatorias

- Pruebas unitarias del dominio: cada fila de la tabla de transiciones es válida y al menos cinco transiciones inválidas (por ejemplo Registrada → Resuelta) lanzan conflicto.
- Transición fuera de la tabla → `409` y la solicitud no cambia en la base de datos.
- Un técnico no asignado que intenta pasar a "En atención" → `403`.
- Asignar sin técnico, o con un técnico de otra área → `400`; reasignar sin motivo → `400`.
- Resolver sin comentario de solución → `400`.
- Cerrar como "No procede" o "Duplicada" solo desde Registrada o En evaluación y solo por el supervisor del área.
- Flujo completo (registrar, evaluar, asignar, atender, resolver, cerrar) deja seis entradas de historial con usuario, fecha y estados.
- El rechazo de la solución devuelve la solicitud a "En atención" y exige motivo.
- Frontend: el detalle solo muestra los botones de las transiciones permitidas; asignar exige elegir técnico; cerrar como supervisor exige motivo; un `409` muestra "Transición no permitida" y refresca el detalle.

## 6. Dependencias con otros integrantes

| Qué | Quién |
|---|---|
| Recibe: token, roles, `AuthenticatedUser` | Miguel |
| Recibe: solicitudes, catálogos y visibilidad por rol | Sevan |
| Recibe: componentes de `shared/ui` para bandejas y formularios | Lita |
| Provee: transiciones, historial y estados a las notificaciones | AngelCarrasco |
| Provee: estados y fechas para los indicadores | AngelCruz |
| Provee: el cierre de la solicitud usado por la encuesta y el cierre automático | Lita |

## 7. Archivos y paquetes que le pertenecen

- Backend: `workflow` (capas `domain`, `application`, `infrastructure`) y sus migraciones.
- Frontend: `frontend/src/features/workflow/`.
- Documentación: `docs/fase-1/`, contrato de transiciones en `docs/api/`.

## 8. Definición de terminado

Ver la [definición de terminado general](README.md#5-definición-de-terminado). Además: cada historia de usuario tiene al menos un escenario BDD que coincide con una prueba automatizada.

## 9. Estado de avance (17 de octubre de 2026)

Resumen: **el backend de `workflow` y su conexión con el frontend están hechos y en `dev` (PR #9)**; faltan las pruebas de interfaz y la verificación manual con el backend real. La documentación de Fase I está redactada en `docs/fase-1/` y pendiente de revisión del equipo.

### Backend (`workflow`)

- [x] Máquina de estados como dominio puro con la tabla de la sección 3 (orden de validación `409` → `403` → `400`)
- [x] `POST /api/v1/requests/{codigo}/transitions`: bloqueo pesimista de la fila, versión optimista e historial por transición; devuelve el detalle actualizado
- [x] Asignación y reasignación (reasignar exige motivo y un técnico distinto; el técnico debe ser activo y del área)
- [x] `GET /api/v1/users/technicians?areaId=`: solo supervisor, de su propia área, con `activeRequests`
- [x] Transiciones permitidas por usuario en el detalle (`allowedTransitions`)
- [x] Pruebas: dominio (50), casos de uso (13) y de integración con MySQL real (20, `WorkflowApiTests`)
- [ ] Cierre automático a las 72 h (tarea programada): sin implementar

### Frontend (`features/workflow`, `features/satisfaction`)

- [x] Los botones del detalle salen de `allowedTransitions` (`actionsFromTransitions`)
- [x] Un `409` muestra "Transición no permitida" y refresca el detalle
- [x] Confirmar la solución cierra la solicitud; la calificación es opcional y se guarda después; rechazar exige motivo
- [x] Mocks alineados con el backend real
- [ ] Pruebas de interfaz de los flujos de supervisor, técnico y solicitante
- [ ] Verificación manual de punta a punta con el backend real en el navegador

### Documentación

- [x] Semántica de las transiciones en `docs/api/ui-extensions.md`
- [x] Fase I en `docs/fase-1/`: problema y alcance, requerimientos, épicas e historias con escenarios BDD (HU-01 a HU-27), arquitectura, wireframes y prototipo, y evidencia
- [ ] Revisión de las historias por el equipo y escribir las pruebas marcadas "Por escribir" en `docs/fase-1/03-epicas-y-historias.md` (las de prioridad S se escriben junto con su backend)
