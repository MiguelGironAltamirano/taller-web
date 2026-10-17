# Taller Web — Sistema Web Inteligente para la Gestión Integral de Servicios Universitarios

Monorepo del proyecto del curso Taller de Construcción de Software Web (UNMSM).

## Estructura

- `backend/` — API REST (Spring Boot, Java 25).
- `frontend/` — aplicación web (React + TypeScript).
- `infra/` — Docker Compose y configuración de despliegue.
- `docs/` — enunciado, especificaciones, planes e informes.

## Documentación

- [Guía de levantamiento](docs/guia-levantamiento.md): cómo clonar, configurar y ejecutar el proyecto.
- [Plan del equipo](plan/README.md): módulos, cronograma y planes individuales.

## Requisitos

- [mise](https://mise.jdx.dev) (instala Java 25 y Node 24 según `mise.toml`).
- Docker con Docker Compose.
- pnpm (vía `corepack enable`).

## Desarrollo local

```bash
mise install
(cd backend && ./mvnw spring-boot:test-run)   # backend con MySQL en Testcontainers
(cd frontend && pnpm install && pnpm dev)    # frontend en http://localhost:5173
```

### Solo el frontend, con cuentas de ejemplo

Si no quieres levantar el backend, usa la API simulada:

```bash
(cd frontend && pnpm install && pnpm dev:mocks)
```

El login muestra cuentas de ejemplo para entrar con cada rol (contraseña `Demo12345!`): `lquispe@` (solicitante), `cmendoza@` (técnico), `avillanueva@` (supervisora) y `rsalazar@` (administrador), todas `@unmsm.edu.pe`. Los datos viven en memoria y se reinician al recargar. No son las cuentas del backend real; esas están en [`docs/guia-levantamiento.md`](docs/guia-levantamiento.md) y en [`frontend/README.md`](frontend/README.md).

## Ramas

`main` (producción) ← `dev` (staging) ← `feature/<nombre>`.
