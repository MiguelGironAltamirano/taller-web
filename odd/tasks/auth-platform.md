# Feature: auth-platform

## Objective
Deliver the platform, security and authentication module (owner: Miguel): initial schema and demo users, JWT security foundation, register/login/refresh/logout API, API contract with frontend types and mock API, and the frontend shell with authentication.

## Sources
- Sprint 1 plan: `docs/plans/2026-10-16-sprint-1-mvp.md` (Tasks A1, A2, A3; streams B1, B2).
- Team plan: `plan/README.md`, `plan/miguel.md`. API contract: `docs/api/contrato-v1.md`.

## Scope
In: A1, A2, B1 (without catalogs, owned by Sevan), A3, B2. Out: requests, workflow, catalogs endpoint.

## Tasks
- [x] A1 — Initial schema, catalog seed, dev users — delegated writer (backend), TDD. Evidence: SchemaMigrationTests 4/4. Deviation: `last_value` is reserved in MySQL 8.4, quoted with backticks (C1 must map it as "`last_value`").
- [x] A2 — JWT security foundation and `/api/v1/me` — delegated writer (backend), TDD. Evidence: SecurityFoundationTests 4/4, ApiConventionsTests 4/4.
- [x] B1 — Register, login, refresh, logout API — delegated writer (backend), TDD. Evidence: AuthApiTests 12/12 (RED 12 failing before implementation). Refresh tokens stored as SHA-256, rotation with conditional update, constant-time-ish login for unknown emails. `app.auth.refresh-cookie-secure` configurable (default true).
- [x] A3 — API contract, frontend types, mock API — delegated writer (frontend), TDD. Evidence: handlers tests 5/5.
- [x] B2 — Frontend shell, routing by role, login/register, session — delegated writer (frontend), TDD. Evidence: 24 frontend tests; lint, typecheck, build pass. Session in memory store, single in-flight refresh, MSW worker with `VITE_USE_MOCKS=true`.
- [x] D — Integration: PR #4 to dev, CI, merge — orchestrator. Native review could not start (lens_context_budget_exceeded: 78 files, ~3.5k lines incl. lockfile and generated MSW worker); the owner chose to merge without native review, relying on 52 automated tests, the Docker smoke test and CI.

## Integration evidence (2026-10-17, branch feature/auth-platform)
- Agent commits cherry-picked with neutral messages; contract renamed to `docs/api/contrato-v1.md`, migration to `V2__initial_schema.sql`.
- Backend `./mvnw verify`: 28 tests, 0 failures. Frontend: 24 tests; lint, typecheck, build pass.
- Docker smoke via nginx: system/info 200; login 200 (SOLICITANTE, expiresIn 900); refresh cookie attributes per contract; /me 200 with token and 401 without; register 201 (UNMSM) and 400 (non-UNMSM); SPA /login 200.

## Follow-ups
- C1 must map `request_code_sequences.last_value` with backticks.
- Pass `APP_AUTH_REFRESH_COOKIE_SECURE` through compose if a non-localhost HTTP environment is ever needed.
