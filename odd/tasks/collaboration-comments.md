# Feature: collaboration-comments

## Objetivo
Implementar comentarios en las solicitudes: `GET` y `POST /api/v1/requests/{code}/comments` con visibilidad de la solicitud, notificación a la otra parte y frontend conectado al backend real.

## Fuentes
- `plan/angel-carrasco.md` §2 y §5
- `docs/api/collaboration-notifications-v1.md` §3 (contrato acordado)
- `docs/fase-1/03-epicas-y-historias.md` HU-10 (RF-12)
- Código de referencia: `workflow/` (puertos y adaptadores entre módulos), `requests/application/GetRequest.java` (visibilidad con `AccessPolicy`)

## Alcance
**Dentro:** migración `V20261017_2100__create_comments.sql`, `collaboration.domain.Comment`, `collaboration.application.{ListComments, CreateComment, RequestAccess}`, `collaboration.infrastructure.{web, persistence, adaptadores}`, notificación al crear comentario.  
**Fuera:** evidencias (tarea separada), notificaciones por correo (Fase G).

## Dependencias con otros módulos
- `requests` (Sevan): puerto `RequestAccess` que expone `requesterId`, `areaId`, `technicianId`, `code`. El adaptador usa `requests.application` internamente.
- `notifications` (propio): el caso de uso `CreateComment` publica un evento o llama al puerto `NotificationService` para notificar a los participantes.

## Tareas

### Contrato
- [x] 1 - Contrato documentado en `docs/api/collaboration-notifications-v1.md` §3 (2026-10-17).

### Migración
- [ ] 2 - `V20261017_2100__create_comments.sql`:
  ```sql
  CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    text VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comments_request FOREIGN KEY (request_id) REFERENCES service_requests (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
  );
  CREATE INDEX ix_comments_request ON comments (request_id, created_at);
  ```

### Dominio (sin Spring)
- [ ] 3 - `Comment(long id, long requestId, long authorId, String authorName, String text, Instant createdAt)`.
- [ ] 4 - Validación de texto (1–1000 caracteres con trim); excepción `CommentTextInvalidException`.

### Casos de uso y puertos
- [ ] 5 - Puerto `RequestAccess` en `collaboration.application`: `RequestSnapshot findVisibleOrThrow(AuthenticatedUser user, String code)` (lanza `RequestNotFoundException` si no existe o no es visible).
- [ ] 6 - `ListComments`: lista comentarios de una solicitud visible.
- [ ] 7 - `CreateComment`: crea el comentario, emite evento para notificar.

### Infraestructura
- [ ] 8 - Adaptador `RequestAccessAdapter` en `collaboration.infrastructure`: implementa el puerto usando `requests.application`.
- [ ] 9 - Repositorio JDBC `CommentRepository`.
- [ ] 10 - Controlador `CollaborationController` con `GET` y `POST /api/v1/requests/{code}/comments`.
- [ ] 11 - DTOs y manejador de excepciones `CollaborationExceptionHandler`.

### Pruebas backend
- [ ] 12 - `CollaborationApiTests.addCommentToVisibleRequestReturns201` (rojo → verde)
- [ ] 13 - `CollaborationApiTests.addCommentToInvisibleRequestReturns404`
- [ ] 14 - `CollaborationApiTests.emptyCommentIsRejectedWith400`
- [ ] 15 - `CollaborationApiTests.commentOver1000CharsIsRejectedWith400`
- [ ] 16 - `CollaborationApiTests.addCommentWithoutTokenReturns401`

### Frontend
- [ ] 17 - `CommentsTab.tsx` ya funciona contra MSW. Verificar que funciona contra el backend real (sin cambios estructurales si el contrato no cambió).
- [ ] 18 - Prueba `CommentsTab.test.tsx`: envío correcto actualiza la lista; error del servidor muestra toast.

## Evidencia rojo → verde
*(se completa al implementar)*

## Verificación
*(se completa al terminar la fase C)*

## Desviaciones
- `POST /comments` devuelve `RequestDetail` completo (para mantener compatibilidad con el frontend actual), no solo `CommentDto`. Se lee la solicitud actualizada tras el commit dentro de la misma transacción.

## Puntos a acordar en el PR
1. **Sevan:** el adaptador `RequestAccessAdapter` usa internamente `requests.application`; se acuerda si Sevan prefiere exponer un puerto explícito o si el acceso directo a su capa de aplicación es aceptable en este monolito.
2. **Angela:** el evento de transición (`RequestTransitionedEvent`) que dispara notificaciones se acuerda en la Fase E (notificaciones). Los comentarios no dependen de él.

## Pendientes
- [ ] Implementar (Fase C).
