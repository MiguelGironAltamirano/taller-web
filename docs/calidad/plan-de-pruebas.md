# Plan de pruebas

Responsable: Lita. Entregable del plan (`plan/lita.md` §2 y §4, semanas 8–10). Define qué se prueba, cómo, con qué criterios y quién; el resultado de su ejecución va al **informe de pruebas** (semanas 13–14).

## 1. Objetivo y alcance

Comprobar que el sistema de solicitudes de servicios universitarios cumple los requerimientos funcionales (RF) y no funcionales (RNF) de `docs/fase-1/02-requerimientos.md` y los escenarios de aceptación de `docs/fase-1/03-epicas-y-historias.md`, antes de cada integración en `dev` y de cada release a `main`.

**Dentro del alcance:** backend (`backend/`, todos los módulos), frontend (`frontend/`), integración entre ambos a través de nginx, accesibilidad, rendimiento (carga y estrés) y seguridad (con los resultados de Miguel).
**Fuera del alcance:** pruebas en dispositivos físicos distintos a escritorio y un móvil de referencia; navegadores distintos a Chrome/Edge (se revisa Firefox de forma manual y puntual).

## 2. Estrategia: niveles de prueba

| Nivel | Qué verifica | Herramientas | Dónde | Responsable |
|---|---|---|---|---|
| Unitarias de dominio y casos de uso | Reglas de negocio (máquina de estados, SLA, triage, calificación, código de solicitud) | JUnit 5, AssertJ; TDD | `backend/src/test/**/domain`, `**/application` | Cada dueño de módulo |
| Integración de API | Endpoints con base real: permisos por rol, validación, errores RFC 7807, concurrencia | Spring Boot Test + Testcontainers (MySQL 8.4) | `backend/src/test/**/*ApiTests.java` (17 clases) | Cada dueño de módulo |
| Arquitectura | Capas `domain` / `application` / `infrastructure` y dependencias entre módulos | ArchUnit | `ArchitectureTest` | Miguel |
| Frontend: lógica | Filtros, calendario, SLA, regla de atención, esquemas de validación, mapeos | Vitest | `frontend/src/**/*.test.ts` | Cada dueño de módulo |
| Frontend: componentes y pantallas | Comportamiento visible y accesible (consultas por rol y etiqueta), con la API simulada | Vitest + Testing Library + MSW | `frontend/src/**/*.test.tsx`, `shared/ui/ui.test.tsx` | Cada dueño; `shared/ui`: Lita |
| Extremo a extremo (E2E) | Recorridos reales por rol contra el stack completo con datos de demostración, sin simulaciones | Playwright | `frontend/e2e/*.e2e.ts`, `infra/e2e.sh` | Miguel (base) y Lita (aceptación) |
| Accesibilidad automática | WCAG 2.1 A/AA en las páginas principales | axe (`@axe-core/playwright`) | `frontend/e2e/a11y.e2e.ts`, `smoke.e2e.ts` | Lita |
| Accesibilidad manual | Flujos clave solo con teclado y con lector de pantalla (NVDA) | Lista de `docs/calidad/guia-accesibilidad.md` §9 | Informe de pruebas | Lita |
| Carga y estrés | Tiempos de respuesta y errores bajo carga del login y del listado | k6 | `infra/k6/` (por crear) | Lita |
| Seguridad | SAST y análisis de dependencias en CI; DAST (OWASP ZAP) sobre staging; revisión OWASP Top 10 | Las que defina Miguel | CI y staging | Miguel (Lita incorpora los resultados) |
| Exploratoria | Usabilidad, modo claro y oscuro, móvil (375 px) y escritorio (1440 px) | Navegador | Informe de pruebas | Lita, con el equipo |

