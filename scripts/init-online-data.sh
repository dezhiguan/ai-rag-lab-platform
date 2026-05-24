#!/usr/bin/env bash
# 线上环境初始化：样例数据、向量重建、ES 索引重建
#
# 用法：
#   export PUBLIC_BASE_URL="http://<LIGHT_SERVER_PUBLIC_IP>"
#   ./scripts/init-online-data.sh

set -euo pipefail

PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-http://127.0.0.1}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL%/}"

echo "=== Init Online Data ==="
echo "PUBLIC_BASE_URL=$PUBLIC_BASE_URL"
echo "---"

curl_json() {
  local method="$1"
  local url="$2"
  curl -sf -X "$method" -H "Content-Type: application/json" "$url"
}

echo "[1/3] Sample init..."
init_resp=$(curl_json POST "$PUBLIC_BASE_URL/api/sample/init" || true)
echo "$init_resp"

kb_id=""
if command -v python3 >/dev/null 2>&1; then
  kb_id=$(echo "$init_resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('knowledgeBaseId') or '')" 2>/dev/null || true)
fi
if [[ -z "$kb_id" ]]; then
  echo "Fetching kb id from sample status..."
  status_resp=$(curl_json GET "$PUBLIC_BASE_URL/api/sample/status")
  echo "$status_resp"
  kb_id=$(echo "$status_resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('knowledgeBaseId') or '')" 2>/dev/null || true)
fi

if [[ -z "$kb_id" ]]; then
  echo "Error: could not determine knowledgeBaseId"
  exit 1
fi
echo "Knowledge base id: $kb_id"

echo "[2/3] Embedding rebuild..."
embed_resp=$(curl_json POST "$PUBLIC_BASE_URL/api/kb/${kb_id}/embedding/rebuild")
echo "$embed_resp"

echo "[3/3] ES index rebuild..."
es_resp=$(curl_json POST "$PUBLIC_BASE_URL/api/search/index/rebuild")
echo "$es_resp"

echo "---"
echo "Done. Verify in browser:"
echo "  $PUBLIC_BASE_URL/debug"
echo "  $PUBLIC_BASE_URL/evaluation"
echo "  $PUBLIC_BASE_URL/rag-experiment"
