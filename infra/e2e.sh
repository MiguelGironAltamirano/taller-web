#!/usr/bin/env bash
# End-to-end tests against the real Docker Compose stack (no mocks).
#
# 1. Builds and starts the stack with demo data and checks it through infra/smoke.sh (which waits until every
#    service is healthy and smoke-tests the API and the SPA).
# 2. Runs the Playwright suite (frontend/e2e) against it.
# 3. Always tears the stack down with its volumes, even when a step fails or the run is interrupted.
#
# Usage: bash infra/e2e.sh [extra Playwright arguments, e.g. a spec name or --headed]
#
# Environment overrides:
#   E2E_PROJECT       Compose project name          (default: taller-web-e2e)
#   E2E_PORT          Host port published by nginx  (default: 8090)
#   E2E_KEEP_STACK=1  Leave the stack running afterwards (tear down with the command printed at the end)
#   E2E_SKIP_BUILD=1  Reuse already built images
#
# Requirements: Docker with Compose v2, curl, and the frontend toolchain (pnpm through mise, or pnpm on PATH).
# Playwright's Chromium is installed on first use. The HTML report and traces of failures stay in
# frontend/playwright-report and frontend/test-results (both git-ignored).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT="${E2E_PROJECT:-taller-web-e2e}"
PORT="${E2E_PORT:-8090}"
BASE_URL="http://localhost:$PORT"

step() { printf '==> %s\n' "$*"; }

teardown() {
  local status=$?
  if [[ "${E2E_KEEP_STACK:-0}" == "1" ]]; then
    echo "Stack '$PROJECT' left running at $BASE_URL"
    echo "Tear it down with: docker compose -p $PROJECT -f infra/docker-compose.yml down -v"
  else
    step "Tearing down stack '$PROJECT'"
    # The variables are only needed to parse the file; down never uses their values.
    MYSQL_DATABASE=x MYSQL_USER=x MYSQL_PASSWORD=x MYSQL_ROOT_PASSWORD=x APP_JWT_SECRET=x \
      docker compose -p "$PROJECT" -f "$SCRIPT_DIR/docker-compose.yml" down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  exit $status
}
trap teardown EXIT
trap 'exit 130' INT TERM

pnpm_cmd() {
  if command -v mise >/dev/null 2>&1; then
    (cd "$ROOT_DIR/frontend" && mise exec -- pnpm "$@")
  else
    (cd "$ROOT_DIR/frontend" && pnpm "$@")
  fi
}

step "Installing frontend dependencies and Playwright Chromium"
pnpm_cmd install --frozen-lockfile >/dev/null
pnpm_cmd exec playwright install chromium >/dev/null

step "Starting stack '$PROJECT' on $BASE_URL with demo data"
SMOKE_PROJECT="$PROJECT" FRONTEND_PORT="$PORT" SMOKE_KEEP_STACK=1 SMOKE_SKIP_BUILD="${E2E_SKIP_BUILD:-0}" \
  bash "$SCRIPT_DIR/smoke.sh"

step "Running Playwright against $BASE_URL"
E2E_BASE_URL="$BASE_URL" pnpm_cmd e2e "$@"

step "End-to-end tests passed"
