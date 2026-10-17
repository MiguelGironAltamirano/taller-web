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
- [x] 2 - `V20261017_2100__create_comments.sql`: creada con constraints de clave foránea a `service_requests` y `users`, e índice en `(request_id, created_at)`.

### Dominio (sin Spring)
- [x] 3 - `Comment(long id, long requestId, long authorId, String authorName, String text, Instant createdAt)`.
- [x] 4 - Validación de texto (1–1000 caracteres con trim); excepción `CommentTextInvalidException`. TDD en `CommentTest` (4 pruebas unitarias en verde).

### Casos de uso y puertos
- [x] 5 - Puerto `RequestAccess` en `collaboration.application`: `RequestSnapshot findVisibleOrThrow(AuthenticatedUser user, String code)` (lanza `RequestNotFoundException` si no existe o no es visible).
- [x] 6 - `ListComments`: lista comentarios de una solicitud visible.
- [x] 7 - `CreateComment`: crea el comentario, emite notificación/evento a participantes visibles (excluyendo al autor). TDD en `CollaborationUseCasesTest` (4 pruebas en verde).

### Infraestructura
- [x] 8 - Adaptador `RequestAccessAdapter` en `collaboration.infrastructure`: implementa el puerto con `JdbcTemplate` y `AccessPolicy`.
- [x] 9 - Repositorio JDBC `JdbcCommentRepository` con join a `users` para obtener `authorName`.
- [x] 10 - Controlador `CollaborationController` con `GET` y `POST /api/v1/requests/{code}/comments`.
- [x] 11 - DTOs `CollaborationDtos` y manejador de excepciones `CollaborationExceptionHandler`.

### Pruebas backend
- [ ] 12 - `CollaborationApiTests.addCommentToVisibleRequestReturns201` — escrita en `CollaborationApiTests.java`, pendiente de ejecutar en CI (sin Docker local).
- [ ] 13 - `CollaborationApiTests.addCommentToInvisibleRequestReturns404` — escrita en `CollaborationApiTests.java`, pendiente de ejecutar en CI.
- [ ] 14 - `CollaborationApiTests.emptyCommentIsRejectedWith400` — escrita en `CollaborationApiTests.java`, pendiente de ejecutar en CI.
- [ ] 15 - `CollaborationApiTests.commentOver1000CharsIsRejectedWith400` — escrita en `CollaborationApiTests.java`, pendiente de ejecutar en CI.
- [ ] 16 - `CollaborationApiTests.addCommentWithoutTokenReturns401` — escrita en `CollaborationApiTests.java`, pendiente de ejecutar en CI.

### Frontend
- [x] 17 - `CommentsTab.tsx` actualizado para consumir `GET /comments` de forma independiente vía hook `useComments(code)`.
- [x] 18 - Prueba `CommentsTab.test.tsx`: renderizado de comentarios, envío con reset de formulario y notificación de error por toast (3 pruebas en verde).

## Evidencia rojo → verde

| Prueba | Rojo (antes) | Verde (después) |
|---|---|---|
| `CommentTest.*` (4 pruebas de dominio) | Clases de dominio no existían | Pasa (0.010 s) |
| `CollaborationUseCasesTest.*` (4 pruebas de casos de uso) | Casos de uso y puertos no existían | Pasa (0.017 s) |
| `CommentsTab.test.tsx` (3 pruebas frontend) | Archivo de prueba nuevo | Pasa (166 tests frontend en verde) |
| `CollaborationApiTests.*` (6 pruebas de integración) | Escritas en `CollaborationApiTests.java` | Pendientes de ejecutar en CI (sin Docker local) |

## Verificación
- Backend compila: `./mvnw -q -DskipTests compile` exit code 0.
- Pruebas unitarias backend: `./mvnw test -Dtest="*Test"`: 208 pruebas ejecutadas, 0 fallos (incluyendo ArchitectureTest).
- Frontend: `pnpm typecheck` (0 errores), `pnpm lint` (0 advertencias), `pnpm test` (166 pruebas ejecutadas, 0 fallos).

## Desviaciones
- `POST /comments` devuelve solo `CommentDto` (201). El frontend consulta `GET /comments` por separado. Se mantiene `comments?` en `RequestDetail` del frontend como opcional para retrocompatibilidad con los mocks, pero el backend desacopla totalmente los endpoints de colaboración de los de `requests`.

## Puntos a acordar en el PR
1. **Sevan:** el adaptador `RequestAccessAdapter` usa internamente `JdbcTemplate` sobre `service_requests` evaluando `AccessPolicy`; no invade `requests.application`.
2. **Angela:** el evento de transición (`RequestTransitionedEvent`) que dispara notificaciones se acuerda en la Fase E (notificaciones). Los comentarios no dependen de él.

## Pendientes
- [ ] Ejecución de pruebas de integración con Testcontainers en CI.
