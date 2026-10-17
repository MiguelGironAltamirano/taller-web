# 06 — Evidencia del primer desarrollo y uso responsable de IA

Estado a **17 de octubre de 2026** (semana 8). Se actualiza antes de la presentación parcial de la semana 10.

## Evidencia del primer desarrollo

### Qué está construido

| Área | Estado | Dónde |
|---|---|---|
| Base del repositorio, CI, migraciones y semillas | Hecho (Sprint 0) | `.github/workflows/ci.yml`, `backend/src/main/resources/db/migration/`, `infra/` |
| Autenticación: registro, login, renovación, cierre de sesión, `GET /me`, límite de intentos | Hecho | `backend/.../auth` |
| Catálogos: áreas, categorías, prioridades y ubicaciones jerárquicas | Hecho | `backend/.../catalog` |
| Solicitudes: registro con código y SLA, listado por rol, detalle con historial | Hecho | `backend/.../requests` |
| Flujo de atención: máquina de estados, asignación y reasignación, rechazo de solución, historial, técnicos del área | Hecho | `backend/.../workflow` |
| Interfaz completa de los cuatro roles (login, solicitudes, bandejas, detalle con acciones, dashboard, administración) | Hecho, contra la API simulada; conectada al backend real en los módulos anteriores | `frontend/` |
| Administración: usuarios (con contraseña temporal y desactivación), catálogos (áreas, categorías, SLA de prioridades, ubicaciones) y consulta de estados | Hecho, conectado al frontend | `backend/.../admin` |
| Comentarios, evidencias, notificaciones, triage, calificación, dashboard y auditoría en el backend | Pendiente (release candidate, semanas 10–12); triage y dashboard ya tienen contrato en `docs/api/contrato-v1.md` | — |

### Flujo de punta a punta que ya funciona

Login → registrar solicitud → guardar en MySQL → consultar la solicitud → evaluar → asignar → atender → resolver → confirmar el cierre, con seis entradas de historial. Lo ejercita la prueba `WorkflowApiTests.theCompleteFlowLeavesSixHistoryEntriesWithUserDateAndStatuses` sobre un MySQL real, y se puede recorrer a mano con el [prototipo](05-wireframes-y-prototipo.md#prototipo).

### Pull requests integrados en `dev`

| PR | Contenido |
|---|---|
| #1 | Base del Sprint 0 |
| #2 y #3 | Plan de trabajo del equipo, plan del Sprint 1 y guía de levantamiento |
| #4 | Plataforma, seguridad y autenticación |
| #5 y #6 | Endurecimiento de la autenticación, política de acceso y límite de intentos |
| #7 | Interfaz completa con API simulada (`feature/ui-design-system`) |
| #8 | Backend de catálogos y solicitudes |
| #9 | Backend del flujo de atención y su conexión con el frontend |
| #10 | Administración de usuarios y consulta de estados |
| #11 | Documento de especificación de la Fase I |
| Este PR | Administración de catálogos y contrato de triage e indicadores |

Cada integración pasó por el CI de GitHub Actions (backend y frontend). El historial completo de commits sigue Conventional Commits en español.

### Pruebas automatizadas

| Conjunto | Cantidad | Qué cubre |
|---|---|---|
| Backend (`./mvnw verify`) | 311 | Dominio sin Spring (máquina de estados, SLA, código de solicitud, política de acceso), casos de uso con dobles, endpoints con MySQL real en Testcontainers y reglas de arquitectura con ArchUnit |
| Frontend (`pnpm test`) | 161 | Lógica pura (SLA, transiciones, triage, filtros, reportes, esquemas), componentes y la API simulada |

El CI ejecuta además lint, verificación de tipos y build del frontend. La trazabilidad entre escenarios BDD y pruebas está en [03](03-epicas-y-historias.md); los escenarios sin prueba están marcados **Por escribir**.

### Decisiones y ajustes que dejó el primer desarrollo

- **La calificación es opcional.** El diseño visual la exigía para cerrar; se siguió el spec (§6): confirmar cierra la solicitud y la calificación se guarda después, si el solicitante la da.
- **El triage elige la regla con más coincidencias**, como indica el spec (§5.1), y no la primera que coincide.
- **Una prueba de integración dependía de ids fijos de usuario** y fallaba en el CI según el orden de ejecución; se corrigió para que no dependa del orden (commit `test(workflow): no fija ids de usuario porque users.id es AUTO_INCREMENT`).
- **Extensiones del contrato** que necesitó el diseño (campos de las bandejas, `closureReason`, `activeRequests` de los técnicos) están en [`docs/api/ui-extensions.md`](../api/ui-extensions.md).

### Qué falta antes de la presentación parcial (semana 10)

- Verificación manual del flujo completo con el backend real en el navegador.
- Pruebas de interfaz de los flujos de supervisor, técnico y solicitante.
- Actualizar esta página con las cifras finales y las capturas de pantalla de la demostración.

## Uso responsable de IA

El enunciado (sección 7) exige registrar cada uso significativo de IA. El equipo lo hace en [`docs/registro-uso-ia.md`](../registro-uso-ia.md), con herramienta, prompt, resultado, si se usó, validación, modificaciones y responsable.

Reglas que se siguen:

1. **La IA propone, una persona decide y responde.** Todo lo generado se revisa antes de integrarse; el responsable de cada entrada del registro es quien lo integró.
2. **Se valida con pruebas, no con confianza.** La lógica de dominio se escribió con pruebas; el CI corre en cada pull request y sus resultados se revisan antes de integrar.
3. **Se contrasta con las fuentes.** Los documentos de esta fase se derivaron del enunciado y del spec, y los nombres de pruebas que citan se verificaron contra el código.
4. **No se entregan secretos ni datos reales a la IA.** Las cuentas de ejemplo son ficticias y los secretos viven en variables de entorno.
5. **Autoría.** Los commits y los pull requests llevan solo a la persona responsable; el uso de IA queda documentado en el registro y no en los trailers de los commits.
6. **Los errores de la IA se anotan.** Por ejemplo, el prototipo de diseño tomaba la primera regla de triage que coincidía y el spec pide la que más coincide; se corrigió y quedó escrito.
