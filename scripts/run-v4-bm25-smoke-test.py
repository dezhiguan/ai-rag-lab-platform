#!/usr/bin/env python3
"""
V4 BM25 冒烟测试：读取 .env，调用本机后端 API（不依赖 docker-compose）。

前置：后端已启动且 .env 中 ES_HOSTS 指向可用 Elasticsearch。

用法:
  python3 scripts/run-v4-bm25-smoke-test.py
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from lib.load_dotenv import load_dotenv  # noqa: E402

BASE_URL = os.environ.get("RAG_API_BASE_URL", "http://localhost:8080").rstrip("/")
KB_ID = int(os.environ.get("RAG_TEST_KB_ID", "1"))

BM25_QUERIES = [
    ("SMS_429", "SMS_429 是什么意思？", "02-api-spec.md"),
    ("send-code", "send-code 接口路径是什么？", "02-api-spec.md"),
    ("send-code-path", "/api/sms/send-code 是什么接口？", "02-api-spec.md"),
]


def post(path: str, body: dict) -> dict:
    url = f"{BASE_URL}{path}"
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        payload = json.loads(resp.read().decode())
    if payload.get("code") != 200:
        raise RuntimeError(payload.get("message", payload))
    return payload.get("data", {})


def main() -> int:
    load_dotenv()
    print(f"API: {BASE_URL}  ES: {os.environ.get('ES_HOSTS', '(未设置)')}\n")

    print("1. 重建 ES 索引 ...")
    try:
        rebuild = post("/api/search/index/rebuild", {})
        print(f"   synced={rebuild.get('syncedCount')} index={rebuild.get('indexName')}\n")
    except Exception as e:
        print(f"   失败: {e}")
        print("   请确认后端已启动且 .env 中 ES 可连通（python3 scripts/probe-rag-services.py）")
        return 1

    passed = 0
    for label, query, expected_doc in BM25_QUERIES:
        print(f"2. BM25 [{label}] query={query!r}")
        try:
            data = post("/api/search/bm25", {"kbId": KB_ID, "query": query, "topK": 5})
            results = data.get("results") or []
            if not results:
                print("   FAIL: 无结果\n")
                continue
            top = results[0]
            top_name = top.get("documentName", "")
            ok = expected_doc in top_name
            print(f"   Top1: {top_name} score={top.get('score')} {'PASS' if ok else 'FAIL (期望含 ' + expected_doc + ')'}\n")
            if ok:
                passed += 1
        except Exception as e:
            print(f"   FAIL: {e}\n")

    print(f"BM25 冒烟: {passed}/{len(BM25_QUERIES)} 通过")
    return 0 if passed == len(BM25_QUERIES) else 1


if __name__ == "__main__":
    sys.exit(main())
