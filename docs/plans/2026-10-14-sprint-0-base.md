# Sprint 0 — Base del proyecto: plan de implementación

> **Ejecución:** tarea por tarea, en orden. Cada paso usa checkbox (`- [ ]`). Rama de trabajo: `feature/sprint-0-base` (sale de `dev`). El seguimiento vive en `odd/tasks/sprint-0-base.md`.

**Objetivo:** dejar un monorepo que compila, prueba y se despliega: backend Spring Boot con MySQL y Flyway, frontend React + TypeScript que consume la API, contenedores, CI y un primer despliegue en staging con Dokploy.

**Arquitectura:** monolito modular Spring Boot (paquetes por feature, capas `domain` / `application` / `infrastructure` vigiladas con ArchUnit) y SPA React servida por Nginx, que hace de proxy de `/api` hacia el backend. Todo se orquesta con Docker Compose, que es lo que despliega Dokploy.

**Stack:** Java 25 (Temurin, vía mise) · Spring Boot 4.1.1 · Maven Wrapper · MySQL 8.4 · Flyway · springdoc-openapi 3.1.1 · ArchUnit 1.5.1 · Testcontainers · Node 24 · pnpm 12.10.1 · Vite 8 · React 19 · TypeScript · TanStack Query · React Router · Tailwind CSS 4 · Vitest 5 · Testing Library · MSW · Docker Compose · GitHub Actions · Dokploy.

**Spec:** `docs/specs/2026-10-14-servicios-universitarios-design.md`

## Restricciones globales

- Java 25 y Spring Boot 4.1.1; versión fijada en `backend/pom.xml`.
- MySQL 8.4 (imagen `mysql:8.4`) en local, pruebas y despliegue.
- Paquete raíz `pe.edu.unmsm.servicios`; un paquete por módulo; `domain` sin dependencias de Spring ni JPA.
- API bajo `/api/v1`; errores en formato RFC 7807 (`application/problem+json`).
- Frontend en TypeScript estricto; textos de la interfaz en español.
- Secretos solo en variables de entorno; `infra/.env` nunca se versiona.
- Ramas: `main` (producción), `dev` (staging), `feature/*` desde `dev`. Conventional Commits.
- Finales de línea LF en el repositorio (los scripts `mvnw` se ejecutan en Linux).

## Foco de revisión

1. **Backend caído o con error 5xx:** la interfaz debe mostrar "API no disponible", nunca una pantalla en blanco → prueba en Task 4.
2. **Ruta inexistente de la API:** debe responder `404` con `application/problem+json`, no HTML → prueba en Task 3.
3. **Finales de línea CRLF** en `mvnw` rompen el build en Linux (contenedor y CI) → verificación en Task 1 y Task 5.
4. **Secretos versionados por error:** `infra/.env` debe estar ignorado → verificación en Task 1.
5. **Recarga en una ruta profunda de la SPA** (`/solicitudes`): Nginx debe devolver `index.html` con `200`, no `404` → verificación en Task 5.

---

### Task 1: Base del repositorio

**Files:**
- Create: `mise.toml`, `.gitattributes`, `.gitignore`, `.editorconfig`, `README.md`

**Interfaces:**
- Consumes: nada.
- Produces: Java 25 y Node 24 activos dentro del repo vía mise; reglas de finales de línea e ignorados que usan todas las tareas.

- [ ] **Step 1: Confirmar la rama de trabajo**

Run: `git branch --show-current`
Expected: `feature/sprint-0-base` (creada desde `dev` junto con este plan).

- [ ] **Step 2: Crear `mise.toml`**

```toml
[tools]
java = "temurin-25"
node = "24"
```

- [ ] **Step 3: Crear `.gitattributes`**

```gitattributes
* text=auto eol=lf
*.cmd text eol=crlf
*.bat text eol=crlf
*.pdf binary
*.png binary
*.jpg binary
```

- [ ] **Step 4: Crear `.gitignore`**

```gitignore
# Build
backend/target/
frontend/dist/
frontend/node_modules/
frontend/coverage/

# Environment
.env
infra/.env

# IDE / OS
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db
```

- [ ] **Step 5: Crear `.editorconfig`**

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 2

[*.java]
indent_size = 4

