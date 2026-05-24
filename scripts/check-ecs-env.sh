#!/usr/bin/env bash
# ECS 环境就绪检查（V9-02）
# 在阿里云 ECS 初始化完成后运行，确认可继续执行生产部署。
#
# 用法：
#   chmod +x scripts/check-ecs-env.sh
#   ./scripts/check-ecs-env.sh
#
# 可选环境变量：
#   DEPLOY_ROOT   默认 /opt/rag-lab
#   UPLOAD_DIR    默认 /var/lib/rag-lab/uploads
#   FRONTEND_DIR  默认 /var/www/rag-lab/frontend

set -uo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/rag-lab}"
UPLOAD_DIR="${UPLOAD_DIR:-/var/lib/rag-lab/uploads}"
FRONTEND_DIR="${FRONTEND_DIR:-/var/www/rag-lab/frontend}"
MIN_JAVA_MAJOR=17
MIN_NODE_MAJOR=18

pass=0
warn=0
fail=0

pass_msg() {
  echo "[PASS] $1"
  pass=$((pass + 1))
}

warn_msg() {
  echo "[WARN] $1"
  warn=$((warn + 1))
}

fail_msg() {
  echo "[FAIL] $1"
  fail=$((fail + 1))
}

java_major() {
  java -version 2>&1 | awk -F '[."-]' '/version/ {
    v = $2
    if (v == "1") { print $3; exit }
    print v
  }'
}

node_major() {
  node -v 2>/dev/null | sed 's/^v//' | cut -d. -f1
}

port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -tln 2>/dev/null | grep -qE ":${port}\b"
    return $?
  fi
  if command -v netstat >/dev/null 2>&1; then
    netstat -tln 2>/dev/null | grep -qE ":${port}\s"
    return $?
  fi
  return 2
}

check_port_listenable() {
  local port="$1"
  local label="$2"
  port_in_use "$port"
  local rc=$?
  if [[ $rc -eq 0 ]]; then
    if command -v nginx >/dev/null 2>&1 && pgrep -x nginx >/dev/null 2>&1; then
      pass_msg "Port ${port} (${label}) in use — nginx is running"
    else
      warn_msg "Port ${port} (${label}) in use — verify it is nginx or intended service"
    fi
  elif [[ $rc -eq 1 ]]; then
    if command -v nginx >/dev/null 2>&1; then
      warn_msg "Port ${port} (${label}) not listening — configure nginx site before go-live"
    else
      warn_msg "Port ${port} (${label}) not listening — install and configure nginx"
    fi
  else
    warn_msg "Cannot detect port ${port} (${label}) — install ss or netstat"
  fi
}

echo "=== RAG Lab ECS Environment Check ==="
echo "DEPLOY_ROOT=${DEPLOY_ROOT}"
echo "UPLOAD_DIR=${UPLOAD_DIR}"
echo "FRONTEND_DIR=${FRONTEND_DIR}"
echo "User: $(whoami)"
echo "---"

# Java
if command -v java >/dev/null 2>&1; then
  major="$(java_major)"
  if [[ -n "$major" && "$major" -ge "$MIN_JAVA_MAJOR" ]]; then
    pass_msg "Java $(java -version 2>&1 | head -n1) (>= ${MIN_JAVA_MAJOR})"
  else
    fail_msg "Java version too old or unreadable (need >= ${MIN_JAVA_MAJOR}): $(java -version 2>&1 | head -n1)"
  fi
else
  fail_msg "Java not found — install OpenJDK 17"
fi

# Node.js
if command -v node >/dev/null 2>&1; then
  major="$(node_major)"
  if [[ -n "$major" && "$major" -ge "$MIN_NODE_MAJOR" ]]; then
    pass_msg "Node $(node -v) (>= v${MIN_NODE_MAJOR})"
  else
    fail_msg "Node.js version too old (need >= v${MIN_NODE_MAJOR}): $(node -v 2>/dev/null || echo unknown)"
  fi
else
  fail_msg "Node.js not found"
fi

# npm
if command -v npm >/dev/null 2>&1; then
  pass_msg "npm $(npm -v)"
else
  fail_msg "npm not found"
fi

# Nginx
if command -v nginx >/dev/null 2>&1; then
  pass_msg "Nginx installed: $(nginx -v 2>&1)"
else
  fail_msg "Nginx not installed"
fi

# curl
if command -v curl >/dev/null 2>&1; then
  pass_msg "curl available: $(curl --version | head -n1)"
else
  fail_msg "curl not found"
fi

# git / unzip / rsync (informational)
for tool in git unzip rsync; do
  if command -v "$tool" >/dev/null 2>&1; then
    pass_msg "${tool} available"
  else
    warn_msg "${tool} not found — recommended for deployment"
  fi
done

# Deploy directory
if [[ -d "$DEPLOY_ROOT" ]]; then
  pass_msg "Deploy directory exists: ${DEPLOY_ROOT}"
else
  fail_msg "Deploy directory missing: ${DEPLOY_ROOT}"
fi

# Upload directory
if [[ -d "$UPLOAD_DIR" ]]; then
  pass_msg "Upload directory exists: ${UPLOAD_DIR}"
else
  fail_msg "Upload directory missing: ${UPLOAD_DIR}"
fi

# Write permission on deploy directory
if [[ -d "$DEPLOY_ROOT" && -w "$DEPLOY_ROOT" ]]; then
  pass_msg "Current user can write to deploy directory: ${DEPLOY_ROOT}"
elif [[ -d "$DEPLOY_ROOT" ]]; then
  fail_msg "Current user cannot write to deploy directory: ${DEPLOY_ROOT}"
fi

# Frontend static directory (optional but recommended)
if [[ -d "$FRONTEND_DIR" ]]; then
  pass_msg "Frontend static directory exists: ${FRONTEND_DIR}"
else
  warn_msg "Frontend static directory missing: ${FRONTEND_DIR} — create before nginx deploy"
fi

# Ports 80 / 443
check_port_listenable 80 "HTTP"
check_port_listenable 443 "HTTPS"

echo "---"
echo "Summary: PASS=${pass} WARN=${warn} FAIL=${fail}"

if [[ "$fail" -gt 0 ]]; then
  echo "Result: NOT READY — fix FAIL items, then re-run."
  exit 1
fi

if [[ "$warn" -gt 0 ]]; then
  echo "Result: READY WITH WARNINGS — you may proceed after reviewing WARN items."
  exit 0
fi

echo "Result: READY — proceed with docs/09-production-deployment.md"
exit 0
