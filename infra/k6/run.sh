#!/usr/bin/env bash
# Load tests with k6 (docs/calidad/plan-de-pruebas.md §6, results in docs/calidad/informe-pruebas-carga.md).
#
# Usage: bash infra/k6/run.sh <smoke|load|stress|spike> [extra k6 arguments]
#
# 1. Starts (or reuses) a dedicated stack with demo data, apart from the development one, so a load run never
#    touches it. It is the production build of every service: nginx, the backend jar and MySQL.
# 2. Loads infra/k6/seed-load.sql once (20 000 extra requests), so the list runs against a realistic volume.
# 3. Runs k6 from its official image inside the Compose network: requests go through nginx exactly as a browser's
#    do, and the host network does not distort the timings.
# 4. Samples CPU and memory of each container every 5 s while k6 runs.
# Raw results (k6 summary JSON and the resource CSV) stay in infra/k6/results/ (git-ignored).
#
# Environment overrides:
#   K6_PROJECT  Compose project of the load stack (default: taller-web-load)
#   K6_PORT     Host port of its nginx, to look at it in a browser (default: 8095)
#   K6_DOWN=1   Tear the stack down with its volumes afterwards
set -euo pipefail

SCRIPT="${1:?usage: bash infra/k6/run.sh <smoke|load|stress|spike> [k6 args]}"
shift
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT="${K6_PROJECT:-taller-web-load}"
PORT="${K6_PORT:-8095}"
K6_IMAGE="grafana/k6:1.4.2"
RESULTS="$SCRIPT_DIR/results"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$RESULTS"

[[ -f "$SCRIPT_DIR/$SCRIPT.js" ]] || { echo "Unknown scenario: $SCRIPT"; exit 2; }
ENV_FILE="$INFRA_DIR/.env"
[[ -f "$ENV_FILE" ]] || ENV_FILE="$INFRA_DIR/.env.example"
# Only the MySQL credentials are read here, and not exported: Compose reads the file itself, and exporting it from
# Git Bash would turn paths such as /app/evidences into Windows paths on their way to Docker.
env_value() { grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2-; }
MYSQL_USER="$(env_value MYSQL_USER)"
MYSQL_PASSWORD="$(env_value MYSQL_PASSWORD)"
MYSQL_DATABASE="$(env_value MYSQL_DATABASE)"

compose() {
  FRONTEND_PORT="$PORT" \
  APP_FLYWAY_LOCATIONS="classpath:db/migration,classpath:db/seed-dev,classpath:db/seed-demo" \
  APP_AUTH_REFRESH_COOKIE_SECURE=false \
    docker compose -p "$PROJECT" -f "$INFRA_DIR/docker-compose.yml" --env-file "$ENV_FILE" "$@"
}

echo "==> Stack '$PROJECT' on http://localhost:$PORT"
compose up -d --build --wait >/dev/null

# MySQL warns that a password on the command line is insecure: expected here (local load stack); other errors show.
mysql_q() {
  compose exec -T mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -N "$@"     2> >(grep -v "Using a password on the command line" >&2)
}
LOADED="$(mysql_q -e "select count(*) from service_requests where code like 'SOL-2025-%'")"
if [[ "$LOADED" == "0" ]]; then
  echo "==> Loading load-test data (infra/k6/seed-load.sql)"
  mysql_q < "$SCRIPT_DIR/seed-load.sql"
fi
echo "==> Requests in the database: $(mysql_q -e "select count(*) from service_requests")"

STATS="$RESULTS/$SCRIPT-resources-$STAMP.csv"
echo "time,container,cpu,memory" > "$STATS"
(
  while true; do
    docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}}" \
      "$PROJECT-frontend-1" "$PROJECT-backend-1" "$PROJECT-mysql-1" 2>/dev/null \
      | sed "s|^|$(date -u +%H:%M:%S),|" >> "$STATS"
    sleep 5
  done
) &
SAMPLER=$!
cleanup() {
  kill "$SAMPLER" 2>/dev/null || true
  if [[ "${K6_DOWN:-0}" == "1" ]]; then compose down -v --remove-orphans >/dev/null 2>&1 || true; fi
}
trap cleanup EXIT

# Git Bash on Windows rewrites "/scripts"-like arguments; a Windows path for the volume avoids it.
HOST_DIR="$SCRIPT_DIR"
command -v cygpath >/dev/null 2>&1 && HOST_DIR="$(cygpath -w "$SCRIPT_DIR")"

echo "==> k6 $SCRIPT ($K6_IMAGE)"
set +e
MSYS_NO_PATHCONV=1 docker run --rm --network "${PROJECT}_default" \
  -v "$HOST_DIR:/scripts" -e BASE_URL=http://frontend \
  "$K6_IMAGE" run "/scripts/$SCRIPT.js" "$@"
STATUS=$?
set -e
echo "==> Resources sampled in $STATS"
exit $STATUS