[*.{cmd,bat}]
end_of_line = crlf
```

- [ ] **Step 6: Crear `README.md`**

````markdown
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
(cd backend && ./mvnw spring-boot:test-run)   # backend con MySQL en Testcontainers
(cd frontend && pnpm install && pnpm dev)    # frontend en http://localhost:5173
```

## Ramas

`main` (producción) ← `dev` (staging) ← `feature/<nombre>`.
````

- [ ] **Step 7: Verificar**

Run: `mise install && mise exec -- java -version`
Expected: la primera línea contiene `25.`.

Run: `git check-ignore infra/.env`
Expected: imprime `infra/.env`.

- [ ] **Step 8: Commit**

```bash
git add mise.toml .gitattributes .gitignore .editorconfig README.md
git commit -m "chore: add repository base configuration"
```

---

### Task 2: Esqueleto del backend con MySQL y Flyway

**Files:**
- Create: `backend/` (generado por Spring Initializr)
- Modify: `backend/src/test/java/pe/edu/unmsm/servicios/TestcontainersConfiguration.java`
- Modify: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/resources/db/migration/V1__baseline.sql`
- Test: `backend/src/test/java/pe/edu/unmsm/servicios/FlywayBaselineTests.java`

**Interfaces:**
- Consumes: `mise.toml` (Java 25).
- Produces: aplicación `pe.edu.unmsm.servicios.ServiciosApplication`; `TestcontainersConfiguration` (MySQL 8.4 con `@ServiceConnection`) que importan todas las pruebas de integración; Maven Wrapper `backend/mvnw`.

- [ ] **Step 1: Generar el proyecto con Spring Initializr**

Desde la raíz del repo:

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d bootVersion=4.1.1 -d javaVersion=25 \
  -d groupId=pe.edu.unmsm -d artifactId=servicios -d name=servicios \
  -d packageName=pe.edu.unmsm.servicios \
  -d dependencies=web,actuator,validation,data-jpa,flyway,mysql,testcontainers \
  -d baseDir=backend -o backend.zip
unzip -q backend.zip && rm backend.zip
rm backend/HELP.md
rm -r backend/src/main/resources/static backend/src/main/resources/templates
```

Expected: existe `backend/pom.xml` con `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, `flyway-mysql`, `mysql-connector-j`, `spring-boot-testcontainers` y `testcontainers-mysql`.

- [ ] **Step 2: Fijar MySQL 8.4 en `TestcontainersConfiguration.java`**

```java
package pe.edu.unmsm.servicios;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

}
```

- [ ] **Step 3: Escribir la prueba que falla**

`backend/src/test/java/pe/edu/unmsm/servicios/FlywayBaselineTests.java`:

```java
package pe.edu.unmsm.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayBaselineTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void appliesBaselineMigration() {
		Integer applied = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success = 1",
				Integer.class);
		assertThat(applied).isEqualTo(1);
	}

}
```

- [ ] **Step 4: Ejecutar y verificar que falla**

Run: `cd backend && ./mvnw -q test -Dtest=FlywayBaselineTests`
Expected: FAIL — la tabla `flyway_schema_history` no existe o el conteo es `0`, porque todavía no hay migraciones.

- [ ] **Step 5: Agregar la migración base y la configuración**

`backend/src/main/resources/db/migration/V1__baseline.sql`:

```sql
-- Baseline: the schema is created by later migrations, one per module.
SELECT 1;
```

`backend/src/main/resources/application.properties`:

```properties
spring.application.name=servicios
app.version=@project.version@

spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

spring.mvc.problemdetails.enabled=true

management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `cd backend && ./mvnw -q test -Dtest=FlywayBaselineTests`
Expected: PASS.

- [ ] **Step 7: Ejecutar toda la suite**

Run: `cd backend && ./mvnw -q verify`
Expected: BUILD SUCCESS (incluye `ServiciosApplicationTests.contextLoads`).

- [ ] **Step 8: Commit**

```bash
git add backend
git update-index --chmod=+x backend/mvnw
git commit -m "feat(backend): add Spring Boot skeleton with MySQL and Flyway baseline"
```

---

### Task 3: Convenciones de la API y guardas de arquitectura

