#!/usr/bin/env bash
# ECS 数据层一键启动（在项目 deploy/data-layer 目录执行）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DATA_DIR="$ROOT_DIR/deploy/data-layer"

cd "$DATA_DIR"

if [[ ! -f .env.data ]]; then
  echo "Error: deploy/data-layer/.env.data not found"
  echo "Run: cp .env.data.example .env.data && edit passwords"
  exit 1
fi

if [[ "$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)" -lt 262144 ]]; then
  echo "Setting vm.max_map_count=262144 (Elasticsearch requirement)..."
  sudo sysctl -w vm.max_map_count=262144 || echo "Warning: could not set vm.max_map_count"
fi

docker compose -f docker-compose.data.yml --env-file .env.data up -d
docker compose -f docker-compose.data.yml --env-file .env.data ps

echo ""
echo "Run health check:"
echo "  $ROOT_DIR/scripts/check-data-layer.sh"
