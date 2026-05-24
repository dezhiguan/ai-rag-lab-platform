#!/usr/bin/env bash
# 线上联调验收（V9-06）
#
# 用法：
#   export PUBLIC_BASE_URL="http://<LIGHT_SERVER_PUBLIC_IP>"
#   export ECS_PRIVATE_IP="<ECS_PRIVATE_IP>"   # 可选，检查内网数据层
#   ./scripts/verify-online-deployment.sh
#
# 在轻量服务器本机验收时可：
#   PUBLIC_BASE_URL="http://127.0.0.1" ./scripts/verify-online-deployment.sh

set -uo pipefail

PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-http://127.0.0.1}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL%/}"
ECS_PRIVATE_IP="${ECS_PRIVATE_IP:-}"
AUTH_USERNAME="${AUTH_USERNAME:-guest}"
AUTH_PASSWORD="${AUTH_PASSWORD:-guest123}"

pass=0
warn=0
fail=0

auth_token=""

login_for_token() {
  local body
  body=$(curl -sf --connect-timeout 8 -X POST "$PUBLIC_BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$AUTH_USERNAME\",\"password\":\"$AUTH_PASSWORD\"}" 2>/dev/null || true)
  auth_token=$(echo "$body" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  if [[ -n "$auth_token" ]]; then
    pass_msg "Auth login OK (user=$AUTH_USERNAME)"
  else
    warn_msg "Auth login skipped or failed — protected API checks may warn"
  fi
}

auth_curl() {
  if [[ -n "$auth_token" ]]; then
    curl -sf --connect-timeout 15 -H "Authorization: Bearer $auth_token" "$1" 2>/dev/null || true
  else
    curl -sf --connect-timeout 15 "$1" 2>/dev/null || true
  fi
}

auth_http_code() {
  if [[ -n "$auth_token" ]]; then
    curl -s -o /dev/null -w "%{http_code}" --connect-timeout 8 -H "Authorization: Bearer $auth_token" "$1" 2>/dev/null || echo "000"
  else
    http_code "$1"
  fi
}

pass_msg() { echo "[PASS] $1"; pass=$((pass + 1)); }
warn_msg() { echo "[WARN] $1"; warn=$((warn + 1)); }
fail_msg() { echo "[FAIL] $1"; fail=$((fail + 1)); }

http_code() {
  curl -s -o /dev/null -w "%{http_code}" --connect-timeout 8 "$1" 2>/dev/null || echo "000"
}

echo "=== Online Deployment Verification (V9-06) ==="
echo "PUBLIC_BASE_URL=$PUBLIC_BASE_URL"
[[ -n "$ECS_PRIVATE_IP" ]] && echo "ECS_PRIVATE_IP=$ECS_PRIVATE_IP"
echo "---"

# Frontend
root_code=$(http_code "$PUBLIC_BASE_URL/")
if [[ "$root_code" == "200" ]]; then
  pass_msg "Frontend reachable: $PUBLIC_BASE_URL/ (HTTP $root_code)"
else
  fail_msg "Frontend not reachable: $PUBLIC_BASE_URL/ (HTTP $root_code)"
fi

# Health via public/nginx
health_code=$(http_code "$PUBLIC_BASE_URL/api/system/health")
if [[ "$health_code" == "200" ]]; then
  pass_msg "Health API: $PUBLIC_BASE_URL/api/system/health (HTTP $health_code)"
  body=$(curl -sf --connect-timeout 8 "$PUBLIC_BASE_URL/api/system/health" 2>/dev/null || true)
  echo "       ${body:0:200}"
else
  fail_msg "Health API failed: $PUBLIC_BASE_URL/api/system/health (HTTP $health_code)"
fi

login_for_token

# System status (PG / ES connectivity from backend)
status_code=$(auth_http_code "$PUBLIC_BASE_URL/api/system/status")
if [[ "$status_code" == "200" ]]; then
  pass_msg "System status API: HTTP $status_code"
  status_body=$(auth_curl "$PUBLIC_BASE_URL/api/system/status")
  if echo "$status_body" | grep -qi postgres; then
    if echo "$status_body" | grep -qiE '"status"\s*:\s*"UP"|"status":"UP"'; then
      pass_msg "System status response contains UP indicators"
    else
      warn_msg "System status returned but check PG/ES fields manually"
    fi
  fi
  echo "       ${status_body:0:300}"
else
  warn_msg "System status API: HTTP $status_code"
fi

# Sample status
sample_code=$(auth_http_code "$PUBLIC_BASE_URL/api/sample/status")
if [[ "$sample_code" == "200" ]]; then
  pass_msg "Sample status API: HTTP $sample_code"
else
  warn_msg "Sample status API: HTTP $sample_code (run init-online-data.sh if first deploy)"
fi

# Data layer from app server (optional)
if [[ -n "$ECS_PRIVATE_IP" && "$ECS_PRIVATE_IP" != *"<"* ]]; then
  echo "---"
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  if [[ -x "$script_dir/check-data-layer.sh" ]]; then
    echo "Running check-data-layer.sh $ECS_PRIVATE_IP ..."
    if "$script_dir/check-data-layer.sh" "$ECS_PRIVATE_IP"; then
      pass_msg "Data layer check from app host"
    else
      fail_msg "Data layer check from app host"
    fi
  fi
fi

echo "---"
echo "Summary: PASS=$pass WARN=$warn FAIL=$fail"

if [[ "$fail" -gt 0 ]]; then
  echo "Result: NOT VERIFIED — see docs/11-dual-server-online-runbook.md"
  exit 1
fi
echo "Result: VERIFIED (with ${warn} warnings)"
exit 0
