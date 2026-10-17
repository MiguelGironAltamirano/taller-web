# Feature: collaboration-evidences

## Objetivo
Implementar subida, listado y descarga autenticada de evidencias adjuntas a solicitudes: `GET`/`POST /api/v1/requests/{code}/evidences` y `GET /api/v1/requests/{code}/evidences/{id}`, con validación por magic bytes, almacenamiento en volumen persistente y descarga autenticada desde el frontend.

## Fuentes
- `plan/angel-carrasco.md` §2 y §5
- `docs/api/collaboration-notifications-v1.md` §4 (contrato acordado)
- `docs/fase-1/03-epicas-y-historias.md` HU-11 (RF-13)
- Spec `docs/specs/2026-10-14-servicios-universitarios-design.md` §5.1 (evidencias)

## Alcance
**Dentro:** migraciones, dominio `Evidence`, puerto `EvidenceStorage`, adaptador a disco, casos de uso, API, reglas de seguridad de archivos, configuración de Nginx y multipart, pruebas incluyendo magic bytes.  
**Fuera:** notificaciones por correo (Fase G).

## Configuración de infraestructura (trampa conocida)

### Spring multipart
```yaml
spring.servlet.multipart.max-file-size: 6MB
spring.servlet.multipart.max-request-size: 6MB
```
`MaxUploadSizeExceededException` → `400` en `CollaborationExceptionHandler`.

### Nginx (`frontend/nginx.conf`)
```nginx
location /api/ {
    client_max_body_size 6m;
    ...
}
```

### Volumen Docker
```yaml
# infra/docker-compose.yml
volumes:
  - evidences_data:/app/evidences
```
Variable de entorno: `APP_EVIDENCES_STORAGE_PATH` (defecto `/app/evidences`).

## Tareas

### Contrato
- [x] 1 - Contrato en `docs/api/collaboration-notifications-v1.md` §4 (2026-10-17).

### Migración
- [x] 2 - `V20261017_2110__create_evidences.sql` creada y aplicada.

### Dominio (sin Spring)
- [x] 3 - `Evidence(long id, long requestId, long uploaderId, String fileName, String storedName, EvidenceKind kind, long sizeBytes, Instant createdAt)`.
- [x] 4 - `EvidenceKind { IMG, PDF }`.
- [x] 5 - `FileTypeValidator.detect(byte[] header): EvidenceKind` — lee los primeros 8 bytes y los compara con las magic bytes. Lanza `FileTypeNotAllowedException` si no coincide con JPEG, PNG o PDF.
- [x] 6 - `EvidenceRules.validate(long currentCount, long sizeBytes)` — lanza excepción si supera el límite de 5 o 5 MB.

### Puerto y adaptador de almacenamiento
- [x] 7 - Puerto `EvidenceStorage` en `collaboration.application`:
  - `void store(String storedName, byte[] content)`
  - `DownloadedEvidence load(String storedName)`
  - `void delete(String storedName)`
- [x] 8 - Adaptador `DiskEvidenceStorage` en `collaboration.infrastructure`: lee la propiedad `app.evidences.storage-path`, previene path traversal, escribe con el UUID como nombre de archivo.

### Casos de uso
- [x] 9 - `UploadEvidence`: recibe `(AuthenticatedUser, String code, InputStream, String originalName, long size)`. Orden: verificar visibilidad → leer magic bytes → validar tipo, tamaño y conteo → generar UUID → guardar en disco → insertar en BD (transaccional). Si el guardado en disco falla, la BD no se inserta (atómico dentro de la transacción).
- [x] 10 - `ListEvidences`: lista las evidencias de una solicitud visible.
- [x] 11 - `DownloadEvidence`: verifica visibilidad **y** que la evidencia pertenezca a la solicitud por `code`; luego lee del disco.

### Infraestructura REST y persistencia
- [x] 12 - Endpoint `POST` con `@RequestPart("file") MultipartFile`.
- [x] 13 - Endpoint `GET /{id}` con `ResponseEntity<Resource>` y cabeceras de seguridad.
- [x] 14 - Repositorio JDBC `JdbcEvidenceRepository`.
- [x] 15 - `CollaborationExceptionHandler` mapea `MaxUploadSizeExceededException`, `FileTypeNotAllowedException`, `EvidenceLimitExceededException`, `EvidenceSizeLimitExceededException`, `EmptyFileException`.

