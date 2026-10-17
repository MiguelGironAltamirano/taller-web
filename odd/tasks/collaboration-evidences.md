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
- [ ] 2 - `V20261017_2110__create_evidences.sql`:
  ```sql
  CREATE TABLE evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    stored_name CHAR(36) NOT NULL,
    kind VARCHAR(5) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_evidences_request FOREIGN KEY (request_id) REFERENCES service_requests (id),
    CONSTRAINT fk_evidences_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
    CONSTRAINT ck_evidences_kind CHECK (kind IN ('IMG', 'PDF'))
  );
  CREATE INDEX ix_evidences_request ON evidences (request_id);
  ```

### Dominio (sin Spring)
- [ ] 3 - `Evidence(long id, long requestId, long uploaderId, String fileName, String storedName, EvidenceKind kind, long sizeBytes, Instant createdAt)`.
- [ ] 4 - `EvidenceKind { IMG, PDF }`.
- [ ] 5 - `FileTypeValidator.detect(byte[] header): EvidenceKind` — lee los primeros 8 bytes y los compara con las magic bytes. Lanza `FileTypeNotAllowedException` si no coincide con JPEG, PNG o PDF.
- [ ] 6 - `EvidenceRules.validate(long currentCount, long sizeBytes)` — lanza excepción si supera el límite de 5 o 5 MB.

### Puerto y adaptador de almacenamiento
- [ ] 7 - Puerto `EvidenceStorage` en `collaboration.application`:
  - `String store(String requestCode, byte[] content, String storedName): void`
  - `byte[] load(String storedName): byte[]`
  - `void delete(String storedName): void`
- [ ] 8 - Adaptador `DiskEvidenceStorage` en `collaboration.infrastructure`: lee la propiedad `app.evidences.storage-path`, previene path traversal, escribe con el UUID como nombre de archivo.

### Casos de uso
- [ ] 9 - `UploadEvidence`: recibe `(AuthenticatedUser, String code, InputStream, String originalName, long size)`. Orden: verificar visibilidad → leer magic bytes → validar tipo, tamaño y conteo → generar UUID → guardar en disco → insertar en BD (transaccional). Si el guardado en disco falla, la BD no se inserta (atómico dentro de la transacción).
- [ ] 10 - `ListEvidences`: lista las evidencias de una solicitud visible.
- [ ] 11 - `DownloadEvidence`: verifica visibilidad **y** que la evidencia pertenezca a la solicitud por `code`; luego lee del disco.

### Infraestructura REST y persistencia
- [ ] 12 - Endpoint `POST` con `@RequestPart("file") MultipartFile`.
- [ ] 13 - Endpoint `GET /{id}` con `ResponseEntity<Resource>` y cabeceras de seguridad.
- [ ] 14 - Repositorio JDBC `EvidenceRepository`.
- [ ] 15 - `CollaborationExceptionHandler` mapea `MaxUploadSizeExceededException`, `FileTypeNotAllowedException`, `EvidenceLimitExceededException`.

### Pruebas backend (integración con MySQL real + `@TempDir`)
- [ ] 16 - `EvidenceApiTests.uploadJpgReturns201`
- [ ] 17 - `EvidenceApiTests.uploadPngReturns201`
- [ ] 18 - `EvidenceApiTests.uploadPdfReturns201`
- [ ] 19 - `EvidenceApiTests.uploadSixthFileReturns400`
- [ ] 20 - `EvidenceApiTests.uploadFileOver5MbReturns400`
- [ ] 21 - `EvidenceApiTests.uploadNonAllowedTypeReturns400` (archivo `.exe`)
- [ ] 22 - `EvidenceApiTests.uploadExeRenamedToPngReturns400` (magic bytes de EXE, extensión `.png`)
- [ ] 23 - `EvidenceApiTests.uploadEmptyFileReturns400`
- [ ] 24 - `EvidenceApiTests.downloadWithoutPermissionReturns404`
- [ ] 25 - `EvidenceApiTests.downloadWithWrongCodeReturns404` (evidencia válida, código de solicitud equivocado)
- [ ] 26 - `EvidenceApiTests.downloadReturnsContentAndHeaders` (usuario autorizado, cabeceras correctas)
- [ ] 27 - `EvidenceApiTests.uploadWithoutTokenReturns401`

### Pruebas dominio (sin Spring)
- [ ] 28 - `FileTypeValidatorTest` — cada tipo permitido y un tipo no permitido.
- [ ] 29 - `EvidenceRulesTest` — límites de conteo y tamaño.

### Nginx y Docker
- [ ] 30 - `frontend/nginx.conf`: `client_max_body_size 6m` en `location /api/`.
- [ ] 31 - `infra/docker-compose.yml`: volumen `evidences_data`, montado en `/app/evidences`.
- [ ] 32 - `infra/.env.example`: variable `APP_EVIDENCES_STORAGE_PATH=/app/evidences`.
- [ ] 33 - `docs/guia-levantamiento.md`: sección de la variable y el volumen.

### Frontend
- [ ] 34 - Helper `apiUploadFile(path, file): Promise<T>` en `shared/api/client.ts` (mínimo y aditivo).
- [ ] 35 - `EvidenceTab.tsx`: subida real con `FileDropzone`, validación en cliente (tipo, tamaño, conteo), descarga autenticada con `fetch`+`Blob`.
- [ ] 36 - Prueba `EvidenceTab.test.tsx`: error cuando el archivo no cumple las reglas; no llama al servidor si la validación cliente falla.

## Evidencia rojo → verde
*(se completa al implementar)*

## Verificación
*(se completa al terminar la fase D)*

## Desviaciones
- La descarga requiere token en la cabecera `Authorization`; no se usa `<a href>` directo.

## Puntos a acordar en el PR
1. **Lita/Miguel:** adición mínima `apiUploadFile` en `shared/api/client.ts`.
2. **Sevan:** si "Nueva solicitud" envía metadatos de evidencias (`CreateRequestBody.evidences`), la subida real de los archivos ocurre en un segundo paso con `POST /requests/{code}/evidences`. Se propone en el PR pero no se modifica la pantalla de Sevan sin su acuerdo.

## Pendientes
- [ ] Implementar (Fase D).
