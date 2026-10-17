# 02 — Requerimientos

Prioridad: **M** = MVP (semana 10), **S** = release candidate (semana 12), **C** = opcional. La columna *Historia* enlaza con [03](03-epicas-y-historias.md).

## 1. Requerimientos funcionales

### Autenticación y cuentas

| Id | Requerimiento | Prioridad | Historia |
|---|---|---|---|
| RF-01 | El sistema permite el autorregistro de solicitantes con correo `@unmsm.edu.pe`, contraseña de 8 a 72 caracteres con al menos una letra y un dígito, nombre de 3 a 150 caracteres y tipo (estudiante, docente o administrativo) | M | HU-01 |
| RF-02 | El sistema permite iniciar sesión con correo y contraseña y emite un token de acceso de 15 minutos y un token de renovación de 7 días | M | HU-02 |
| RF-03 | El sistema renueva la sesión rotando el token de renovación y permite cerrar sesión revocándolo | M | HU-03 |
| RF-04 | Cada endpoint exige el rol correcto y verifica pertenencia: un solicitante solo ve sus solicitudes y un técnico solo las que tiene asignadas | M | HU-04 |
| RF-05 | Un usuario autenticado puede cambiar su contraseña | S | HU-05 |
| RF-06 | Recuperación de contraseña por correo | C | — |

### Registro de solicitudes

| Id | Requerimiento | Prioridad | Historia |
|---|---|---|---|
| RF-07 | Un solicitante registra una solicitud con categoría, ubicación, descripción (20 a 2000 caracteres sin contar espacios de los bordes) y prioridad sugerida | M | HU-06, HU-07 |
| RF-08 | Cada solicitud recibe un código `SOL-AAAA-NNNNNN`, correlativo por año, único e inmutable | M | HU-06 |
| RF-09 | El área de la solicitud es el área por defecto de la categoría elegida y define qué supervisor la recibe | M | HU-06 |
| RF-10 | La ubicación se elige en un catálogo jerárquico Facultad → Pabellón → Ambiente | M | HU-09 |
| RF-11 | El solicitante lista sus solicitudes (con filtro por estado y paginación) y ve el detalle con su historial | M | HU-08 |
| RF-12 | Los participantes de una solicitud pueden comentarla | S | HU-10 |
| RF-13 | Se pueden adjuntar hasta 5 evidencias por solicitud, de máximo 5 MB cada una, solo JPG, PNG o PDF, servidas solo a usuarios autorizados | S | HU-11 |

### Flujo de atención

| Id | Requerimiento | Prioridad | Historia |
|---|---|---|---|
| RF-14 | Las solicitudes siguen la máquina de estados del spec (§6): Registrada → En evaluación → Asignada → En atención → Resuelta → Cerrada, con las salidas Cerrada (no procede o duplicada) y En atención (solución rechazada) | M | HU-12 a HU-17 |
| RF-15 | El supervisor del área evalúa, asigna y reasigna; la asignación exige un técnico activo del área y la reasignación exige además un motivo | M | HU-12 a HU-14 |
| RF-16 | El técnico asignado inicia la atención y resuelve, y para resolver debe escribir la solución | M | HU-15 |
| RF-17 | El supervisor puede cerrar una solicitud Registrada o En evaluación indicando "No procede" o "Duplicada" | M | HU-16 |
| RF-18 | El solicitante confirma la solución (y puede calificarla) o la rechaza con un motivo | M / S | HU-17, HU-24 |
| RF-19 | Toda transición registra quién, cuándo, estado anterior, estado nuevo y motivo | M | HU-18 |
| RF-20 | Cualquier transición no listada se rechaza con `409`; si el rol no corresponde, con `403`; si faltan datos, con `400` | M | HU-19 |
| RF-21 | Cada prioridad define un SLA (Crítica 8 h, Alta 24 h, Media 72 h, Baja 120 h); la fecha de vencimiento es el registro más esas horas y se recalcula si el supervisor cambia la prioridad. Una solicitud está vencida si no llegó a Resuelta antes de esa fecha | S | HU-20 |
| RF-22 | Una solicitud Resuelta sin respuesta del solicitante en 72 horas se cierra automáticamente, sin calificación | S | HU-21 |

