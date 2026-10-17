# Feature: ui-design-system

## Objective
Integrar en `frontend/` el diseño final de Claude Design ("Servicios Universitarios UNMSM"), solo interfaz, con datos simulados (MSW) y sin conectar con el backend.

## Problem / Why
El diseño estaba como prototipo suelto (`.dc.html` + `support.js`). Hay que convertirlo en el frontend real del proyecto, con la arquitectura del spec, para que cada integrante construya su módulo sobre él en lugar de rehacer pantallas.

## Sources
- Spec: `docs/specs/2026-10-14-servicios-universitarios-design.md`
- Diseño: proyecto de Claude Design `Servicios Universitarios UNMSM` (archivos `Servicios Universitarios UNMSM.dc.html` y `support.js`)
- Contrato: `docs/api/contrato-v1.md` y las extensiones en `docs/api/ui-extensions.md`
- Plan del equipo: `plan/README.md` (propietarios de carpetas) y `plan/sevan.md` §9 (funcionalidades extra)

## Scope
In: tokens y modo claro/oscuro, `shared/ui`, layout y navegación por rol, las 16 vistas del diseño, API simulada con MSW alineada al contrato, pruebas.
Out: backend, subida real de evidencias, exportación a PDF, cierre automático a las 72 h.

## Tasks
- [x] Dependencias del spec: react-hook-form, zod, @hookform/resolvers, recharts
- [x] Tokens del diseño como variables CSS (claro/oscuro) enlazadas a Tailwind
- [x] Lógica de dominio con pruebas: SLA, transiciones por rol, triage, código de solicitud, filtros, calendario, reportes, esquemas Zod
- [x] `shared/ui`: átomos y moléculas con pruebas de comportamiento
- [x] API simulada con MSW (auth, catálogos, solicitudes, flujo, notificaciones, triage, dashboard, admin) con pruebas de los escenarios de los planes
- [x] Sesión en memoria con renovación al cargar, guardas por rol, login, registro, cambio de contraseña
- [x] Layout: sidebar colapsable, topbar con paleta Ctrl+K, perfil, notificaciones, cajón y navegación inferior móvil
- [x] Solicitudes: inicio, listado (filtros en URL, kanban, CSV), nueva solicitud (triage, evidencias), detalle (SLA, historial, comentarios, evidencias), calendario
- [x] Flujo: acciones por rol con modal, confirmar o rechazar la solución
- [x] Dashboard, equipo, reportes; usuarios, catálogos, reglas de triage, auditoría, configuración
- [x] Verificación manual en navegador: login, restauración de sesión, dashboard, listado, detalle y asignación de técnico; vista móvil a 390 px
- [x] Integrar `origin/dev` (Miguel integró en paralelo layout, auth, cliente y mocks): conflictos resueltos adoptando su `sessionStore` y su cliente HTTP
- [x] PR #7 a `dev`: fusionado el 17 de octubre de 2026, con los 2 checks de CI en verde
- [ ] Revisión de otro integrante (el PR se fusionó sin revisiones): pedir a Miguel que revise lo que se reemplazó de auth y layout, y a Lita `shared/ui`
- [x] Prueba de interfaz de "Nueva solicitud" (código generado, lista, descripción fuera de rango, campos obligatorios)
- [ ] Pruebas de accesibilidad con herramienta automática (axe) y revisión en lector de pantalla

## Decisions
- Los mocks usan el contrato del MVP (estados `EN_ATENCION`, fechas ISO, ids); lo que el diseño necesita de más está documentado en `docs/api/ui-extensions.md`.
- Los filtros del listado viven en la URL (`?view=venc&status=Resuelta`) para poder compartir vistas y enlazar desde el inicio y el dashboard.
- La calificación es obligatoria al confirmar (como en el diseño) aunque el spec la declara opcional: ver `plan/sevan.md` §9.
- El triage sigue el spec (regla con más coincidencias) y no el prototipo (primera coincidencia).
- La API simulada se activa con `VITE_USE_MOCKS=true` (variable del equipo, ver `docs/guia-levantamiento.md`); con ella el login muestra cuentas de ejemplo. Sin la variable, `pnpm dev` va contra el backend real.

## Acceptance criteria
- `pnpm lint`, `pnpm typecheck`, `pnpm test` y `pnpm build` pasan en `frontend/`.
- Con `pnpm dev` se recorren los cuatro roles con las cuentas de ejemplo y todas las pantallas del diseño funcionan con datos simulados.
- Ninguna pantalla muestra datos fuera de la visibilidad del rol (el solicitante solo ve lo suyo, etc.).

## Evidence
- `pnpm test`: 144 pruebas en 21 archivos (tras el merge con `dev` y la prueba de "Nueva solicitud").
- `pnpm lint` (oxlint) sin avisos, `pnpm typecheck` y `pnpm build` sin errores.
- Navegador (Chrome, dev server): login como supervisora, recarga con sesión restaurada, dashboard con gráficos, listado con SLA, detalle y asignación de técnico (estado `Asignada`, historial 3 entradas); vista móvil en 390 px sin iconos de escritorio.
