#!/usr/bin/env bash
# 应用入口层：部署并启动 RAG 后端 jar（轻量服务器）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

APP_DEPLOY_DIR="${APP_DEPLOY_DIR:-/opt/rag-lab/app}"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/deploy/app-layer/.env.app}"
SRC_JAR="${SRC_JAR:-$ROOT_DIR/backend/target/backend-0.0.1-SNAPSHOT.jar}"
DEST_JAR="$APP_DEPLOY_DIR/backend.jar"
PID_FILE="$APP_DEPLOY_DIR/rag-backend.pid"
LOG_DIR="$APP_DEPLOY_DIR/logs"
LOG_FILE="$LOG_DIR/backend.log"
HEALTH_URL="http://127.0.0.1:${SERVER_PORT:-8080}/api/system/health"

echo "=== Deploy RAG Backend (App Layer) ==="
echo "ROOT_DIR=$ROOT_DIR"
echo "APP_DEPLOY_DIR=$APP_DEPLOY_DIR"
echo "ENV_FILE=$ENV_FILE"

if [[ ! -f "$SRC_JAR" ]]; then
  echo "Error: jar not found: $SRC_JAR"
  echo "Run: cd backend && mvn -DskipTests clean package"
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Error: env file not found: $ENV_FILE"
  echo "Run: cp deploy/app-layer/.env.app.example deploy/app-layer/.env.app"
  exit 1
fi

mkdir -p "$APP_DEPLOY_DIR" "$LOG_DIR"
UPLOAD_DIR_LINE="$(grep -E '^RAG_STORAGE_PATH=' "$ENV_FILE" || true)"
if [[ -n "$UPLOAD_DIR_LINE" ]]; then
  # shellcheck disable=SC1090
  eval "export $UPLOAD_DIR_LINE"
  if [[ -n "${RAG_STORAGE_PATH:-}" ]]; then
    mkdir -p "$RAG_STORAGE_PATH"
  fi
fi

echo "Copying jar -> $DEST_JAR"
cp -f "$SRC_JAR" "$DEST_JAR"

stop_old_process() {
  if [[ -f "$PID_FILE" ]]; then
    old_pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    if [[ -n "$old_pid" ]] && kill -0 "$old_pid" 2>/dev/null; then
      echo "Stopping previous backend (pid=$old_pid)..."
      kill "$old_pid" 2>/dev/null || true
      for _ in 1 2 3 4 5; do
        kill -0 "$old_pid" 2>/dev/null || break
        sleep 1
      done
      if kill -0 "$old_pid" 2>/dev/null; then
        echo "Force stopping pid=$old_pid"
        kill -9 "$old_pid" 2>/dev/null || true
      fi
    fi
    rm -f "$PID_FILE"
  fi
}

stop_old_process

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
SERVER_PORT="${SERVER_PORT:-8080}"
HEALTH_URL="http://127.0.0.1:${SERVER_PORT}/api/system/health"

echo "Starting backend (profile=$SPRING_PROFILES_ACTIVE, port=$SERVER_PORT)..."
nohup java ${JAVA_OPTS:-} -jar "$DEST_JAR" >>"$LOG_FILE" 2>&1 &
new_pid=$!
echo "$new_pid" >"$PID_FILE"
echo "Started pid=$new_pid, log=$LOG_FILE"

echo "Waiting for health check..."
for i in $(seq 1 30); do
  if curl -sf "$HEALTH_URL" >/dev/null 2>&1; then
    echo "Health check OK: $HEALTH_URL"
    curl -s "$HEALTH_URL" | head -c 500
    echo ""
    echo "Done. Via Nginx: http://<LIGHT_SERVER_PUBLIC_IP>/api/system/health"
    exit 0
  fi
  sleep 2
done

echo "Warning: health check not ready yet: $HEALTH_URL"
echo "Check log: tail -f $LOG_FILE"
exit 0
