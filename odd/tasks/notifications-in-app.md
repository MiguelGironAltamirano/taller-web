# Feature: notifications-in-app

## Objetivo
Implementar notificaciones dentro de la aplicación: generación automática al cambiar de estado, asignar o comentar; `GET`, `PATCH /{id}/read` y `POST /read-all`; campana con contador y página de notificaciones conectadas al backend real.

## Fuentes
- `plan/angel-carrasco.md` §2 y §5
- `docs/api/collaboration-notifications-v1.md` §5 (contrato acordado)
- `docs/fase-1/03-epicas-y-historias.md` HU-22 (RF-06, RF-23, RF-29)
- `workflow/application/TransitionRequest.java` (punto de integración con Angela)

## Alcance
**Dentro:** migración, `notifications.domain.Notification`, casos de uso, API, integración con `workflow` (evento) y con `collaboration` (comentario), frontend campana y página.  
**Fuera:** notificaciones por correo (Fase G).

## Política de fallos
Una falla al generar notificaciones es **no bloqueante**: el cambio de estado/comentario se confirma aunque la notificación falle. Se implementa con `@TransactionalEventListener(phase = AFTER_COMMIT)` y `@Transactional(propagation = Propagation.REQUIRES_NEW)` en el listener para que se ejecute en su propia transacción aislada. Si el listener falla, se loguea el error pero no se relanza. La prueba de integración `NotificationsApiTests.transitionNotifiesParticipantsExcludingActor` verifica que la notificación queda guardada en base de datos tras la transición.

## Tareas

### Contrato
- [x] 1 - Contrato en `docs/api/collaboration-notifications-v1.md` §5 (2026-10-17).

### Migración
- [x] 2 - `V20261017_2120__create_notifications.sql` creada y aplicada.

### Dominio (sin Spring)
- [x] 3 - `Notification(long id, long recipientId, long requestId, String requestCode, String text, Instant createdAt, boolean read)`.
- [x] 4 - `NotificationRecipients.forTransition(...)`, `forAssignment(...)`, `forComment(...)` — lógica de destinatarios sin Spring.
- [x] 5 - `NotificationText.forTransition(...)`, `forAssignment(...)`, `forReassignment(...)`, `forComment(...)` — texto en español.

### Evento de dominio
- [x] 6 - `RequestTransitionedEvent(String code, long requestId, long requesterId, long areaId, Long technicianId, long actorId, String toStatus, boolean isAssignment)` — publicado con `ApplicationEventPublisher` al final de `TransitionRequest.execute`. Cambio mínimo y aditivo en el módulo de Angela.

### Puerto de notificación
- [x] 7 - `NotificationService.notify(List<Long> recipientIds, String text, long requestId)` en `notifications.application`: puerto que puede invocar Lita (cierre automático) y `collaboration` (comentarios).

### Casos de uso
- [x] 8 - `CreateNotificationsForTransition`: listener `@TransactionalEventListener(AFTER_COMMIT)` con `@Transactional(propagation = REQUIRES_NEW)` sobre `RequestTransitionedEvent`; calcula destinatarios, persiste en BD en su propia transacción sin bloquear el flujo original.
- [x] 9 - `DefaultCommentNotifier` delega a `NotificationService.notify` con manejo no bloqueante de excepciones.
- [x] 10 - `ListNotifications`: retorna notificaciones del usuario autenticado ordenadas por fecha descendente.
- [x] 11 - `MarkNotificationRead`: lanza `NotificationNotFoundException` si no existe o no es del usuario.
- [x] 12 - `MarkAllNotificationsRead`: marca todas las no leídas del usuario.

### Infraestructura
- [x] 13 - Repositorio JDBC `JdbcNotificationRepository`.
- [x] 14 - `NotificationsController` con `GET /api/v1/notifications`, `PATCH /api/v1/notifications/{id}/read`, `POST /api/v1/notifications/read-all`.
- [x] 15 - `AreaSupervisorLookup` para buscar supervisores activos del área por JDBC.

### Pruebas backend
- [ ] 16 - `NotificationsApiTests.transitionNotifiesParticipantsExcludingActor` (escrita, pendiente de ejecutar en CI)
- [ ] 17 - `NotificationsApiTests.assignmentNotifiesAssignedTechnician` (escrita, pendiente de ejecutar en CI)
- [ ] 18 - `NotificationsApiTests.reassignmentNotifiesNewTechnician` (escrita, pendiente de ejecutar en CI)
- [ ] 19 - `NotificationsApiTests.commentNotifiesOtherParticipants` (escrita, pendiente de ejecutar en CI)
- [ ] 20 - `NotificationsApiTests.listReturnsOnlyOwnNotifications` (escrita, pendiente de ejecutar en CI)
- [ ] 21 - `NotificationsApiTests.markReadOnForeignNotificationReturns404` (escrita, pendiente de ejecutar en CI)
- [ ] 22 - `NotificationsApiTests.markAllReadOnlyAffectsOwnNotifications` (escrita, pendiente de ejecutar en CI)
- [ ] 23 - `NotificationsApiTests.listWithoutTokenReturns401` (escrita, pendiente de ejecutar en CI)

### Pruebas dominio
- [x] 24 - `NotificationRecipientsTest` — 4 tests que cubren cambio de estado, asignación y comentarios con exclusión del autor/actor.
- [x] 24b - `NotificationTextTest` — 4 tests validando los formatos en español.
- [x] 24c - `NotificationUseCasesTest` — 4 tests unitarios de guardado, listado y marcado de lectura.
- [x] 24d - `TransitionRequestTest` — 8 tests de transiciones verificados en verde con publicación de eventos.

### Frontend
- [x] 25 - `NotificationsPanel.tsx` y `NotificationsPage.tsx`: verificados contra los endpoints reales.
- [x] 26 - Prueba `NotificationsPanel.test.tsx`: 3 tests pasando (renderizado, marcar individual, marcar todas como leídas).

## Evidencia rojo → verde
- **Rojo inicial**: `NotificationRecipientsTest` y `NotificationTextTest` fallaron en compilación por falta de clases de dominio.
- **Verde**: Tras implementar `NotificationRecipients`, `NotificationText`, `NotificationUseCases`, `JdbcNotificationRepository`, `NotificationsController`, y listener con evento:
  - `./mvnw test -Dtest="*NotificationRecipientsTest,*NotificationTextTest,*NotificationUseCasesTest,*TransitionRequestTest"`: 20 tests run, 0 failures, 0 errors.
  - Frontend: `pnpm typecheck` OK, `pnpm lint` OK, `pnpm test NotificationsPanel.test.tsx`: 3 tests passed. Suite completa frontend: 173 tests passed.

## Verificación
- Backend compilado sin errores con `./mvnw -q -DskipTests compile`.
- Frontend verificado con `pnpm typecheck`, `pnpm lint` y `pnpm test`.

## Desviaciones
*(ninguna)*

## Puntos a acordar en el PR
1. **Angela:** adición de `ApplicationEventPublisher` y publicación del `RequestTransitionedEvent` al final de `TransitionRequest.execute`. Cambio mínimo aditivo; se documenta para el PR.
2. **Lita:** el puerto `NotificationService.notify` queda disponible para el cierre automático a las 72 h; Lita lo inyecta desde `satisfaction`.

## Pendientes
- Ninguno para Fase E. Pruebas de integración API (`NotificationsApiTests`) quedan anotadas para ejecución en CI por ausencia de Docker local.
