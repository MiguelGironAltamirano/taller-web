# Frontend

React 19 + Vite + TypeScript estricto + Tailwind 4. Estado de servidor con TanStack Query, formularios con React Hook Form + Zod, gráficos con Recharts y API simulada con MSW.

## Comandos

```bash
pnpm install
pnpm dev          # http://localhost:5173 contra el backend real
pnpm dev:mocks    # con la API simulada y las cuentas de ejemplo en el login
pnpm lint         # oxlint
pnpm typecheck
pnpm test         # vitest
pnpm build
```

## Datos simulados

Con `pnpm dev:mocks` (equivale a `VITE_USE_MOCKS=true pnpm dev`) la interfaz usa MSW en lugar del backend y el login muestra cuentas de ejemplo. Sin esa variable, `pnpm dev` habla con el backend real (ver `docs/guia-levantamiento.md`).

Los datos viven en memoria y se reinician al recargar. Los mocks cumplen `docs/api/contrato-v1.md` y sus extensiones documentadas en `docs/api/ui-extensions.md`.

Cuentas de ejemplo (contraseña `Demo12345!`, todas `@unmsm.edu.pe`): `lquispe@` (solicitante), `cmendoza@` (técnico), `avillanueva@` (supervisora) y `rsalazar@` (administrador). **No son las del backend real** (`estudiante@`, `supervisor.mantenimiento@`…, ver la guía de levantamiento).

## Estructura

```text
src/
├── app/            proveedores, rutas por rol y layout (sidebar, topbar, paleta Ctrl+K, navegación móvil)
├── features/       una carpeta por módulo: auth, requests, workflow, collaboration, satisfaction,
│                   notifications, triage, dashboard, admin (cada una expone solo su index.ts)
├── shared/
│   ├── api/        cliente HTTP, tipos del contrato, mapeadores, claves de consulta
│   ├── domain/     modelo de vista (estados, prioridades, roles)
│   ├── hooks/ lib/ utilidades (fechas de Lima, CSV, formato)
│   └── ui/         sistema de diseño: atoms/ y molecules/
├── mocks/          API simulada: datos de ejemplo, handlers por módulo y pruebas
└── styles/         tokens de diseño (modo claro y oscuro)
```

Dentro de cada feature: `containers/` (páginas que piden datos), `components/` (presentacionales), `api.ts` (hooks de TanStack Query), `schemas/` (Zod) y `lib/` (lógica pura con pruebas).

## Convenciones

- Colores, radios y sombras salen de los tokens (`src/styles/tokens.css`); no se escriben valores sueltos en los componentes.
- Una feature importa de otra solo por su `index.ts`.
- El token de acceso vive en memoria; el de renovación irá en una cookie `httpOnly` (los mocks lo simulan con `sessionStorage`).
- Los botones de acción de una solicitud salen de `allowedActions` del servidor, nunca se deducen en la interfaz.
