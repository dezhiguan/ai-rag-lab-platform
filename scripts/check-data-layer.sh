#!/usr/bin/env bash
# 数据与检索层健康检查（PostgreSQL / Elasticsearch / Redis）
#
# 用法：
#   ./scripts/check-data-layer.sh                    # 检查 127.0.0.1（ECS 本机）
#   ./scripts/check-data-layer.sh <ECS_PRIVATE_IP>     # 从轻量服务器检查内网
#
# 可选环境变量（覆盖端口与凭据）：
#   POSTGRES_PORT POSTGRES_USER POSTGRES_DB POSTGRES_PASSWORD
#   ELASTICSEARCH_PORT REDIS_PORT REDIS_PASSWORD

set -uo pipefail

HOST="${1:-127.0.0.1}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-rag_user}"
POSTGRES_DB="${POSTGRES_DB:-rag_lab}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"
ELASTICSEARCH_PORT="${ELASTICSEARCH_PORT:-9200}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

pass=0
warn=0
fail=0

pass_msg() { echo "[PASS] $1"; pass=$((pass + 1)); }
warn_msg() { echo "[WARN] $1"; warn=$((warn + 1)); }
fail_msg() { echo "[FAIL] $1"; fail=$((fail + 1)); }

tcp_open() {
  local host="$1" port="$2"
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 3 "$host" "$port" 2>/dev/null
    return $?
  fi
  if command -v bash >/dev/null 2>&1; then
    (echo >/dev/tcp/"$host"/"$port") 2>/dev/null
    return $?
  fi
  return 2
}

echo "=== RAG Lab Data Layer Health Check ==="
echo "Target host: ${HOST}"
echo "PostgreSQL port: ${POSTGRES_PORT}"
echo "Elasticsearch port: ${ELASTICSEARCH_PORT}"
echo "Redis port: ${REDIS_PORT}"
echo "---"

# PostgreSQL port
if tcp_open "$HOST" "$POSTGRES_PORT"; then
  pass_msg "PostgreSQL port ${POSTGRES_PORT} reachable on ${HOST}"
else
  fail_msg "PostgreSQL port ${POSTGRES_PORT} not reachable on ${HOST}"
fi

# Elasticsearch HTTP
if command -v curl >/dev/null 2>&1; then
  es_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 \
    "http://${HOST}:${ELASTICSEARCH_PORT}" 2>/dev/null || echo "000")
  if [[ "$es_code" == "200" ]]; then
    pass_msg "Elasticsearch HTTP ${HOST}:${ELASTICSEARCH_PORT} (status ${es_code})"
    cluster=$(curl -sf --connect-timeout 5 \
      "http://${HOST}:${ELASTICSEARCH_PORT}/_cluster/health?pretty" 2>/dev/null | head -5)
    if [[ -n "$cluster" ]]; then
      echo "       $(echo "$cluster" | tr '\n' ' ')"
    fi
  else
    fail_msg "Elasticsearch not healthy at http://${HOST}:${ELASTICSEARCH_PORT} (http_code=${es_code})"
  fi
else
  if tcp_open "$HOST" "$ELASTICSEARCH_PORT"; then
    warn_msg "Elasticsearch port open but curl not installed — cannot verify HTTP"
  else
    fail_msg "Elasticsearch port ${ELASTICSEARCH_PORT} not reachable (curl missing)"
  fi
fi

# Redis port
if tcp_open "$HOST" "$REDIS_PORT"; then
  pass_msg "Redis port ${REDIS_PORT} reachable on ${HOST}"
else
  fail_msg "Redis port ${REDIS_PORT} not reachable on ${HOST}"
fi

# Optional: psql
if command -v psql >/dev/null 2>&1; then
  export PGPASSWORD="${POSTGRES_PASSWORD}"
  if psql -h "$HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -c "SELECT 1 AS ok;" -tA 2>/dev/null | grep -q '^1$'; then
    pass_msg "psql query OK (${POSTGRES_USER}@${HOST}/${POSTGRES_DB})"
    if psql -h "$HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
      -c "SELECT extname FROM pg_extension WHERE extname = 'vector';" -tA 2>/dev/null | grep -q vector; then
      pass_msg "PgVector extension present"
    else
      warn_msg "PgVector extension not found — run schema.sql after first deploy"
    fi
  else
    warn_msg "psql installed but query failed — check POSTGRES_PASSWORD / user / db"
  fi
  unset PGPASSWORD
else
  warn_msg "psql not installed — skipping database query check"
fi

# Optional: redis-cli
if command -v redis-cli >/dev/null 2>&1; then
  if [[ -n "$REDIS_PASSWORD" ]]; then
    redis_reply=$(redis-cli -h "$HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping 2>/dev/null || true)
  else
    redis_reply=$(redis-cli -h "$HOST" -p "$REDIS_PORT" ping 2>/dev/null || true)
  fi
  if [[ "$redis_reply" == "PONG" ]]; then
    pass_msg "redis-cli PING OK"
  else
    warn_msg "redis-cli PING failed (reply=${redis_reply:-empty})"
  fi
else
  warn_msg "redis-cli not installed — skipping Redis command check"
fi

echo "---"
echo "Summary: PASS=${pass} WARN=${warn} FAIL=${fail}"

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