**Files:**
- Modify: `backend/pom.xml` (dependencias springdoc y ArchUnit)
- Create: `backend/src/main/java/pe/edu/unmsm/servicios/shared/infrastructure/web/SystemInfoController.java`
- Create: `backend/src/main/java/pe/edu/unmsm/servicios/shared/infrastructure/web/SystemInfoResponse.java`
- Test: `backend/src/test/java/pe/edu/unmsm/servicios/shared/infrastructure/web/ApiConventionsTests.java`
- Test: `backend/src/test/java/pe/edu/unmsm/servicios/ArchitectureTest.java`

**Interfaces:**
- Consumes: `TestcontainersConfiguration` (Task 2); propiedad `app.version`.
- Produces: `GET /api/v1/system/info` → `{"name":"servicios","version":"<versión del pom>"}`; OpenAPI en `/v3/api-docs` y Swagger UI en `/swagger-ui.html`; errores `application/problem+json`; reglas ArchUnit para todos los módulos futuros.

- [ ] **Step 1: Escribir la prueba de convenciones que falla**

`ApiConventionsTests.java`:

```java
package pe.edu.unmsm.servicios.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import pe.edu.unmsm.servicios.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApiConventionsTests {

	@Value("${local.server.port}")
	int port;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void systemInfoReturnsNameAndVersion() throws Exception {
		HttpResponse<String> response = get("/api/v1/system/info");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"name\":\"servicios\"").contains("\"version\":\"0.0.1-SNAPSHOT\"");
	}

	@Test
	void unknownApiRouteReturnsProblemDetails() throws Exception {
		HttpResponse<String> response = get("/api/v1/does-not-exist");
		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/problem+json"));
	}

	@Test
	void openApiDocumentIsPublished() throws Exception {
		HttpResponse<String> response = get("/v3/api-docs");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("/api/v1/system/info");
	}

}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd backend && ./mvnw -q test -Dtest=ApiConventionsTests`
Expected: FAIL — `systemInfoReturnsNameAndVersion` y `openApiDocumentIsPublished` reciben `404`.

- [ ] **Step 3: Agregar springdoc al `pom.xml`** (dentro de `<dependencies>`)

```xml
<dependency>
	<groupId>org.springdoc</groupId>
	<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
	<version>3.1.1</version>
</dependency>
```

- [ ] **Step 4: Implementar el endpoint**

`SystemInfoResponse.java`:

```java
package pe.edu.unmsm.servicios.shared.infrastructure.web;

public record SystemInfoResponse(String name, String version) {
}
```

`SystemInfoController.java`:

```java
package pe.edu.unmsm.servicios.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
class SystemInfoController {

	private final SystemInfoResponse info;

	SystemInfoController(@Value("${spring.application.name}") String name,
			@Value("${app.version}") String version) {
		this.info = new SystemInfoResponse(name, version);
	}

	@GetMapping("/info")
	SystemInfoResponse info() {
		return info;
	}

}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd backend && ./mvnw -q test -Dtest=ApiConventionsTests`
Expected: PASS (3 pruebas).

- [ ] **Step 6: Escribir la prueba de arquitectura que falla**

`ArchitectureTest.java`:

```java
package pe.edu.unmsm.servicios;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "pe.edu.unmsm.servicios", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule domainIsFrameworkFree = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "..application..", "..infrastructure..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule applicationDoesNotDependOnInfrastructure = noClasses()
			.that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAPackage("..infrastructure..")
			.allowEmptyShould(true);

}
```

- [ ] **Step 7: Ejecutar y verificar que falla**

Run: `cd backend && ./mvnw -q test -Dtest=ArchitectureTest`
Expected: FAIL de compilación — `package com.tngtech.archunit... does not exist`.

- [ ] **Step 8: Agregar ArchUnit al `pom.xml`** (dentro de `<dependencies>`)

```xml
<dependency>
	<groupId>com.tngtech.archunit</groupId>
	<artifactId>archunit-junit5</artifactId>
	<version>1.5.1</version>
	<scope>test</scope>
</dependency>
```

- [ ] **Step 9: Ejecutar y verificar que pasa**

Run: `cd backend && ./mvnw -q test -Dtest=ArchitectureTest`
Expected: PASS (2 reglas).

- [ ] **Step 10: Ejecutar toda la suite**

Run: `cd backend && ./mvnw -q verify`
Expected: BUILD SUCCESS.

- [ ] **Step 11: Commit**

```bash
git add backend
git commit -m "feat(backend): add system info endpoint, problem details, OpenAPI and architecture rules"
```

---

### Task 4: Esqueleto del frontend con cliente de API

