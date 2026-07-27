#!/usr/bin/env bash

set -Eeuo pipefail

APP_CONTAINER="${APP_CONTAINER:-matoapp-backend}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-mato-mysql}"
TUNNEL_CONTAINER="${TUNNEL_CONTAINER:-mato-quick-tunnel}"
SERVER_PORT="${SERVER_PORT:-18080}"
ASSET_PATH="/app/build/v2-map-assets"
BACKUP_ROOT="${MATO_BACKUP_ROOT:-${HOME}/mato-backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/${STAMP}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command is missing: $1" >&2
    exit 1
  }
}

require_container() {
  docker inspect "$1" >/dev/null 2>&1 || {
    echo "Required container is missing: $1" >&2
    exit 1
  }
}

require_command docker
require_command curl
require_command gzip
docker compose version >/dev/null
require_container "$APP_CONTAINER"
require_container "$MYSQL_CONTAINER"

COMPOSE_DIR="$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project.working_dir" }}' "$APP_CONTAINER")"
if [[ -z "$COMPOSE_DIR" || ! -d "$COMPOSE_DIR" ]]; then
  echo "Could not resolve the existing Compose working directory." >&2
  exit 1
fi

cd "$COMPOSE_DIR"
docker compose config --services | grep -qx app || {
  echo "The existing Compose project does not contain the app service." >&2
  exit 1
}

mkdir -p "$BACKUP_DIR/v2-map-assets"

echo "[1/6] Backing up uploaded map assets"
if docker exec "$APP_CONTAINER" sh -c "test -d '$ASSET_PATH'"; then
  docker cp "${APP_CONTAINER}:${ASSET_PATH}/." "$BACKUP_DIR/v2-map-assets/"
fi

echo "[2/6] Creating and validating a MySQL dump"
docker exec "$MYSQL_CONTAINER" sh -ec '
  test -n "$MYSQL_DATABASE"
  exec mysqldump --single-transaction --routines --triggers --events \
    -uroot -p"$MYSQL_ROOT_PASSWORD" --databases "$MYSQL_DATABASE"
' | gzip -c > "$BACKUP_DIR/database.sql.gz"
test -s "$BACKUP_DIR/database.sql.gz"
gzip -t "$BACKUP_DIR/database.sql.gz"

echo "[3/6] Updating the backend source"
git fetch origin develop
git merge --ff-only origin/develop

echo "[4/6] Building and replacing only the backend app container"
docker compose up -d --build app
docker exec "$APP_CONTAINER" mkdir -p "$ASSET_PATH"
docker cp "$BACKUP_DIR/v2-map-assets/." "${APP_CONTAINER}:${ASSET_PATH}/"

echo "[5/6] Waiting for backend health"
healthy=""
for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" >/dev/null; then
    healthy="yes"
    break
  fi
  sleep 1
done
if [[ "$healthy" != "yes" ]]; then
  docker logs --tail 120 "$APP_CONTAINER" >&2 || true
  echo "Backend did not become healthy. Existing data and backup are untouched at $BACKUP_DIR" >&2
  exit 1
fi

echo "[6/6] Starting a temporary HTTPS/WSS Cloudflare tunnel"
docker rm -f "$TUNNEL_CONTAINER" >/dev/null 2>&1 || true
docker run -d \
  --name "$TUNNEL_CONTAINER" \
  --restart unless-stopped \
  --network host \
  cloudflare/cloudflared:latest \
  tunnel --no-autoupdate --url "http://127.0.0.1:${SERVER_PORT}" >/dev/null

tunnel_url=""
for _ in $(seq 1 60); do
  tunnel_url="$(docker logs "$TUNNEL_CONTAINER" 2>&1 | grep -Eo 'https://[-a-z0-9]+\.trycloudflare\.com' | tail -n 1 || true)"
  if [[ -n "$tunnel_url" ]]; then
    break
  fi
  sleep 1
done

if [[ -z "$tunnel_url" ]]; then
  docker logs --tail 120 "$TUNNEL_CONTAINER" >&2 || true
  echo "Backend is healthy, but the temporary tunnel URL was not created." >&2
  exit 1
fi

echo
echo "BACKUP_DIR=$BACKUP_DIR"
echo "MATO_TUNNEL_URL=$tunnel_url"
