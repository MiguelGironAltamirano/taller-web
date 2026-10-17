# Feature: miguel-auth-hardening

## Objective
Close the pending items of weeks 8-10 of the platform and authentication module (owner: Miguel): area claim with a reusable access policy, security headers, configurable CORS, login attempt limit, and the mandatory authentication tests still missing from `plan/miguel.md`.

## Sources
- `plan/miguel.md` (scope and mandatory tests), `docs/api/contrato-v1.md`, `odd/tasks/auth-platform.md`.

## Tasks
- [x] 1 - Area claim and reusable access policy. `AuthenticatedUser.areaId`, `area_id` token claim, `areaId` in `GET /me` and login/refresh user; pure-Java `AccessPolicy` (`isOwner`, `isSupervisorOfArea`, `isAssignedTechnician`, `isAdministrator`, `canViewRequest`); contract section "Reglas de acceso reutilizables". Evidence: AccessPolicyTest (9 tests, RED = compilation failure before implementation), SecurityFoundationTests and AuthApiTests area assertions, ArchitectureTest green.
- [x] 2 - Security headers and configurable CORS. `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`; `app.cors.allowed-origins` (env `APP_CORS_ALLOWED_ORIGINS`, default empty = no cross-origin). Evidence: SecurityHeadersAndCorsTests 3/3 and CorsDisabledByDefaultTests 1/1 (RED observed: 3 failures before implementation).
- [x] 3 - Login attempt limit. In-memory `LoginAttemptLimiter` keyed by lowercased email, 5 failures in 15 minutes, `429` problem+json with `Retry-After`, reset on success, injectable `Clock`. Evidence: LoginAttemptLimitTests 4/4 (RED observed: 3 failures before implementation).
- [x] 4 - Frontend security headers in `frontend/nginx.conf` (SPA location): nosniff, frame deny, no-referrer and a restrictive Content-Security-Policy. Evidence: configuration review only; no runnable test for nginx in this repository.
- [x] 5 - Missing mandatory tests: expired access token gives 401, role without permission gives 403 (test-only endpoint), no demo users with default Flyway locations. Evidence: AuthorizationFailureTests 3/3, SeedlessStartupTests 1/1. These characterize behavior that already worked, so no RED was possible.

## Verification
- Backend `./mvnw verify`: 51 tests, 0 failures, 0 errors.
- Frontend: 24 tests pass; lint and typecheck pass.

## Deviations
- The frontend `/me` mock now returns `areaId`; `types.ts` was not changed because `UserSummary` already declares `areaId`.
- `AuthenticatedUser` keeps a four-argument constructor (area `null`) so existing callers and tests keep compiling.
- Security headers in nginx apply to the SPA location only; `/api/` responses carry the backend headers.

## Follow-ups
- The login limiter is per instance; move it to a shared store if the backend is ever scaled horizontally.
- Verify the nginx headers against the running container in staging (CSP may need adjusting if inline assets are added).
