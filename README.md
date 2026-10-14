# Taller Web — Sistema Web Inteligente para la Gestión Integral de Servicios Universitarios

Monorepo del proyecto del curso Taller de Construcción de Software Web (UNMSM).

## Estructura

- `backend/` — API REST (Spring Boot, Java 25).
- `frontend/` — aplicación web (React + TypeScript).
- `infra/` — Docker Compose y configuración de despliegue.
- `docs/` — enunciado, especificaciones, planes e informes.

## Requisitos

- [mise](https://mise.jdx.dev) (instala Java 25 y Node 24 según `mise.toml`).
- Docker con Docker Compose.
- pnpm (vía `corepack enable`).

## Desarrollo local

```bash
mise install
cd backend && ./mvnw spring-boot:test-run   # backend con MySQL en Testcontainers
cd frontend && pnpm install && pnpm dev      # frontend en http://localhost:5173
```

## Ramas

`main` (producción) ← `dev` (staging) ← `feature/<nombre>`.
