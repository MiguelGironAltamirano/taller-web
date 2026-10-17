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
Out: cierre automático a las 72 h, comentarios y evidencias (AngelCarrasco), calificación (Lita).

## Tasks
- [x] Dominio: `StateMachine` pura, comandos y excepciones (409 → 403 → 400)
- [x] Aplicación: `TransitionRequest`, `ListTechnicians`, `WorkflowAllowedTransitions`, puertos hacia `requests` y `auth`
- [x] Infraestructura: controladores, DTOs, manejador RFC 7807, bloqueo pesimista y `@Version`
- [x] Pruebas unitarias (dominio y casos de uso) y de integración con MySQL real
- [x] Frontend: acciones derivadas de `allowedTransitions`, manejo del 409, confirmar y calificar en dos pasos
- [x] Corrección de `WorkflowApiTests`: sin ids de usuario fijos (`users.id` es AUTO_INCREMENT)
- [ ] Pruebas de interfaz de workflow y satisfacción
- [ ] Verificación manual de punta a punta con el backend real
- [ ] Cierre automático a las 72 h
- [x] Documentación de Fase I en `docs/fase-1/` (problema, requerimientos, épicas e historias con BDD, arquitectura, wireframes, evidencia)
- [ ] Revisión de `docs/fase-1/` por el equipo antes de la semana 10
