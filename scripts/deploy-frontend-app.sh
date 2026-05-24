#!/usr/bin/env bash
# 应用入口层：构建前端并同步到 Nginx 静态目录
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
NGINX_STATIC_DIR="${NGINX_STATIC_DIR:-/var/www/rag-lab/frontend}"
SKIP_BUILD="${SKIP_BUILD:-0}"

echo "=== Deploy RAG Frontend (App Layer) ==="
echo "FRONTEND_DIR=$FRONTEND_DIR"
echo "NGINX_STATIC_DIR=$NGINX_STATIC_DIR"

if [[ "$SKIP_BUILD" != "1" ]]; then
  if [[ ! -f "$FRONTEND_DIR/package.json" ]]; then
    echo "Error: frontend not found at $FRONTEND_DIR"
    exit 1
  fi
  echo "Building frontend..."
  (
    cd "$FRONTEND_DIR"
    if [[ ! -f .env.production ]]; then
      if [[ -f .env.production.example ]]; then
        cp .env.production.example .env.production
        echo "Created .env.production from example (VITE_API_BASE_URL should be empty)"
      fi
    fi
    npm install
    npm run build
  )
else
  echo "SKIP_BUILD=1, using existing frontend/dist"
fi

DIST_DIR="$FRONTEND_DIR/dist"
if [[ ! -d "$DIST_DIR" ]] || [[ ! -f "$DIST_DIR/index.html" ]]; then
  echo "Error: dist not found. Run build first or unset SKIP_BUILD"
  exit 1
fi

echo "Creating Nginx static directory: $NGINX_STATIC_DIR"
mkdir -p "$NGINX_STATIC_DIR"

if command -v rsync >/dev/null 2>&1; then
  rsync -av --delete "$DIST_DIR/" "$NGINX_STATIC_DIR/"
else
  echo "rsync not found, using cp -r"
  rm -rf "${NGINX_STATIC_DIR:?}"/*
  cp -r "$DIST_DIR"/* "$NGINX_STATIC_DIR/"
fi

echo "Frontend deployed to $NGINX_STATIC_DIR"
echo ""
echo "Access (replace placeholder with your light server public IP):"
echo "  http://<LIGHT_SERVER_PUBLIC_IP>/"
echo ""
echo "Ensure Nginx is configured: deploy/app-layer/nginx-rag.conf.example"
echo "Health via Nginx: http://<LIGHT_SERVER_PUBLIC_IP>/api/system/health"
