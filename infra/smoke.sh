#!/usr/bin/env bash
# Smoke test for the local Docker Compose stack.
#
# Builds and starts the stack with demo data (db/seed-dev), waits until every service is healthy,
# checks the API and the SPA through nginx, logs in with a demo user and tears everything down.
# It never reads infra/.env: it generates a throwaway env file so runs are reproducible.
#
# Environment overrides:
#   SMOKE_PROJECT       Compose project name          (default: taller-web-smoke)
#   FRONTEND_PORT       Host port published by nginx  (default: 8089)
#   SMOKE_KEEP_STACK=1  Leave the stack running (e.g. to run Playwright afterwards); tear down later with
#                       docker compose -p <project> -f infra/docker-compose.yml down -v
#   SMOKE_SKIP_BUILD=1  Reuse already built images
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
PROJECT="${SMOKE_PROJECT:-taller-web-smoke}"
PORT="${FRONTEND_PORT:-8089}"
BASE_URL="http://localhost:$PORT"
DEMO_EMAIL="estudiante@unmsm.edu.pe"
DEMO_PASSWORD='Demo12345!'

ENV_FILE="$(mktemp)"
WORK_DIR="$(mktemp -d)"

compose() {
  docker compose -p "$PROJECT" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

cleanup() {
  local status=$?
  if [[ $status -ne 0 ]]; then
    echo "--- smoke failed; last backend logs ---" >&2
    compose logs --no-color --tail 60 backend >&2 || true
  fi
  if [[ "${SMOKE_KEEP_STACK:-0}" == "1" && $status -eq 0 ]]; then
    echo "Stack '$PROJECT' left running at $BASE_URL"
  else
    compose down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  rm -rf "$ENV_FILE" "$WORK_DIR"
  exit $status
}
trap cleanup EXIT

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
  else
    od -An -N32 -tx1 /dev/urandom | tr -d ' \n'
  fi
}

cat >"$ENV_FILE" <<EOF
MYSQL_DATABASE=servicios
MYSQL_USER=servicios
MYSQL_PASSWORD=$(random_secret)
MYSQL_ROOT_PASSWORD=$(random_secret)
FRONTEND_PORT=$PORT
APP_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed-dev
APP_JWT_SECRET=$(random_secret)
APP_EVIDENCES_STORAGE_PATH=/app/evidences
APP_AUTH_REFRESH_COOKIE_SECURE=true
APP_CORS_ALLOWED_ORIGINS=
EOF

step() { printf '==> %s\n' "$*"; }
fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }

# Retries a request until it answers 200, so a slow nginx/DNS warm-up does not fail the run.
http_get() {
  local url=$1 out=$2 code=""
  for _ in $(seq 1 15); do
    code=$(curl -s -o "$out" -w '%{http_code}' "$url" || true)
    [[ "$code" == "200" ]] && return 0
    sleep 2
  done
  fail "GET $url answered ${code:-no response}"
}

if [[ "${SMOKE_SKIP_BUILD:-0}" == "1" ]]; then
  step "Starting stack '$PROJECT' (no build)"
  compose up -d --wait --wait-timeout 300
else
  step "Building and starting stack '$PROJECT'"
  compose up -d --build --wait --wait-timeout 300
fi
compose ps --format 'table {{.Service}}\t{{.Status}}'

step "GET /api/v1/system/info through nginx"
http_get "$BASE_URL/api/v1/system/info" "$WORK_DIR/info.json"
grep -q '"name":"servicios"' "$WORK_DIR/info.json" || fail "unexpected system info: $(cat "$WORK_DIR/info.json")"
echo "    $(cat "$WORK_DIR/info.json")"

step "GET / serves the SPA"
http_get "$BASE_URL/" "$WORK_DIR/index.html"
grep -q '<div id="root">' "$WORK_DIR/index.html" || fail "index.html does not contain the SPA root"
echo "    index.html OK"

step "POST /api/v1/auth/login as $DEMO_EMAIL"
code=$(curl -s -o "$WORK_DIR/login.json" -D "$WORK_DIR/login.headers" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\"}" \
  "$BASE_URL/api/v1/auth/login")
[[ "$code" == "200" ]] || fail "login answered $code: $(cat "$WORK_DIR/login.json")"
grep -q '"accessToken":"' "$WORK_DIR/login.json" || fail "login response has no accessToken"
grep -qi '^set-cookie: refresh_token=' "$WORK_DIR/login.headers" || fail "login did not set the refresh cookie"
echo "    login OK (access token + refresh cookie)"

step "Smoke test passed"
