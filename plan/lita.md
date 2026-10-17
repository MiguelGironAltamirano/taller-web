# Plan de Lita — Experiencia de usuario, frontend compartido y calidad

Plan general: [`README.md`](README.md).

## 1. Objetivo del módulo

Dar al equipo una interfaz consistente, responsive y accesible mediante componentes compartidos; implementar la encuesta de satisfacción; y comprobar la calidad del sistema con pruebas funcionales, de aceptación y de carga, consolidando los manuales.

Este módulo se construye sobre la autenticación de Miguel (token, roles y `GET /api/v1/me`) y sobre el contrato de la API de la Fase A en `docs/api/`.

## 2. Alcance

**Backend** (paquete `satisfaction`)
- Calificación de 1 a 5 con comentario opcional, registrada al confirmar una solicitud resuelta.
- Cierre automático a las 72 horas sin calificación si el solicitante no responde.

**Frontend**
- `shared/ui`: botones, campos de formulario, tablas, estados vacío, de carga y de error, y notificaciones emergentes (toasts), siguiendo atomic design. Los demás integrantes los consumen.
- Diseño responsive consistente y accesibilidad (WCAG AA): etiquetas asociadas, foco visible, contraste suficiente y navegación por teclado.
- `features/satisfaction`: pantalla para confirmar y calificar, o rechazar la solución.

**Calidad y documentación**
- Estrategia e informe de pruebas; pruebas funcionales y de aceptación a partir de los escenarios BDD de `docs/fase-1/`.
- Pruebas de carga y estrés con k6 (`infra/`).
- Consolidación del manual de usuario y del manual técnico, e informe final.

La seguridad operativa (SAST, DAST) y la observabilidad están a cargo de Miguel; Lita incorpora sus resultados al informe de pruebas.

## 3. Endpoints y pantallas

| Endpoint | Acceso | Notas |
|---|---|---|
| `POST /api/v1/requests/{codigo}/rating` | Solicitante dueño | Puntaje de 1 a 5 y comentario; la solicitud debe estar en "Resuelta"; la solicitud pasa a "Cerrada" |
| Tarea programada de cierre automático | Sistema | Cierra a las 72 h las solicitudes resueltas sin respuesta; detalle por definir en `docs/api/` antes de implementar |
| Consulta de calificaciones para el dashboard | Interno | Por definir en `docs/api/` antes de implementar |

Pantallas: confirmación y calificación (1 a 5) al resolver, con opción de rechazar la solución con motivo; estados vacío, de carga y de error y toasts reutilizables en todas las pantallas.

## 4. Entregables por semana

| Semanas | Entregable |
|---|---|
| 8–10 | Componentes base de `shared/ui` (botones, campos, estados vacío/carga/error, toasts); guía de accesibilidad y de diseño responsive; plan de pruebas |
| 10 | **Presentación parcial** (todos) |
| 10–12 | Encuesta de satisfacción (backend y pantalla); cierre automático a las 72 h; tablas compartidas y adopción de los componentes en las pantallas de los demás |
| 13–14 | Revisión de accesibilidad; pruebas funcionales y de aceptación; pruebas de carga con k6; informe de pruebas |
| 15 | Consolidación del manual de usuario y del manual técnico; informe final |
| 16 | **Sustentación final** (todos) |

## 5. Pruebas obligatorias

- Calificar con puntaje 0 o 6 → `400`; con 1 a 5 → `201` y la solicitud pasa a "Cerrada" con la calificación guardada.
- Calificar una solicitud que no está en "Resuelta" → `409`; calificar una solicitud ajena → `404`; calificar dos veces la misma solicitud → rechazado.
- Cierre automático: una solicitud resuelta hace más de 72 horas se cierra sin calificación; una resuelta hace menos de 72 horas no cambia; una ya cerrada no se vuelve a procesar.
- Rechazar la solución devuelve la solicitud a "En atención" y exige motivo.
- Componentes de `shared/ui`: cada campo asocia su etiqueta, muestra su mensaje de error y recibe foco con el teclado; los botones deshabilitados no disparan acciones; el estado de error ofrece reintentar.
- Accesibilidad: sin violaciones de nivel AA detectables automáticamente en las pantallas principales; todo flujo clave se completa solo con teclado.
- Aceptación: cada escenario BDD de `docs/fase-1/` tiene una prueba que lo ejecuta o queda registrado como no cubierto en el informe.
- Carga (k6): umbrales definidos en el plan de pruebas para el listado de solicitudes y el login, con resultados en el informe.

## 6. Dependencias con otros integrantes

| Qué | Quién |
|---|---|
| Recibe: token, roles, `/api/v1/me`, layout | Miguel |
| Recibe: informes SAST y DAST y datos de observabilidad para el informe de pruebas | Miguel |
| Recibe: el cierre de la solicitud y la transición Resuelta → Cerrada | Angela |
| Recibe: solicitudes y su detalle | Sevan |
| Recibe: escenarios BDD de `docs/fase-1/` | Angela |
| Provee: componentes de `shared/ui`, toasts y guía de accesibilidad | Todos |
| Provee: calificaciones para los indicadores | AngelCruz |
| Provee: informe de pruebas y manuales consolidados | Todos |

## 7. Archivos y paquetes que le pertenecen

- Backend: `satisfaction` y su migración.
- Frontend: `frontend/src/shared/ui/`, `frontend/src/features/satisfaction/`.
- Pruebas de carga: scripts k6 en `infra/`.
- Documentación: estrategia e informe de pruebas, manual de usuario, manual técnico consolidado, informe final.

## 8. Definición de terminado

Ver la [definición de terminado general](README.md#5-definición-de-terminado). Además: los componentes de `shared/ui` tienen pruebas con Testing Library y cumplen los criterios de accesibilidad definidos en la guía.
