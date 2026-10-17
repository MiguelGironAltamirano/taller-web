# Guía de levantamiento

Cómo clonar, configurar y ejecutar el proyecto en Windows (PowerShell o Git Bash). Los comandos se pueden copiar y pegar; donde difieren, se muestran ambas variantes.

## 1. Requisitos previos

| Herramienta | Para qué | Notas |
|---|---|---|
| Git | Clonar y versionar | Incluye Git Bash |
| [mise](https://mise.jdx.dev) | Instala Java 25 y Node 24 según `mise.toml` | Verifica con `mise --version` |
| Docker Desktop con Compose | Levanta el stack completo y MySQL | Debe estar en ejecución |
| pnpm | Gestor de paquetes del frontend | Se activa con Corepack |

Activa pnpm una sola vez:

```bash
corepack enable
```

## 2. Clonar el repositorio

```bash
git clone <url-del-repositorio> taller-web
cd taller-web
git switch dev
```

## 3. Instalar Java y Node con mise

```bash
mise trust
mise install
```

Esto instala Java 25 (Temurin) y Node 24 definidos en `mise.toml`. Comprueba las versiones:

```bash
mise exec -- java -version
mise exec -- node -v
```

## 4. Archivo de entorno

PowerShell:

```powershell
Copy-Item infra/.env.example infra/.env
```

Git Bash:

```bash
cp infra/.env.example infra/.env
```

Edita `infra/.env` (no se versiona). Variables principales:

| Variable | Descripción |
|---|---|
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Base de datos; cambia las contraseñas de ejemplo |
| `FRONTEND_PORT` | Puerto de la aplicación (por defecto `8088`) |
| `APP_JWT_SECRET` | Secreto con el que se firman los tokens. Mínimo 32 caracteres y distinto en cada entorno. Nunca lo subas al repositorio |
| `APP_FLYWAY_LOCATIONS` | Ubicaciones de migraciones de Flyway |
| `APP_EVIDENCES_STORAGE_PATH` | Ruta para almacenar las evidencias adjuntas (por defecto `/app/evidences` en Docker o `./evidences` en local) |

Para cargar los usuarios de demostración, agrega:

```env
APP_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed-dev
```

`db/seed-dev` agrega usuarios de demostración. Úsalo **solo en local y staging, nunca en producción**. Si esas variables no aparecen en tu `infra/.env.example`, es porque el módulo de autenticación aún no está integrado en tu rama; agrégalas a mano cuando lo esté.

## 5. Stack completo con Docker

```bash
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d --build
```

- Aplicación: <http://localhost:8088>
- Comprobación de salud: <http://localhost:8088/api/v1/system/info>

Detener el stack:

```bash
docker compose -f infra/docker-compose.yml --env-file infra/.env down
```

Detener y borrar la base de datos (reinicio completo):

```bash
docker compose -f infra/docker-compose.yml --env-file infra/.env down -v
```

## 6. Usuarios de demostración

Disponibles cuando el módulo de autenticación está integrado y `APP_FLYWAY_LOCATIONS` incluye `db/seed-dev`. Contraseña de todos: `Demo12345!`.

| Rol | Correo |
|---|---|
| Administrador | `admin@unmsm.edu.pe` |
| Supervisor | `supervisor.mantenimiento@unmsm.edu.pe` |
| Técnico | `tecnico.mantenimiento@unmsm.edu.pe` |
| Solicitante | `estudiante@unmsm.edu.pe` |

> Con la API simulada (`VITE_USE_MOCKS=true`, sección 7) el login muestra otras cuentas de ejemplo, definidas en `frontend/src/mocks/db.ts` (ver `frontend/README.md`); las de esta tabla son las de la base de datos real.

## 7. Modo desarrollo

Backend (MySQL en Testcontainers; requiere Docker en ejecución):

```bash
cd backend
mise exec -- ./mvnw spring-boot:test-run
```

Frontend, en otra terminal (<http://localhost:5173>, con proxy de `/api` al puerto 8080):

```bash
cd frontend
pnpm install
pnpm dev
```

### Solo frontend, con API simulada

```bash
cd frontend
pnpm dev:mocks
```

Equivale a `VITE_USE_MOCKS=true pnpm dev` (PowerShell: `$env:VITE_USE_MOCKS='true'; pnpm dev`). El login muestra cuentas de ejemplo para entrar con cada rol (contraseña `Demo12345!`); los datos viven en memoria y se reinician al recargar.

Si `pnpm` falla con "no se reconoce como un comando interno o externo", tu `pnpm` global es anterior a la versión que fija `frontend/package.json` (12.10.1). Se arregla con `npm install -g pnpm@12.10.1` o con `corepack enable`. Mientras tanto, los scripts se pueden ejecutar con `npm run dev:mocks`, `npm run test`, etc. (instalar dependencias sí requiere `pnpm`).

## 8. Ejecutar las pruebas

| Qué | Comando |
|---|---|
| Backend (unitarias, integración y ArchUnit) | `cd backend && mise exec -- ./mvnw verify` |
| Frontend: pruebas | `cd frontend && pnpm test` |
| Frontend: lint | `cd frontend && pnpm lint` |
| Frontend: tipos | `cd frontend && pnpm typecheck` |

Las pruebas de integración del backend usan Testcontainers, por lo que Docker debe estar en ejecución.

## 9. Flujo de Git

1. Parte siempre de `dev`: `git switch dev && git pull`.
2. Crea tu rama: `git switch -c feature/<modulo>-<descripcion>`.
3. Haz commits con Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`, `ci:`).
4. Abre el pull request hacia `dev`. El CI debe estar en verde y otro integrante debe revisarlo.
5. Nada entra directo a `dev` ni a `main`; `main` solo recibe releases desde `dev`.

Más detalle en [`plan/README.md`](../plan/README.md).

## 10. Solución de problemas

| Síntoma | Causa probable y solución |
|---|---|
| `Cannot connect to the Docker daemon` o error de Testcontainers | Docker Desktop no está en ejecución. Ábrelo, espera a que indique que está activo y reintenta |
| El puerto 8088, 8080 o 5173 está ocupado | Cierra el proceso que lo usa o cambia `FRONTEND_PORT` en `infra/.env`. Para ver quién usa un puerto: `netstat -ano \| findstr :8088` |
| Error de versión de Java (por ejemplo `release version 25 not supported`) | Estás usando otro JDK. Ejecuta los comandos con `mise exec --` o confirma que `mise install` terminó |
| `mvnw` falla con errores de saltos de línea (`bad interpreter`, `\r`) | El repositorio fuerza LF mediante `.gitattributes`, por lo que no hace falta cambiar `core.autocrlf`. Si el archivo quedó dañado, vuelve a clonar el repositorio |
| `pnpm install --frozen-lockfile` falla por desajuste | `pnpm-lock.yaml` no coincide con `package.json`. Ejecuta `pnpm install`, revisa el cambio del lockfile y súbelo junto con tu cambio |
| `pnpm` no se reconoce | Ejecuta `corepack enable` y abre una terminal nueva |
| Los usuarios de demostración no existen | Falta `APP_FLYWAY_LOCATIONS` con `db/seed-dev` en `infra/.env`. Corrígelo y reinicia con `down -v` seguido de `up -d --build` |