**Files:**
- Create: `frontend/` (plantilla `react-ts` de create-vite 9.2.1)
- Modify: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/main.tsx`, `frontend/src/index.css`
- Create: `frontend/src/app/App.tsx`, `frontend/src/shared/api/client.ts`, `frontend/src/test/setup.ts`, `frontend/src/test/msw-server.ts`
- Delete: `frontend/src/App.tsx`, `frontend/src/App.css`, `frontend/src/assets/`
- Test: `frontend/src/shared/api/client.test.ts`, `frontend/src/app/App.test.tsx`

**Interfaces:**
- Consumes: `GET /api/v1/system/info` (Task 3).
- Produces: `apiGet<T>(path: string): Promise<T>` y `class ApiError { status: number; title: string; detail?: string }` en `src/shared/api/client.ts`; `server` de MSW en `src/test/msw-server.ts` para todas las pruebas futuras.

- [ ] **Step 1: Generar el proyecto e instalar dependencias**

Desde la raíz del repo:

```bash
corepack enable
pnpm create vite@9.2.1 frontend --template react-ts --no-interactive
cd frontend
npm pkg set packageManager=pnpm@12.10.1
pnpm install
pnpm add react-router @tanstack/react-query
pnpm add -D tailwindcss @tailwindcss/vite vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event msw
rm -r src/assets src/App.tsx src/App.css
npm pkg set scripts.test="vitest run" scripts.typecheck="tsc -b"
```

- [ ] **Step 2: Configurar Vite, Tailwind y Vitest**

`frontend/vite.config.ts`:

```ts
/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
```

`frontend/src/index.css`:

```css
@import "tailwindcss";
```

`frontend/src/test/msw-server.ts`:

```ts
import { setupServer } from 'msw/node'

export const server = setupServer()
```

`frontend/src/test/setup.ts`:

```ts
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './msw-server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  server.resetHandlers()
  cleanup()
})
afterAll(() => server.close())
```

- [ ] **Step 3: Escribir las pruebas del cliente que fallan**

`frontend/src/shared/api/client.test.ts`:

```ts
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw-server'
import { ApiError, apiGet } from './client'

describe('apiGet', () => {
  it('returns the parsed JSON body on success', async () => {
    server.use(http.get('*/api/v1/things', () => HttpResponse.json({ id: 1 })))
    await expect(apiGet<{ id: number }>('/things')).resolves.toEqual({ id: 1 })
  })

  it('throws ApiError with problem details fields', async () => {
    server.use(
      http.get('*/api/v1/things', () =>
        HttpResponse.json(
          { title: 'Not Found', status: 404, detail: 'No existe' },
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const error = await apiGet('/things').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 404, title: 'Not Found', detail: 'No existe' })
  })

  it('throws ApiError with the status text when the body is not problem details', async () => {
    server.use(http.get('*/api/v1/things', () => new HttpResponse('boom', { status: 503, statusText: 'Service Unavailable' })))
    await expect(apiGet('/things')).rejects.toMatchObject({ status: 503, title: 'Service Unavailable' })
  })
})
```

- [ ] **Step 4: Ejecutar y verificar que falla**

Run: `cd frontend && pnpm test src/shared/api`
Expected: FAIL — `Failed to resolve import "./client"`.

- [ ] **Step 5: Implementar el cliente**

`frontend/src/shared/api/client.ts`:

```ts
const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1'

type ProblemDetails = { title?: string; status?: number; detail?: string }

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail?: string

  constructor(status: number, title: string, detail?: string) {
    super(detail ?? title)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.detail = detail
  }
}

function toUrl(path: string): string {
  return new URL(`${API_BASE}${path}`, window.location.origin).toString()
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(toUrl(path), {
    headers: { Accept: 'application/json' },
    credentials: 'include',
  })
  if (!response.ok) {
    const contentType = response.headers.get('Content-Type') ?? ''
    if (contentType.includes('application/problem+json')) {
      const problem = (await response.json()) as ProblemDetails
      throw new ApiError(response.status, problem.title ?? response.statusText, problem.detail)
    }
    throw new ApiError(response.status, response.statusText)
  }
  return (await response.json()) as T
}
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `cd frontend && pnpm test src/shared/api`
Expected: PASS (3 pruebas).

- [ ] **Step 7: Escribir las pruebas de la App que fallan**

