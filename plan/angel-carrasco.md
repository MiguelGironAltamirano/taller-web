# Plan de AngelCarrasco — Colaboración y notificaciones

Plan general: [`README.md`](README.md).

## 1. Objetivo del módulo

Permitir la comunicación alrededor de cada solicitud (comentarios y evidencias) y mantener informados a los interesados mediante notificaciones, además de completar la gestión de contraseñas.

Este módulo se construye sobre la autenticación de Miguel (token, roles y `GET /api/v1/me`) y sobre el contrato de la API de la Fase A en `docs/api/`.

## 2. Alcance

**Backend** (paquetes `collaboration` y `notifications`)
- Comentarios en las solicitudes, visibles solo para quienes pueden ver la solicitud.
- Evidencias adjuntas: hasta 5 archivos por solicitud, máximo 5 MB cada uno, solo JPG, PNG o PDF; almacenadas en un volumen persistente y servidas solo a usuarios autorizados.
- Notificaciones dentro de la aplicación ante cambios de estado y asignaciones.
- Notificaciones por correo (opcional según el plazo).
- Cambio de contraseña (`PUT /api/v1/auth/password`) y recuperación por correo (opcional).

**Frontend** (`features/collaboration`, `features/notifications`)
- Sección de comentarios y de evidencias en el detalle de la solicitud.
- Campana de notificaciones con listado y marca de leída.
- Pantalla de cambio de contraseña.

**Documentación**
- Contratos de comentarios, evidencias y notificaciones en `docs/api/`; notas del manual de usuario.

## 3. Endpoints y pantallas

| Endpoint | Acceso | Notas |
|---|---|---|
| `GET /api/v1/requests/{codigo}/comments` | Token con visibilidad | Lista de comentarios |
| `POST /api/v1/requests/{codigo}/comments` | Token con visibilidad | Crea un comentario |
| `GET /api/v1/requests/{codigo}/evidences` | Token con visibilidad | Lista de evidencias |
| `POST /api/v1/requests/{codigo}/evidences` | Token con visibilidad | Carga un archivo; `400` por tipo o tamaño |
| `GET /api/v1/requests/{codigo}/evidences/{id}` | Token con visibilidad | Descarga la evidencia |
| `GET /api/v1/notifications` | Token | Notificaciones del usuario |
| `PATCH /api/v1/notifications/{id}/read` | Token | Marca como leída |
| `PUT /api/v1/auth/password` | Token | Cambio de contraseña |
| Recuperación de contraseña por correo | Público | Por definir en `docs/api/` antes de implementar |
| Notificaciones por correo | Interno | Por definir en `docs/api/` antes de implementar |

Pantallas: comentarios y evidencias en el detalle, campana y listado de notificaciones, cambio de contraseña.

## 4. Entregables por semana

| Semanas | Entregable |
|---|---|
| 8–10 | Contrato de comentarios, evidencias y notificaciones en `docs/api/` |
| 10 | **Presentación parcial** (todos) |
| 10–12 | Comentarios, evidencias, notificaciones dentro de la aplicación y cambio de contraseña |
| 13–14 | Pruebas de su módulo y correcciones; opcionales: correo y recuperación de contraseña |
| 15 | Manual técnico de su módulo |
| 16 | **Sustentación final** (todos) |

## 5. Pruebas obligatorias

- Comentar una solicitud visible → `201`; comentar una solicitud ajena → `404`.
- Subir un archivo JPG, PNG o PDF de hasta 5 MB → aceptado; un sexto archivo, uno de más de 5 MB o de otro tipo (por ejemplo `.exe`) → `400`.
- Descargar una evidencia como usuario sin permiso sobre la solicitud → denegado (`404` o `403`, según el contrato).
- Un cambio de estado genera una notificación para los interesados; una asignación notifica al técnico asignado.
- `GET /notifications` devuelve solo las del usuario autenticado; `PATCH /{id}/read` sobre una notificación ajena → `404`.
- Cambio de contraseña con la contraseña actual incorrecta → rechazado; con datos válidos, el siguiente login usa la nueva contraseña y la anterior deja de servir.
- Frontend: el contador de notificaciones sin leer disminuye al marcar una como leída; la carga de evidencias muestra el error cuando el archivo no cumple las reglas.

## 6. Dependencias con otros integrantes

| Qué | Quién |
|---|---|
| Recibe: token, roles, `/api/v1/me` y el módulo de autenticación donde se agrega el cambio de contraseña | Miguel |
| Recibe: solicitudes y su visibilidad por rol | Sevan |
| Recibe: eventos de transición y asignación | Angela |
| Recibe: componentes de `shared/ui` (toasts, estados, campos) | Lita |
| Provee: notificaciones y comentarios que se verán en el detalle | Sevan, Angela |

## 7. Archivos y paquetes que le pertenecen

- Backend: `collaboration`, `notifications` y sus migraciones; el almacenamiento de evidencias.
- Frontend: `frontend/src/features/collaboration/`, `frontend/src/features/notifications/`.
- Documentación: contratos de comentarios, evidencias y notificaciones en `docs/api/`.

## 8. Definición de terminado

Ver la [definición de terminado general](README.md#5-definición-de-terminado). Además: las evidencias nunca se sirven sin verificar la pertenencia a la solicitud.
