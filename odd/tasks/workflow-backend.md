# Feature: workflow-backend

## Objective
Implementar el backend del módulo `workflow` del plan de Angela (máquina de estados, transiciones y técnicos) y conectarlo con el frontend.

## Problem / Why
El detalle devolvía siempre `allowedTransitions: []` y no había endpoint para cambiar el estado de una solicitud, por lo que ningún rol podía avanzar el flujo.

## Sources
- Spec: `docs/specs/2026-10-14-servicios-universitarios-design.md` §6
- Plan: `plan/angela.md` §2, §3 y §5
- Contrato: `docs/api/contrato-v1.md` y `docs/api/ui-extensions.md`

## Scope
In: `POST /api/v1/requests/{codigo}/transitions`, `GET /api/v1/users/technicians`, transiciones permitidas en el detalle, ajuste del frontend y de los mocks.
Out: comentarios y evidencias (AngelCarrasco), calificación (Lita).

## Tasks
- [x] Dominio: `StateMachine` pura, comandos y excepciones (409 → 403 → 400)
- [x] Aplicación: `TransitionRequest`, `ListTechnicians`, `WorkflowAllowedTransitions`, puertos hacia `requests` y `auth`
- [x] Infraestructura: controladores, DTOs, manejador RFC 7807, bloqueo pesimista y `@Version`
- [x] Pruebas unitarias (dominio y casos de uso) y de integración con MySQL real
- [x] Frontend: acciones derivadas de `allowedTransitions`, manejo del 409, confirmar y calificar en dos pasos
- [x] Corrección de `WorkflowApiTests`: sin ids de usuario fijos (`users.id` es AUTO_INCREMENT)
- [ ] Pruebas de interfaz de workflow y satisfacción
- [ ] Verificación manual de punta a punta con el backend real
- [x] Cierre automático a las 72 h (FL6, rama `feature/fl-autoclose`; commits `5c3a9d9`, `4cc8db9`, `db571b6`). `AutoCloseJob` (`@Scheduled`, `app.workflow.auto-close.*`) ejecuta `AutoCloseUnconfirmedRequests`: cierra con `AUTOMATICA` las solicitudes en `RESUELTA` desde hace `autoCloseHours` (configuración del administrador) contadas desde la última entrada a `RESUELTA` del historial; cada una en su propia transacción con bloqueo y nueva comprobación (idempotente). El historial lo registra el sistema (`actor_id = NULL`, migración `V20261026_0400`, mostrado como `Sistema`) y se notifica a solicitante, técnico y supervisores. Evidencia: `StateMachineTest`, `AutoCloseUnconfirmedRequestsTest` (6), `AutoCloseApiTests` (6, MySQL real, incluye `409` al calificar), `AutoCloseJobTests` (2), `AutoCloseJobWiringTests` (2); `./mvnw verify`: 539 pruebas, 0 fallos. También: `SuggestTriage` respeta `triageSuggestions` (commit `0599526`, `SuggestTriageTest` y `TriageApiTests`).
- [x] Documentación de Fase I en `docs/fase-1/` (problema, requerimientos, épicas e historias con BDD, arquitectura, wireframes, evidencia)
- [ ] Revisión de `docs/fase-1/` por el equipo antes de la semana 10
