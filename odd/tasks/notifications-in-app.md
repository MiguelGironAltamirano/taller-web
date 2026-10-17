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
Una falla al generar notificaciones es **no bloqueante**: el cambio de estado/comentario se confirma aunque la notificación falle. Se implementa con `@TransactionalEventListener(phase = AFTER_COMMIT)`. Si el listener falla, se loguea el error pero no se relanza.

## Tareas

### Contrato
- [x] 1 - Contrato en `docs/api/collaboration-notifications-v1.md` §5 (2026-10-17).

### Migración
- [ ] 2 - `V20261017_2120__create_notifications.sql`:
  ```sql
  CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_request FOREIGN KEY (request_id) REFERENCES service_requests (id)
  );
  CREATE INDEX ix_notifications_recipient_read ON notifications (recipient_id, read_at);
  ```

### Dominio (sin Spring)
- [ ] 3 - `Notification(long id, long recipientId, long requestId, String requestCode, String text, Instant createdAt, boolean read)`.
- [ ] 4 - `NotificationRecipients.forTransition(TransitionEvent): List<Long>` — lógica de destinatarios; sin Spring.
- [ ] 5 - `NotificationText.forTransition(String statusLabel, String code): String` — texto en español.

### Evento de dominio
- [ ] 6 - `RequestTransitionedEvent(String code, long requestId, long requesterId, long areaId, Long technicianId, long actorId, String toStatus, boolean isAssignment)` — publicado con `ApplicationEventPublisher` al final de `TransitionRequest.execute`. Cambio mínimo y aditivo en el módulo de Angela.

### Puerto de notificación
- [ ] 7 - `NotificationService.notify(List<Long> recipientIds, String text, long requestId)` en `notifications.application`: puerto que puede invocar Lita (cierre automático) y `collaboration` (comentarios).

### Casos de uso
- [ ] 8 - `CreateNotificationsForTransition`: listener `@TransactionalEventListener(AFTER_COMMIT)` sobre `RequestTransitionedEvent`; calcula destinatarios, persiste en BD.
- [ ] 9 - `CreateNotificationsForComment`: invocado desde `CreateComment`; calcula destinatarios excluyendo al autor.
- [ ] 10 - `ListNotifications(AuthenticatedUser): List<Notification>`.
- [ ] 11 - `MarkNotificationRead(AuthenticatedUser, long id)` — lanza `NotificationNotFoundException` si no existe o no es del usuario.
- [ ] 12 - `MarkAllNotificationsRead(AuthenticatedUser)`.

### Infraestructura
- [ ] 13 - Repositorio JDBC `NotificationRepository`.
- [ ] 14 - `NotificationsController` con los tres endpoints.
- [ ] 15 - Adaptadores para leer supervisores del área y el técnico asignado (usando `auth.application.UserRepository`).

### Pruebas backend
- [ ] 16 - `NotificationsApiTests.transitionNotifiesParticipantsExcludingActor`
- [ ] 17 - `NotificationsApiTests.assignmentNotifiesAssignedTechnician`
- [ ] 18 - `NotificationsApiTests.reassignmentNotifiesNewTechnician`
- [ ] 19 - `NotificationsApiTests.commentNotifiesOtherParticipants`
- [ ] 20 - `NotificationsApiTests.listReturnsOnlyOwnNotifications`
- [ ] 21 - `NotificationsApiTests.markReadOnForeignNotificationReturns404`
- [ ] 22 - `NotificationsApiTests.markAllReadOnlyAffectsOwnNotifications`
- [ ] 23 - `NotificationsApiTests.listWithoutTokenReturns401`

### Pruebas dominio
- [ ] 24 - `NotificationRecipientsTest` — cada tipo de evento y sus destinatarios.

### Frontend
- [ ] 25 - `NotificationsPanel.tsx` y `NotificationsPage.tsx`: ya existen; verificar que funcionan contra el backend real.
- [ ] 26 - Prueba `NotificationsPanel.test.tsx`: el contador de sin leer disminuye al marcar una como leída.

## Evidencia rojo → verde
*(se completa al implementar)*

## Verificación
*(se completa al terminar la fase E)*

## Desviaciones
*(ninguna conocida aún)*

## Puntos a acordar en el PR
1. **Angela:** adición de `ApplicationEventPublisher` y publicación del `RequestTransitionedEvent` al final de `TransitionRequest.execute`. Cambio mínimo; si Angela prefiere otra forma (puerto explícito), se acuerda en el PR.
2. **Lita:** el puerto `NotificationService.notify` queda disponible para el cierre automático a las 72 h; Lita lo inyecta desde `satisfaction`.

## Pendientes
- [ ] Implementar (Fase E).
