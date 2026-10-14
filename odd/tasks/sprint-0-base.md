# Feature: sprint-0-base

## Objective
Deliver a monorepo that builds, tests, and deploys: Spring Boot backend with MySQL and Flyway, React + TypeScript frontend consuming the API, containers, CI, and a first staging deployment on Dokploy.

## Problem / Why
Week 8 of 16 with nothing built; the week 10 MVP needs a working base and an early deployment to remove infrastructure risk.

## Sources
- Spec: `docs/specs/2026-10-14-servicios-universitarios-design.md`
- Plan: `docs/plans/2026-10-14-sprint-0-base.md` (exact steps, code, and expected results)

## Scope
In: plan Tasks 1–7. Out: business modules (auth, requests, workflow), which belong to the Sprint 1 plan.

## Tasks
- [x] T1 — Repository base (mise, line endings, ignores, editorconfig, README) — route: inline (mechanical files). Evidence: `mise exec -- java -version` → 25.0.4.1 LTS; node v24.21.0; `git check-ignore infra/.env` → `infra/.env`.
- [x] T2 — Backend skeleton with MySQL 8.4 and Flyway baseline — route: delegated writer, TDD. Evidence: RED `appliesBaselineMigration` expected 1 but was 0; GREEN `-Dtest=FlywayBaselineTests` exit 0; `./mvnw -q verify` exit 0 (2 tests); orchestrator spot check re-run exit 0. Scaffold step done by orchestrator (network).
- [x] T3 — API conventions (system info, problem details, OpenAPI) and architecture rules — route: delegated writer, TDD. Evidence: RED A expected 200 but was 404; RED B archunit package missing; GREEN ApiConventionsTests 3, ArchitectureTest 2 rules; verify 7 tests; spot check exit 0; pom adds only springdoc 3.1.1 and archunit-junit5 1.5.1.
- [x] T4 — Frontend skeleton with API client and system status — route: delegated writer, TDD. Evidence: RED A cannot resolve ./client, RED B cannot resolve ./App; GREEN 5 tests; lint (oxlint), typecheck, build pass; spot check 5/5. Scaffold + installs done by orchestrator (network). Deviation: MSW 3 uses `onUnhandledFrame` instead of `onUnhandledRequest`; template lint is oxlint.
- [x] T5 — Container images and Docker Compose stack — route: delegated writer; verification by writer (RDD on) + orchestrator file check. Evidence: compose up --build exit 0; /api/v1/system/info via nginx → {"name":"servicios","version":"0.0.1-SNAPSHOT"}; /solicitudes → 200; mvnw i/lf; down -v exit 0; infra/.env ignored.
- [~] T6 — CI pipeline (local checks done; reopened until the first real GitHub Actions run is green) — route: delegated writer; verification by writer (RDD on) + orchestrator diff check. Evidence: local CI-equivalent commands all pass (mvnw verify 7 tests; pnpm install --frozen-lockfile, lint, typecheck, test 5, build). Real GitHub Actions run pending (triggers on pull_request and push to dev/main).
- [x] T6.1 — Hardening from review review-78bf9f9dc9b800c8: nginx dynamic upstream resolution, MySQL healthcheck after init, CI least-privilege permissions — route: delegated writer
- [ ] T7 — Remote repository and Dokploy staging — route: owner + orchestrator; blocked by D1/D2

## Acceptance criteria
- `backend/mvnw verify` passes (Flyway baseline, API conventions, architecture rules).
- `pnpm lint && pnpm typecheck && pnpm test && pnpm build` pass in `frontend/`.
- The Compose stack serves `/api/v1/system/info` through Nginx and returns `200` for deep SPA routes.
- CI runs both jobs on push to `dev`; staging responds over HTTPS.

## Checks
Per task: the plan's RED/GREEN commands. At close: full backend and frontend suites, Compose smoke test.

## TDD
On for T2–T4 (JUnit + Testcontainers, Vitest + MSW). T1, T5, T6: structural or smoke checks; no meaningful RED.

## Delivery
Strategy: ask-on-risk. Work-unit commit per task on `feature/sprint-0-base`; merge to `dev` in T7. Review boundary starts at `dev` (5794e96).

## Progress
- 2026-10-14: plan committed (0572fa6). T1 committed (8882a2f); native review (lineage review-d14a71287dcae653, medium, slice budget) approved and acknowledged. Advisories fixed in plan and README: mvnw exec bit (git update-index --chmod=+x), public TestcontainersConfiguration, README cd chain. Next: T2.
- 2026-10-14: T2 committed (114cb2e, mvnw mode 100755). Native review high risk, 4 lenses (lineage review-7716b3994c8016a5) approved and acknowledged. Follow-up suggestions (non-blocking): wrapper checksum, pom metadata, redundant `spring.flyway.enabled`, assert actuator probes, `contextLoads` without assertion. Next: T3.
- 2026-10-14: T3 (7a2f39d) + T4 (386e614) reviewed together (medium, slice budget; lineage review-55195b68571762df) approved and acknowledged. Follow-ups: apiGet should wrap non-JSON bodies in ApiError (WARNING), app.version default, version assertion tied to pom, prove MSW unhandled guard. Remote: origin = MiguelGironAltamirano/taller-web; main, dev, feature/sprint-0-base pushed. Review boundary: 386e614. Next: T5.
- 2026-10-14: T5 (453df47) + T6 (f2026db) pushed; review high, 4 lenses (review-78bf9f9dc9b800c8) approved and acknowledged with WARNINGs (nginx static upstream, MySQL healthcheck init window, CI permissions, T6 marked done before a real CI run). T6 reopened; T6.1 added. GCM credentials for both GitHub accounts re-created by the owner; per-folder default usernames configured. Review boundary: f2026db.
- 2026-10-14: T6.1 done: nginx resolver 127.0.0.11 + variable upstream; MySQL healthcheck = TCP query as app user with start_period 30s; CI permissions contents:read + concurrency. Evidence: compose config/up/curl/restart backend/curl/SPA 200/down all pass. Caveat: restart did not prove an IP change, so DNS re-resolution itself is untested.
