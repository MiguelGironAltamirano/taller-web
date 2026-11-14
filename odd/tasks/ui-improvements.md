# Feature: ui-improvements

## Objective
Pulir la experiencia visual y corregir los problemas funcionales detectados en la revisión de la interfaz (documento de mejoras de 14 puntos), con una estética institucional sobria, consistente en modo claro y oscuro, sin cambiar reglas de negocio.

## Problem / Why
La interfaz funcionaba, pero tenía problemas de claridad y acabado: estados con colores poco semánticos, barra de progreso con líneas sobre los números, botones sin estados de interacción, solicitudes nuevas que no destacaban, desplegables y notificaciones genéricos, y dos fallos funcionales (solución vacía al cerrar y equipo vacío del supervisor).

## Sources
- Documento de mejoras pendientes (14 puntos, entregado por Lita).
- Plan: `plan/lita.md` (`shared/ui`, accesibilidad y diseño responsive).
- Contrato: `docs/api/ui-extensions.md`.

## Scope
In: `shared/ui` (componentes y tokens), layout (barra lateral, barra superior, ruta), acceso (login y registro), bandeja, calendario, detalle de solicitud, notificaciones, dashboard (tarjetas), kanban.
Out: reglas de negocio, contrato de la API, backend (salvo lo que ya resolvió la integración de `finish-local`).

## Tasks
### Bloque A — correcciones funcionales (PR #16)
- [x] Solución vacía al estudiante tras cerrar: `ClosedCard` muestra la solución aplicada (luego el backend pasó a enviar `solution`, PR #19).
- [x] Equipo vacío del supervisor: `useTeam` apuntaba a un endpoint inexistente (luego `GET /dashboard/team` se implementó en el backend, PR #19).

### Bloque B — componentes compartidos (PR #16 y #20)
- [x] Estados con colores semánticos e ícono por estado; prioridades con barras de nivel (no dependen solo del color).
- [x] Stepper sin líneas sobre los círculos, con relleno animado, halo en la etapa actual, ícono y fecha por etapa.
- [x] Botones con hover, foco, presión y deshabilitado coherentes; spinner al cargar; hover en botones nativos.
- [x] Campos con foco visible, error con ícono junto al campo y foco rojo si son inválidos; `Alert` con tono de advertencia.
- [x] Avisos no modales con ícono por tono, título, barra de tiempo, cierre manual, pausa al pasar el mouse y duración por tono.
- [x] Paleta: sin franjas laterales y verde sobrio.

### Bloque C — bandeja, calendario y atención (PR #16)
- [x] Desplegables con estilo propio (`appearance: base-select` en Chrome y Edge; siguen siendo `<select>` nativos).
- [x] "Requieren tu atención": acción pendiente por rol + notificaciones sin leer; panel, filas resaltadas, acción directa y vista propia.
- [x] Bandeja con pestañas de estado y tabla a dos líneas; calendario con navegación, resumen, color por plazo y agenda por día.

### Pulido final (rama `feature/lita-ui-polish`)
- [x] Barra lateral, logo (monograma provisional) y favicon.
- [x] Login: panel con el escudo oficial fundido con el fondo, tarjeta con borde guinda y encabezado guinda con título en serif; registro con el mismo estilo; contraseña con botón para mostrarla; errores por campo.
- [x] Bienvenida no bloqueante tras iniciar sesión (aviso con el nombre); revisada contra heurísticas: se descartó un modal.
- [x] Tarjetas de indicadores (`KpiCard`) con ícono y variación; dashboard en cuadrícula 3×2.
- [x] Kanban: tarjetas que requieren acción resaltadas, con etiqueta de acción y punto de novedad.
- [x] Encabezados de sección: tarjeta blanca con borde guinda, ícono guinda y título en serif.
- [x] Ruta de navegación clicable ("Inicio" lleva al inicio del rol) con ícono y separadores.
- [x] Notificaciones (página y panel): ícono y tono por tipo, punto de no leída, grupos por día y estado vacío.
- [ ] Revisión de otro integrante en el PR de `feature/lita-ui-polish`.

### Pruebas de `shared/ui` (rama `feature/lita-shared-ui-tests`)
- [x] Pruebas de todos los componentes, junto a cada uno (`atoms/*.test.tsx`, `molecules/*.test.tsx`), centradas en el comportamiento y la accesibilidad: nombres accesibles, roles, foco y teclado, estados deshabilitado y ocupado, anuncios (`status`/`alert`), duración y pausa de los avisos.
- [x] `ErrorState` con «Reintentar» (requisito de `plan/lita.md` §5), usado en la bandeja y en el detalle; prueba de pantalla que simula un error del servidor y el reintento.
- [x] Corrección hallada por las pruebas: las pestañas con contador se anunciaban «Comentarios0»; ahora «Comentarios 0».

## Decisions
- **Sin franjas laterales de acento**: se reemplazaron por bordes y fondos tenues a pedido de la responsable.
- **Desplegables nativos estilizados** en lugar de un componente propio: conserva teclado, formularios y pruebas (`selectOptions`).
- **"Requieren tu atención" usa el mecanismo existente** (notificaciones sin leer y refresco cada 30 s), sin infraestructura nueva.
- **El registro de cuentas se mantiene** como enlace discreto en el login: es el requisito RF-01 (HU-01).
- **No se inventó un logo oficial**: el monograma "U" es provisional; el escudo oficial se usa solo como imagen del panel de acceso (`public/brand/escudo-unmsm.webp`, provisto por el equipo).
- **Serif del sistema (Georgia)** para títulos: no se añadió una fuente descargable (la instalación del paquete no se aprobó y la CSP bloquea Google Fonts).
- **Bienvenida como aviso, no modal**: un modal en cada inicio de sesión rompe eficiencia de uso, diseño minimalista y control del usuario.

## Acceptance criteria
- `pnpm lint`, `pnpm typecheck`, `pnpm typecheck:e2e`, `pnpm test` y `pnpm build` en verde.
- Las pantallas se ven bien en modo claro y oscuro y en móvil.
- Ningún cambio altera reglas de negocio ni el contrato de la API.

## Evidence
- PR #16 y #20 fusionados en `dev`.
- `feature/lita-ui-polish`: 227 pruebas del frontend en verde, lint, typecheck (también E2E) y build; revisión en el navegador como supervisora, técnico y solicitante, en claro, oscuro y móvil.
- No verificado aquí: la suite E2E de Playwright (necesita el stack de Docker); se ajustó un selector de `e2e/requester.e2e.ts` por el nuevo enlace de la ruta.