`frontend/src/app/App.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/msw-server'
import { App } from './App'

function renderApp() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App', () => {
  it('shows the API version when the backend responds', async () => {
    server.use(http.get('*/api/v1/system/info', () => HttpResponse.json({ name: 'servicios', version: '0.0.1-SNAPSHOT' })))
    renderApp()
    expect(screen.getByRole('heading', { name: 'Servicios Universitarios' })).toBeInTheDocument()
    expect(await screen.findByText('Versión 0.0.1-SNAPSHOT')).toBeInTheDocument()
  })

  it('shows an unavailable message when the backend fails', async () => {
    server.use(http.get('*/api/v1/system/info', () => new HttpResponse(null, { status: 503 })))
    renderApp()
    expect(await screen.findByText('API no disponible')).toBeInTheDocument()
  })
})
```

- [ ] **Step 8: Ejecutar y verificar que falla**

Run: `cd frontend && pnpm test src/app`
Expected: FAIL — `Failed to resolve import "./App"`.

- [ ] **Step 9: Implementar la App y el punto de entrada**

`frontend/src/app/App.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../shared/api/client'

type SystemInfo = { name: string; version: string }

export function App() {
  const info = useQuery({
    queryKey: ['system-info'],
    queryFn: () => apiGet<SystemInfo>('/system/info'),
  })

  let status = 'Conectando…'
  if (info.isSuccess) status = `Versión ${info.data.version}`
  if (info.isError) status = 'API no disponible'

  return (
    <main className="grid min-h-screen place-items-center bg-slate-50">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-slate-900">Servicios Universitarios</h1>
        <p className="mt-2 text-slate-600">{status}</p>
      </div>
    </main>
  )
}
```

`frontend/src/main.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import './index.css'

const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
```

- [ ] **Step 10: Ejecutar y verificar que pasa**

Run: `cd frontend && pnpm test`
Expected: PASS (5 pruebas).

- [ ] **Step 11: Verificar lint, tipos y build**

Run: `cd frontend && pnpm lint && pnpm typecheck && pnpm build`
Expected: sin errores; se genera `frontend/dist/index.html`.

- [ ] **Step 12: Commit**

```bash
git add frontend
git commit -m "feat(frontend): add React TypeScript skeleton with API client and system status"
```

---

### Task 5: Contenedores y Docker Compose

**Files:**
- Create: `backend/Dockerfile`, `backend/.dockerignore`
- Create: `frontend/Dockerfile`, `frontend/.dockerignore`, `frontend/nginx.conf`
- Create: `infra/docker-compose.yml`, `infra/.env.example`

**Interfaces:**
- Consumes: backend (Tasks 2–3) y frontend (Task 4).
- Produces: `infra/docker-compose.yml` con los servicios `mysql`, `backend` y `frontend`; el frontend expone el puerto 80 del contenedor y hace de proxy de `/api/` hacia `backend:8080`. Es el archivo que despliega Dokploy.

- [ ] **Step 1: Backend**

`backend/Dockerfile`:

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /app/target/servicios-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`backend/.dockerignore`:

```text
target/
.idea/
*.iml
```

- [ ] **Step 2: Frontend**

`frontend/nginx.conf`:

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  location / {
    try_files $uri /index.html;
  }
}
```

`frontend/Dockerfile`:

```dockerfile
FROM node:24-alpine AS build
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

`frontend/.dockerignore`:

```text
node_modules/
dist/
coverage/
```

- [ ] **Step 3: Compose y variables**

`infra/docker-compose.yml`:

```yaml
services:
  mysql:
    image: mysql:8.4
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build: ../backend
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    build: ../frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "${FRONTEND_PORT:-8088}:80"

volumes:
  mysql-data:
```

`infra/.env.example`:

```dotenv
MYSQL_DATABASE=servicios
MYSQL_USER=servicios
MYSQL_PASSWORD=change-me
MYSQL_ROOT_PASSWORD=change-me-too
FRONTEND_PORT=8088
```

- [ ] **Step 4: Verificar el stack completo**

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d --build
```

Run: `curl -s http://localhost:8088/api/v1/system/info`
Expected: `{"name":"servicios","version":"0.0.1-SNAPSHOT"}` (reintentar hasta 60 s mientras arranca el backend).