### Pruebas backend (integración con MySQL real + `@TempDir`)
- [ ] 16 - `EvidenceApiTests.uploadJpgReturns201` (escrita, pendiente de ejecutar en CI)
- [ ] 17 - `EvidenceApiTests.uploadPngReturns201` (escrita, pendiente de ejecutar en CI)
- [ ] 18 - `EvidenceApiTests.uploadPdfReturns201` (escrita, pendiente de ejecutar en CI)
- [ ] 19 - `EvidenceApiTests.uploadSixthFileReturns400` (escrita, pendiente de ejecutar en CI)
- [ ] 20 - `EvidenceApiTests.uploadFileOver5MbReturns400` (escrita, pendiente de ejecutar en CI)
- [ ] 21 - `EvidenceApiTests.uploadNonAllowedTypeReturns400` (escrita, pendiente de ejecutar en CI)
- [ ] 22 - `EvidenceApiTests.uploadExeRenamedToPngReturns400` (escrita, pendiente de ejecutar en CI)
- [ ] 23 - `EvidenceApiTests.uploadEmptyFileReturns400` (escrita, pendiente de ejecutar en CI)
- [ ] 24 - `EvidenceApiTests.downloadWithoutPermissionReturns404` (escrita, pendiente de ejecutar en CI)
- [ ] 25 - `EvidenceApiTests.downloadWithWrongCodeReturns404` (escrita, pendiente de ejecutar en CI)
- [ ] 26 - `EvidenceApiTests.downloadReturnsContentAndHeaders` (escrita, pendiente de ejecutar en CI)
- [ ] 27 - `EvidenceApiTests.uploadWithoutTokenReturns401` (escrita, pendiente de ejecutar en CI)

### Pruebas dominio (sin Spring)
- [x] 28 - `FileTypeValidatorTest` — cada tipo permitido y tipos no permitidos (5 tests).
- [x] 29 - `EvidenceRulesTest` — límites de conteo y tamaño (4 tests).
- [x] 29b - `EvidenceUseCasesTest` — validación completa de UploadEvidence, ListEvidences y DownloadEvidence (5 tests).

### Nginx y Docker
- [x] 30 - `frontend/nginx.conf`: `client_max_body_size 6m` en `location /api/`.
- [x] 31 - `infra/docker-compose.yml`: volumen `evidences_data`, montado en `/app/evidences`.
- [x] 32 - `infra/.env.example`: variable `APP_EVIDENCES_STORAGE_PATH=/app/evidences`.
- [x] 33 - `docs/guia-levantamiento.md`: sección de la variable y el volumen.

### Frontend
- [x] 34 - Helper `apiUploadFile(path, file): Promise<T>` en `shared/api/client.ts` (mínimo y aditivo).
- [x] 35 - `EvidenceTab.tsx`: subida real con validación en cliente (tipo, tamaño, conteo) y descarga autenticada con `fetch`+`Blob`.
- [x] 36 - Prueba `EvidenceTab.test.tsx`: 4 pruebas (renderizado, rechazo de extensión, rechazo por tamaño, subida exitosa).

## Evidencia rojo → verde
- **Rojo inicial**: Pruebas `FileTypeValidatorTest`, `EvidenceRulesTest`, `EvidenceUseCasesTest` fallaron al compilar por ausencia de clases de dominio y casos de uso.
- **Verde**: Tras implementar clases de dominio, excepciones, validadores, almacenamiento en disco y casos de uso:
  - `./mvnw test -Dtest="*FileTypeValidatorTest,*EvidenceRulesTest,*EvidenceUseCasesTest"`: 14 tests run, 0 failures, 0 errors.
  - Frontend: `pnpm typecheck` OK, `pnpm lint` OK, `pnpm test EvidenceTab.test.tsx`: 4 tests passed. Suite completa frontend: 170 tests passed.

## Verificación
- Backend compilado sin errores con `./mvnw -q -DskipTests compile`.
- Frontend compilado y verificado con `pnpm typecheck`, `pnpm lint` y `pnpm test`.

## Desviaciones
- La descarga requiere token en la cabecera `Authorization`; no se usa `<a href>` directo.

## Puntos a acordar en el PR
1. **Lita/Miguel:** adición mínima `apiUploadFile` en `shared/api/client.ts`.
2. **Sevan:** si "Nueva solicitud" envía metadatos de evidencias (`CreateRequestBody.evidences`), la subida real de los archivos ocurre en un segundo paso con `POST /requests/{code}/evidences`. Se propone en el PR pero no se modifica la pantalla de Sevan sin su acuerdo.

## Pendientes
- Ninguno para Fase D. Pruebas de integración API (`EvidenceApiTests`) quedan anotadas para ejecución en CI por ausencia de Docker local.
