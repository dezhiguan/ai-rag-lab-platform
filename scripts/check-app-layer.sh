#!/usr/bin/env bash
# 应用入口层健康检查（轻量服务器）
#
# 用法：
#   ./scripts/check-app-layer.sh
#   ./scripts/check-app-layer.sh <ECS_PRIVATE_IP>   # 额外检查到数据层端口

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ECS_HOST="${1:-}"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/deploy/app-layer/.env.app}"

APP_DEPLOY_DIR="${APP_DEPLOY_DIR:-/opt/rag-lab/app}"
NGINX_STATIC_DIR="${NGINX_STATIC_DIR:-/var/www/rag-lab/frontend}"
SERVER_PORT="${SERVER_PORT:-8080}"
HEALTH_PATH="/api/system/health"

pass=0
warn=0
fail=0

pass_msg() { echo "[PASS] $1"; pass=$((pass + 1)); }
warn_msg() { echo "[WARN] $1"; warn=$((warn + 1)); }
fail_msg() { echo "[FAIL] $1"; fail=$((pass + 1)); }

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
  SERVER_PORT="${SERVER_PORT:-8080}"
  ECS_HOST="${ECS_HOST:-${POSTGRES_HOST:-}}"
fi

tcp_open() {
  local host="$1" port="$2"
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 3 "$host" "$port" 2>/dev/null
    return $?
  fi
  (echo >/dev/tcp/"$host"/"$port") 2>/dev/null
}

java_major() {
  java -version 2>&1 | awk -F '[."-]' '/version/ {
    v = $2
    if (v == "1") { print $3; exit }
    print v
  }'
}

echo "=== RAG Lab App Layer Health Check ==="
echo "APP_DEPLOY_DIR=$APP_DEPLOY_DIR"
echo "NGINX_STATIC_DIR=$NGINX_STATIC_DIR"
echo "SERVER_PORT=$SERVER_PORT"
[[ -n "$ECS_HOST" && "$ECS_HOST" != *"<"* ]] && echo "ECS_HOST=$ECS_HOST"
echo "---"

# Java
if command -v java >/dev/null 2>&1; then
  major="$(java_major)"
  if [[ -n "$major" && "$major" -ge 17 ]]; then
    pass_msg "Java $(java -version 2>&1 | head -n1) (>= 17)"
  else
    fail_msg "Java version too old (need >= 17)"
  fi
else
  fail_msg "Java not found"
fi

# Nginx
if command -v nginx >/dev/null 2>&1; then
  pass_msg "Nginx installed: $(nginx -v 2>&1)"
else
  fail_msg "Nginx not installed"
fi

# Backend port
if tcp_open 127.0.0.1 "$SERVER_PORT"; then
  pass_msg "Backend port ${SERVER_PORT} listening on 127.0.0.1"
else
  fail_msg "Backend port ${SERVER_PORT} not listening — run deploy-backend-app.sh"
fi

# Frontend static dir
if [[ -d "$NGINX_STATIC_DIR" && -f "$NGINX_STATIC_DIR/index.html" ]]; then
  pass_msg "Frontend static dir OK: $NGINX_STATIC_DIR/index.html"
elif [[ -d "$NGINX_STATIC_DIR" ]]; then
  warn_msg "Nginx static dir exists but index.html missing: $NGINX_STATIC_DIR"
else
  fail_msg "Frontend static dir missing: $NGINX_STATIC_DIR — run deploy-frontend-app.sh"
fi

# Health API (direct to backend)
if command -v curl >/dev/null 2>&1; then
  direct_url="http://127.0.0.1:${SERVER_PORT}${HEALTH_PATH}"
  code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$direct_url" 2>/dev/null || echo "000")
  if [[ "$code" == "200" ]]; then
    pass_msg "Backend health OK: $direct_url"
  else
    fail_msg "Backend health failed: $direct_url (http_code=$code)"
  fi

  # Health via Nginx (port 80)
  nginx_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 \
    "http://127.0.0.1${HEALTH_PATH}" 2>/dev/null || echo "000")
  if [[ "$nginx_code" == "200" ]]; then
    pass_msg "Nginx proxy health OK: http://127.0.0.1${HEALTH_PATH}"
  else
    warn_msg "Nginx proxy health not OK (http_code=$nginx_code) — configure nginx-rag.conf.example"
  fi
else
  warn_msg "curl not installed — skipping HTTP health checks"
fi

# Deploy dir / jar
if [[ -f "$APP_DEPLOY_DIR/backend.jar" ]]; then
  pass_msg "Deployed jar exists: $APP_DEPLOY_DIR/backend.jar"
else
  warn_msg "Deployed jar not found: $APP_DEPLOY_DIR/backend.jar"
fi

if [[ -f "$ROOT_DIR/deploy/app-layer/.env.app" ]]; then
  pass_msg "deploy/app-layer/.env.app exists"
elif [[ -f "$ROOT_DIR/.env.prod" ]]; then
  warn_msg "Using .env.prod only — recommend deploy/app-layer/.env.app"
else
  warn_msg "No deploy/app-layer/.env.app — copy from .env.app.example"
fi

# ECS data layer ports (optional)
if [[ -n "$ECS_HOST" && "$ECS_HOST" != *"<"* && "$ECS_HOST" != *"ECS_PRIVATE"* ]]; then
  echo "---"
  echo "Data layer checks (ECS $ECS_HOST):"
  for port_label in "5432:PostgreSQL" "9200:Elasticsearch" "6379:Redis"; do
    port="${port_label%%:*}"
    label="${port_label#*:}"
    if tcp_open "$ECS_HOST" "$port"; then
      pass_msg "${label} port ${port} reachable on ${ECS_HOST}"
    else
      fail_msg "${label} port ${port} not reachable on ${ECS_HOST}"
    fi
  done
  if [[ -x "$ROOT_DIR/scripts/check-data-layer.sh" ]]; then
    echo "(Run ./scripts/check-data-layer.sh $ECS_HOST for detailed checks)"
  fi
elif [[ -n "$ECS_HOST" ]]; then
  warn_msg "ECS host looks like placeholder — pass real <ECS_PRIVATE_IP> for data layer checks"
else
  warn_msg "No ECS_PRIVATE_IP — run: ./scripts/check-app-layer.sh <ECS_PRIVATE_IP>"
fi

echo "---"
echo "Summary: PASS=$pass WARN=$warn FAIL=$fail"

if [[ "$fail" -gt 0 ]]; then
  echo "Result: NOT READY"
  exit 1
fi
if [[ "$warn" -gt 0 ]]; then
  echo "Result: READY WITH WARNINGS"
  exit 0
fi
echo "Result: READY"
exit 0
