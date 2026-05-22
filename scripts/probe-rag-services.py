#!/usr/bin/env python3
"""
探测本机/远程 RAG 依赖是否可用（读取 .env，忽略 docker-compose.yml）。

用法:
  python3 scripts/probe-rag-services.py

环境变量（可由 .env 提供）:
  POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
  ES_HOSTS, ES_USERNAME, ES_PASSWORD
  RAG_API_BASE_URL  默认 http://localhost:8080
"""
from __future__ import annotations

import base64
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from lib.load_dotenv import load_dotenv  # noqa: E402


def probe_url(url: str, auth: tuple[str, str] | None = None, timeout: int = 5) -> tuple[bool, str]:
    req = urllib.request.Request(url)
    if auth and auth[0]:
        token = base64.b64encode(f"{auth[0]}:{auth[1]}".encode()).decode()
        req.add_header("Authorization", f"Basic {token}")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read(200).decode("utf-8", errors="replace")
            return True, f"HTTP {resp.status} {body[:120]}"
    except urllib.error.HTTPError as e:
        return False, f"HTTP {e.code} {e.reason}"
    except Exception as e:
        return False, str(e)


def main() -> int:
    env_file = load_dotenv()
    print("=== RAG 环境探测（来源: .env，非 docker-compose）===\n")
    if env_file:
        print(f"已加载: {env_file}\n")
    else:
        print("未找到 .env，仅使用当前 shell 环境变量\n")

    api_base = os.environ.get("RAG_API_BASE_URL", "http://localhost:8080").rstrip("/")
    es_hosts = os.environ.get("ES_HOSTS", "http://localhost:9200").rstrip("/")
    es_user = os.environ.get("ES_USERNAME", "")
    es_pass = os.environ.get("ES_PASSWORD", "")
    pg_host = os.environ.get("POSTGRES_HOST", "localhost")
    pg_port = os.environ.get("POSTGRES_PORT", "5432")
    pg_db = os.environ.get("POSTGRES_DB", "postgres")

    auth = (es_user, es_pass) if es_user else None

    checks: list[tuple[str, bool, str]] = []

    ok, msg = probe_url(f"{es_hosts}")
    if not ok and auth:
        ok, msg = probe_url(f"{es_hosts}", auth=auth)
    checks.append(("Elasticsearch", ok, f"{es_hosts} → {msg}"))

    ok, msg = probe_url(f"{api_base}/api/system/health")
    checks.append(("后端 API", ok, f"{api_base} → {msg}"))

    ok, msg = probe_url(f"{api_base}/api/kb")
    checks.append(("知识库 API", ok, msg))

    print(f"PostgreSQL（配置）: {pg_host}:{pg_port}/{pg_db}")
    print("  （脚本不直连 PG，由 Spring Boot 启动时验证）\n")

    all_ok = True
    for name, ok, detail in checks:
        status = "OK" if ok else "FAIL"
        print(f"[{status}] {name}: {detail}")
        if not ok and name != "知识库 API":
            all_ok = False

    print("\n--- 建议 ---")
    if not checks[0][1]:
        print("- 检查 .env 中 ES_HOSTS / ES_USERNAME / ES_PASSWORD")
    if not checks[1][1]:
        print("- 在 VS Code 用 launch「RagApplication」（已配置 envFile: .env）启动后端")
    if checks[0][1] and checks[1][1]:
        print("- 可执行: curl -X POST {}/api/search/index/rebuild".format(api_base))
        print("- 可执行: python3 scripts/run-v4-bm25-smoke-test.py")

    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
