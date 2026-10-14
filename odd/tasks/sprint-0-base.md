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
- [ ] T5 — Container images and Docker Compose stack — route: delegated writer; independent verification
- [ ] T6 — CI pipeline — route: delegated writer; independent verification
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
