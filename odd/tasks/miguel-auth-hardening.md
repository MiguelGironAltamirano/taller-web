# Feature: miguel-auth-hardening

## Objective
Close the pending items of weeks 8-10 of the platform and authentication module (owner: Miguel): area claim with a reusable access policy, security headers, configurable CORS, login attempt limit, and the mandatory authentication tests still missing from `plan/miguel.md`.

## Sources
- `plan/miguel.md` (scope and mandatory tests), `docs/api/contrato-v1.md`, `odd/tasks/auth-platform.md`.

## Tasks
- [x] 1 - Area claim and reusable access policy. `AuthenticatedUser.areaId`, `area_id` token claim, `areaId` in `GET /me` and login/refresh user; pure-Java `AccessPolicy` (`isOwner`, `isSupervisorOfArea`, `isAssignedTechnician`, `isAdministrator`, `canViewRequest`); documented in `AccessPolicy` Javadoc (the contract has no such section). Evidence: AccessPolicyTest (9 tests, RED = compilation failure before implementation), SecurityFoundationTests and AuthApiTests area assertions, ArchitectureTest green.
- [x] 2 - Security headers and configurable CORS. `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`; `app.cors.allowed-origins` (env `APP_CORS_ALLOWED_ORIGINS`, default empty = no cross-origin). Evidence: SecurityHeadersAndCorsTests 3/3 and CorsDisabledByDefaultTests 1/1 (RED observed: 3 failures before implementation).
- [x] 3 - Login attempt limit. In-memory `LoginAttemptLimiter` keyed by lowercased email (see follow-up fixes for the per-client key), `429` problem+json with `Retry-After`, reset on success, injectable `Clock`. Evidence: LoginAttemptLimitTests 4/4 (RED observed: 3 failures before implementation).
- [x] 4 - Frontend security headers in `frontend/nginx.conf` (SPA location): nosniff, frame deny, no-referrer and a restrictive Content-Security-Policy. Evidence: configuration review only; no runnable test for nginx in this repository.
- [x] 5 - Missing mandatory tests: expired access token gives 401, role without permission gives 403 (test-only endpoint), no demo users with default Flyway locations. Evidence: AuthorizationFailureTests 3/3, SeedlessStartupTests 1/1. These characterize behavior that already worked, so no RED was possible.

## Verification
- Backend `./mvnw verify`: 51 tests (58 after the follow-up fixes), 0 failures, 0 errors.
- Frontend: 24 tests pass; lint and typecheck pass.

## Deviations
- The frontend `/me` mock now returns `areaId`; `types.ts` was not changed because `UserSummary` already declares `areaId`.
- `AuthenticatedUser` keeps a four-argument constructor (area `null`) so existing callers and tests keep compiling.
- Security headers in nginx apply to the SPA location only; `/api/` responses carry the backend headers.

## Follow-ups
- The login limiter is per instance; move it to a shared store if the backend is ever scaled horizontally.
- Verify the nginx headers against the running container in staging (CSP may need adjusting if inline assets are added).

## Follow-up fixes
Code-review warnings addressed on `feature/miguel-login-limiter-fixes`:
1. Lockout DoS: failures are keyed by email + client IP (`getRemoteAddr()`, plain String into the use case) with a higher per-email ceiling (5 / 20 per 15 minutes). Evidence: `LoginAttemptLimiterTest` (RED = compilation failure before implementation).
2. Check-then-act race: attempts are counted atomically when acquired, before verifying the password. Evidence: `LoginAttemptLimitTests.concurrentWrongAttemptsCannotExceedTheLimit` (20 concurrent attempts give 5x401 and 15x429; RED observed before the fix).
3. Unbounded map: tracked keys capped (default 10000), lazy expiry on access, oldest entries dropped. Evidence: `LoginAttemptLimiterTest.trackedKeysAreCappedDroppingTheOldest`.
4. Configurable policy: `app.auth.login-limit.*` (`LoginLimitProperties`), documented in the login row of `docs/api/contrato-v1.md`.
5. CORS wildcard with credentials: `*` patterns in `app.cors.allowed-origins` fail startup with `IllegalStateException`. Evidence: `CorsOriginsValidationTest` (RED observed: no exception thrown before the fix).
6. This document: removed the reference to a non-existent contract section.
Verification: backend `./mvnw verify` 58 tests, 0 failures.