Run: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/solicitudes`
Expected: `200` (Nginx devuelve `index.html` en rutas profundas).

Run: `git ls-files --eol backend/mvnw`
Expected: la columna del índice muestra `i/lf`.

```bash
docker compose -f infra/docker-compose.yml --env-file infra/.env down -v
```

- [ ] **Step 5: Commit**

```bash
git add backend/Dockerfile backend/.dockerignore frontend/Dockerfile frontend/.dockerignore frontend/nginx.conf infra/docker-compose.yml infra/.env.example
git commit -m "build: add container images and Docker Compose stack"
```

---

### Task 6: Integración continua (GitHub Actions)

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `backend/mvnw verify` (Tasks 2–3); scripts `lint`, `typecheck`, `test`, `build` del frontend (Task 4).
- Produces: workflow `CI` con los jobs `backend` y `frontend`, ejecutado en cada pull request y en cada push a `main` o `dev`.

- [ ] **Step 1: Crear el workflow**

`.github/workflows/ci.yml`:

```yaml
name: CI

on:
  pull_request:
  push:
    branches: [main, dev]

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
      - run: ./mvnw -B verify

  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v5
      - uses: pnpm/action-setup@v4
        with:
          package_json_file: frontend/package.json
      - uses: actions/setup-node@v5
        with:
          node-version: '24'
          cache: pnpm
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: pnpm install --frozen-lockfile
      - run: pnpm lint
      - run: pnpm typecheck
      - run: pnpm test
      - run: pnpm build
```

- [ ] **Step 2: Verificar localmente los mismos comandos**

Run: `cd backend && ./mvnw -B verify`
Expected: BUILD SUCCESS.

Run: `cd frontend && pnpm install --frozen-lockfile && pnpm lint && pnpm typecheck && pnpm test && pnpm build`
Expected: todo sin errores.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add backend and frontend pipeline"
```

La ejecución real del workflow se verifica en Task 7, al existir el repositorio remoto.

---

### Task 7: Repositorio remoto y despliegue en staging (Dokploy)

**Requisito previo:** decisiones abiertas D1 (cuenta y repositorio de GitHub, credenciales de push con la identidad UNMSM) y D2 (dominio de staging, en adelante `STAGING_DOMAIN`) resueltas. Cada operación remota requiere autorización explícita del responsable del proyecto.

**Files:**
- Ninguno en el repositorio (configuración en GitHub y Dokploy).

**Interfaces:**
- Consumes: `infra/docker-compose.yml` (Task 5) y `.github/workflows/ci.yml` (Task 6).
- Produces: aplicación de staging en `https://STAGING_DOMAIN` que se redespliega con cada push a `dev`.

- [ ] **Step 1: Integrar la rama a `dev`**

```bash
git switch dev
git merge --no-ff feature/sprint-0-base -m "merge: sprint 0 base"
```

- [ ] **Step 2: Crear el repositorio remoto y publicar `main` y `dev`** (con autorización)

```bash
git remote add origin <URL del repositorio definida en D1>
git push -u origin main
git push -u origin dev
```

Expected: el workflow `CI` termina en verde para `dev` en la pestaña Actions de GitHub.

- [ ] **Step 3: Crear la aplicación en Dokploy** (en la interfaz web de Dokploy)

1. Nuevo proyecto `taller-web` → servicio de tipo **Compose** llamado `staging`.
2. Proveedor: GitHub; repositorio del paso 2; rama `dev`; ruta del compose `infra/docker-compose.yml`.
3. Variables de entorno: las de `infra/.env.example`, con contraseñas nuevas y seguras.
4. Dominio: `STAGING_DOMAIN` → servicio `frontend`, puerto `80`, HTTPS activado.
5. Activar el despliegue automático (Autodeploy) y desplegar.

- [ ] **Step 4: Verificar el despliegue**

Run: `curl -s https://STAGING_DOMAIN/api/v1/system/info`
Expected: `{"name":"servicios","version":"0.0.1-SNAPSHOT"}`.

Run: `curl -s -o /dev/null -w '%{http_code}' https://STAGING_DOMAIN/solicitudes`
Expected: `200`.

- [ ] **Step 5: Registrar el consumo de la VPS**

Anotar en `odd/tasks/sprint-0-base.md` la memoria y CPU de los tres contenedores (panel de Dokploy). Ese dato resuelve la decisión abierta D3 (observabilidad).