**Situación actual (2026-11-14):** backend 591 pruebas en verde (`./mvnw verify`, integración de la PR #19); frontend 322 pruebas en 40 archivos (incluye las de todos los componentes de `shared/ui`); 26 pruebas E2E (incluye axe en 16 páginas, el detalle y el login).

## 3. Entornos y datos de prueba

| Entorno | Para qué | Datos |
|---|---|---|
| Testcontainers (MySQL 8.4) | Integración de API en local y en CI | Cada prueba prepara los suyos; migraciones Flyway reales |
| Frontend con MSW (`pnpm dev:mocks`, pruebas de Vitest) | Componentes y pantallas sin backend | `frontend/src/mocks/` alineados con el contrato |
| Stack Docker Compose (`infra/docker-compose.yml`) | E2E, carga y pruebas manuales | Usuarios `seed-dev` (contraseña `Demo12345!`, ver `docs/guia-levantamiento.md` §6) y datos de demostración `R__demo_data.sql` |
| Staging (despliegue de `dev`) | DAST y prueba de humo antes de un release | Datos de demostración |

Las pruebas no usan datos personales reales.

## 4. Criterios de entrada y de salida

**Para integrar un PR en `dev`:**
- CI en verde: `backend` (`./mvnw -B verify`), `frontend` (`pnpm lint`, `typecheck`, `test`, `build`) y `smoke and e2e` (`infra/e2e.sh`).
- Toda lógica nueva de dominio con prueba escrita primero (regla del equipo).
- Si cambia una pantalla: revisión en navegador a 375 px y 1440 px, en claro y oscuro, y la lista de accesibilidad de la guía.
- Revisión de un integrante de otro módulo (salvo excepción acordada).

**Para un release a `main`:**
- Todos los escenarios BDD de prioridad M con una prueba en verde o registrados como no cubiertos con su justificación.
- Sin hallazgos SAST o DAST de severidad alta abiertos.
- Umbrales de carga (sección 6) cumplidos en staging o en el stack local equivalente.
- Sin violaciones de axe en las páginas principales.

## 5. Pruebas de aceptación (trazabilidad)

`docs/fase-1/03-epicas-y-historias.md` tiene **27 historias y 109 escenarios** en formato Dado/Cuando/Entonces, cada uno con la prueba que lo ejecuta. A la fecha, **14 escenarios figuran como «Por escribir»** (5 de ellos con el backend pendiente en su momento) y algunos solo con prueba de frontend.

Procedimiento:
1. Revisar cada escenario «Por escribir»: si la funcionalidad ya existe (la PR #19 completó triage, dashboard, cierre automático y filtros), escribir la prueba en el nivel más bajo que lo cubra (API antes que E2E).
2. Relacionar las 26 pruebas E2E con los escenarios que recorren de punta a punta y anotarlas en la columna «Prueba».
3. Lo que quede sin prueba se registra en el informe como **no cubierto**, con el motivo y el riesgo.
4. Resultado esperado en el informe: tabla de cobertura por historia (cubierto / parcial / no cubierto).

## 6. Pruebas de carga y estrés (k6)

Requerimiento: RNF-10 (listados paginados con tope y pruebas de carga y estrés). Scripts en `infra/k6/`, contra el stack de Docker Compose con datos de demostración.

| Escenario | Endpoint | Perfil de carga | Umbrales (aprobado si se cumplen) |
|---|---|---|---|
| Login | `POST /api/v1/auth/login` | Carga: 20 usuarios virtuales durante 3 min | p95 < 800 ms; errores < 1 % |
| Listado de solicitudes | `GET /api/v1/requests?page=0&size=10` con token, alternando filtros de estado y texto | Carga: 50 usuarios virtuales durante 5 min | p95 < 500 ms; p99 < 1 s; errores < 1 % |
| Contadores de la bandeja | `GET /api/v1/requests/counts` | Junto con el listado | p95 < 400 ms |
| Estrés del listado | Igual que el listado | Rampa de 0 a 200 usuarios virtuales en 10 min | Registrar el punto en que p95 > 1 s o los errores > 5 %; el sistema debe recuperarse al bajar la carga |

Consideraciones:
- **Límite de intentos de login.** El backend cuenta cada intento antes de validar la contraseña (5 fallos por cliente y 20 por correo en 15 min, `app.auth.login-limit.*`). El escenario de login debe repartir la carga entre varias cuentas de demostración y no medir errores `429` como fallas del sistema; si hace falta, se sube el límite **solo** en el entorno de carga y se documenta.
- El token del listado se obtiene una vez por usuario virtual en `setup`, para no medir el login dentro del listado.
- Los umbrales son la propuesta inicial; se confirman en la primera ejecución (línea base) y cualquier ajuste se justifica en el informe.
- Se ejecuta en una máquina sin otras cargas pesadas y se registran sus características en el informe.

## 7. Accesibilidad

Reglas y lista de verificación en [`guia-accesibilidad.md`](guia-accesibilidad.md). Se prueba con axe en CI (automático) y con teclado y NVDA en los seis flujos clave (manual, semanas 13–14). Criterio: sin violaciones de axe de nivel A/AA y todos los flujos clave completables solo con teclado.

## 8. Gestión de defectos

- Cada defecto se registra como *issue* en GitHub con: pasos para reproducir, resultado esperado y obtenido, entorno, captura o traza y severidad.
- **Severidad:** crítica (bloquea un flujo clave o expone datos), alta (falla una regla de negocio o de seguridad), media (falla con alternativa), baja (estética o texto).
- Las críticas y altas bloquean el release. Cada corrección incluye una prueba de regresión que habría detectado el defecto.

## 9. Cronograma

| Semanas | Actividad |
|---|---|
| 8–10 | Este plan y la guía de accesibilidad; componentes de `shared/ui` con pruebas |
| 10–12 | Pruebas de los módulos junto con su desarrollo (cada dueño); pruebas faltantes de `shared/ui` |
| 13–14 | Escenarios «Por escribir» y trazabilidad E2E; k6 (línea base, carga y estrés); accesibilidad manual; recopilar SAST, DAST y observabilidad; **informe de pruebas** |
| 15 | Ajustes finales y consolidación en el informe final |

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| Las E2E dependen del stack completo y tardan | Se ejecutan en CI en un job propio; en local, `E2E_SKIP_BUILD=1` reutiliza imágenes |
| Resultados de carga distintos entre máquinas | Línea base en la misma máquina; registrar hardware y versión |
| Selectores E2E frágiles ante cambios de interfaz | Seleccionar por rol y nombre accesible (como las pruebas actuales), nunca por clases CSS |
| Escenarios sin prueba al cierre | Priorizar los de prioridad M y registrar el resto como no cubiertos |

## 11. Entregables

- Este plan y la guía de accesibilidad (`docs/calidad/`).
- Scripts de k6 en `infra/k6/` con instrucciones de ejecución.
- Informe de pruebas: resultados por nivel, cobertura de escenarios, resultados de k6, accesibilidad, SAST y DAST, defectos y conclusiones.