### Seguimiento, calidad y administración

| Id | Requerimiento | Prioridad | Historia |
|---|---|---|---|
| RF-23 | Notificaciones dentro de la aplicación por cada cambio de estado y asignación, con marca de leída | S | HU-22 |
| RF-24 | Al escribir la descripción, el sistema sugiere categoría y prioridad según la regla de palabras clave con más coincidencias; el solicitante puede aceptar o cambiar la sugerencia | S | HU-23 |
| RF-25 | El solicitante califica la atención de 1 a 5 con un comentario opcional | S | HU-24 |
| RF-26 | El dashboard muestra los nueve indicadores del enunciado (registradas, pendientes, atendidas, por categoría, por prioridad, tiempo promedio de atención, porcentaje resueltas, vencidas y por responsable), con filtros por rango de fechas y área y vista según el rol | S | HU-25 |
| RF-27 | El administrador gestiona usuarios, áreas, categorías, prioridades y ubicaciones, y consulta los estados en solo lectura | S | HU-26 |
| RF-28 | El administrador gestiona las reglas de triage | S | HU-27 |
| RF-29 | Notificaciones por correo | C | — |

## 2. Requerimientos no funcionales

| Id | Categoría | Requerimiento | Cómo se verifica |
|---|---|---|---|
| RNF-01 | Seguridad | Contraseñas con BCrypt; secretos solo en variables de entorno | Pruebas de registro; revisión del repositorio |
| RNF-02 | Seguridad | El token de renovación viaja en cookie `HttpOnly`, `Secure`, `SameSite=Strict`; el de acceso se guarda en memoria | Pruebas de login y de la cookie |
| RNF-03 | Seguridad | Límite de intentos de login (respuesta `429` con `Retry-After`), cabeceras de seguridad y CORS restringido al dominio del frontend | `LoginAttemptLimitTests`, `SecurityHeadersAndCorsTests` |
| RNF-04 | Seguridad | Validación de entradas en el backend (Bean Validation) y en el frontend (Zod); revisión contra OWASP Top 10 en la Fase III | Pruebas de validación; informe OWASP |
| RNF-05 | Calidad | Pruebas automatizadas: TDD en dominio y casos de uso, integración con MySQL real (Testcontainers) y reglas de arquitectura (ArchUnit) | `mvn verify` en CI |
| RNF-06 | Mantenibilidad | Monolito modular por feature con capas `domain`, `application` e `infrastructure`; `domain` sin dependencias de frameworks | `ArchitectureTest` |
| RNF-07 | Consistencia de la API | REST bajo `/api/v1`, errores en formato RFC 7807, documentación OpenAPI | Pruebas de convenciones de la API; Swagger UI |
| RNF-08 | Concurrencia | Dos transiciones simultáneas sobre la misma solicitud no pueden ganar ambas; los códigos concurrentes son distintos y consecutivos | `twoSimultaneousIdenticalTransitionsOnlyOneWins`, `tenParallelCreationsGetTenDistinctConsecutiveCodes` |
| RNF-09 | Usabilidad | Interfaz responsive (escritorio y móvil), modo claro y oscuro, mensajes de validación claros | Revisión manual con los cuatro roles; pruebas de componentes |
| RNF-10 | Rendimiento | Listados paginados con tope de tamaño de página; pruebas de carga y estrés con k6 en la Fase IV | `sizeIsCappedAndNegativePageIsTreatedAsFirst`; informe de carga |
| RNF-11 | Disponibilidad y operación | Despliegue por Docker Compose en staging (`dev`) y producción (`main`), con CI en cada pull request | Workflow de GitHub Actions |
| RNF-12 | Observabilidad | Logs JSON con identificador de traza, métricas Prometheus (Actuator + Micrometer) y trazas OpenTelemetry | Fase IV |
| RNF-13 | Trazabilidad | Toda transición queda en el historial y toda solicitud conserva su código | Pruebas de historial |
| RNF-14 | Uso responsable de IA | Cada uso significativo de IA se registra con herramienta, prompt, resultado, validación y responsable | [`docs/registro-uso-ia.md`](../registro-uso-ia.md) |
