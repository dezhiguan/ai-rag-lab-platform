#!/usr/bin/env python3
"""
V3 Context 过滤测试执行脚本。
真实调用 POST /api/debug/query，将结果写入 docs/test-cases/v3-context-filter-test-result.md
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
RESULT_PATH = ROOT / "docs" / "test-cases" / "v3-context-filter-test-result.md"

sys.path.insert(0, str(ROOT / "scripts"))
from lib.load_dotenv import load_dotenv  # noqa: E402

load_dotenv()

BASE_URL = os.environ.get("RAG_API_BASE_URL", "http://localhost:8080").rstrip("/")
KB_ID = int(os.environ.get("RAG_TEST_KB_ID", "1"))
TOP_K = int(os.environ.get("RAG_TEST_TOP_K", "5"))

# TC_009 / TC_010 复用与 TC_001 相同的问题
PROMPT_CHECK_QUESTION = "短信验证码发不出去怎么排查？"

TEST_CASES: list[tuple[str, str, str | None]] = [
    ("V3_TC_001", "短信验证码发不出去怎么排查？", None),
    ("V3_TC_002", "SMS_429 是什么意思？", None),
    ("V3_TC_003", "send-code 接口路径是什么？", None),
    ("V3_TC_004", "公司年终奖发几个月？", None),
    ("V3_TC_005", "短信验证码接口有哪些限制？", None),
    ("V3_TC_006", "为什么后续会使用 PgVector？", None),
    ("V3_TC_007", "Redis 缓存异常怎么排查？", None),
    ("V3_TC_008", "公司股票期权归属规则是什么？", None),
    ("V3_TC_009", PROMPT_CHECK_QUESTION, "prompt_isolation"),
    ("V3_TC_010", PROMPT_CHECK_QUESTION, "field_completeness"),
]

REQUIRED_TOP_FIELDS = [
    "queryLogId",
    "question",
    "embeddingProvider",
    "embeddingModel",
    "chatProvider",
    "chatModel",
    "retrievedChunks",
    "contextChunks",
    "prompt",
    "answer",
    "latency",
]

REQUIRED_CHUNK_FIELDS = [
    "chunkId",
    "documentId",
    "documentName",
    "chunkIndex",
    "score",
    "content",
    "usedInPrompt",
]

REQUIRED_LATENCY_FIELDS = ["retrievalTimeMs", "generationTimeMs", "totalTimeMs"]


@dataclass
class CaseResult:
    case_id: str
    question: str
    case_type: str | None
    passed: bool
    failures: list[str] = field(default_factory=list)
    suggestions: list[str] = field(default_factory=list)
    request: dict[str, Any] = field(default_factory=dict)
    response: dict[str, Any] | None = None
    http_status: int | None = None
    api_error: str | None = None
    duration_ms: int | None = None


def call_debug_query(question: str) -> tuple[dict[str, Any] | None, int | None, str | None, int]:
    url = f"{BASE_URL}/api/debug/query"
    body = json.dumps({"kbId": KB_ID, "question": question, "topK": TOP_K}).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    import time

    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            elapsed = int((time.perf_counter() - start) * 1000)
            raw = resp.read().decode("utf-8")
            payload = json.loads(raw)
            status = resp.status
    except urllib.error.HTTPError as e:
        elapsed = int((time.perf_counter() - start) * 1000)
        try:
            err_body = e.read().decode("utf-8")
        except Exception:
            err_body = str(e)
        return None, e.code, f"HTTP {e.code}: {err_body[:500]}", elapsed
    except urllib.error.URLError as e:
        elapsed = int((time.perf_counter() - start) * 1000)
        return None, None, f"连接失败: {e.reason}", elapsed
    except Exception as e:
        elapsed = int((time.perf_counter() - start) * 1000)
        return None, None, str(e), elapsed

    if payload.get("code") != 200:
        msg = payload.get("message", json.dumps(payload, ensure_ascii=False)[:300])
        return None, status, f"API 业务错误 code={payload.get('code')}: {msg}", elapsed

    data = payload.get("data")
    if data is None:
        return None, status, "响应缺少 data 字段", elapsed
    return data, status, None, elapsed


def normalize_content(text: str | None) -> str:
    if not text:
        return ""
    return " ".join(text.split())


def check_prompt_isolation(data: dict[str, Any]) -> list[str]:
    failures: list[str] = []
    prompt = data.get("prompt") or ""
    context = data.get("context") or ""
    retrieved = data.get("retrievedChunks") or []
    context_chunks = data.get("contextChunks") or []

    if not context_chunks:
        failures.append("contextChunks 为空，无法验证 Prompt 隔离")
        return failures

    for chunk in retrieved:
        if chunk.get("usedInPrompt"):
            continue
        content = chunk.get("content") or ""
        if not content.strip():
            continue
        norm = normalize_content(content)
        if len(norm) < 20:
            snippet = content.strip()
        else:
            snippet = norm[:80]
        if snippet and snippet in prompt:
            failures.append(
                f"Prompt 包含被过滤 Chunk 正文片段（documentName={chunk.get('documentName')}, "
                f"filterReason={chunk.get('filterReason')}）"
            )
        if snippet and snippet in context:
            failures.append(
                f"context 包含被过滤 Chunk 正文片段（documentName={chunk.get('documentName')}）"
            )

    for chunk in context_chunks:
        content = chunk.get("content") or ""
        if content.strip() and content.strip() not in prompt and normalize_content(content)[:40] not in prompt:
            failures.append(
                f"Prompt 未包含 contextChunk 内容（documentName={chunk.get('documentName')}）"
            )
    return failures


def check_field_completeness(data: dict[str, Any]) -> list[str]:
    failures: list[str] = []
    for f in REQUIRED_TOP_FIELDS:
        if f not in data or data[f] is None:
            failures.append(f"缺少顶层字段: {f}")

    retrieved = data.get("retrievedChunks")
    if retrieved is not None:
        if not isinstance(retrieved, list):
            failures.append("retrievedChunks 不是数组")
        elif len(retrieved) == 0:
            failures.append("retrievedChunks 为空")
        else:
            for i, chunk in enumerate(retrieved):
                for cf in REQUIRED_CHUNK_FIELDS:
                    if cf not in chunk:
                        failures.append(f"retrievedChunks[{i}] 缺少字段: {cf}")
                if "filterReason" not in chunk:
                    failures.append(f"retrievedChunks[{i}] 缺少字段: filterReason")
                if chunk.get("usedInPrompt") is True and chunk.get("filterReason") not in (None, ""):
                    failures.append(
                        f"retrievedChunks[{i}] usedInPrompt=true 但 filterReason 非空"
                    )
                if chunk.get("usedInPrompt") is False and not chunk.get("filterReason"):
                    failures.append(
                        f"retrievedChunks[{i}] usedInPrompt=false 但 filterReason 为空"
                    )

    context_chunks = data.get("contextChunks")
    if context_chunks is not None:
        if not isinstance(context_chunks, list):
            failures.append("contextChunks 不是数组")
        elif len(context_chunks) == 0:
            failures.append("contextChunks 为空")

    latency = data.get("latency")
    if latency is None:
        failures.append("缺少 latency")
    else:
        for lf in REQUIRED_LATENCY_FIELDS:
            if lf not in latency or latency[lf] is None:
                failures.append(f"latency 缺少字段: {lf}")

    return failures


def evaluate_standard(data: dict[str, Any]) -> list[str]:
    failures: list[str] = []
    retrieved = data.get("retrievedChunks") or []
    context_chunks = data.get("contextChunks") or []

    if not retrieved:
        failures.append("retrievedChunks 为空")
    if not context_chunks:
        failures.append("contextChunks 为空")

    failures.extend(check_prompt_isolation(data))

    for chunk in retrieved:
        if "usedInPrompt" not in chunk:
            failures.append(f"Chunk 缺少 usedInPrompt（documentName={chunk.get('documentName')}）")
        if chunk.get("usedInPrompt") is False and not chunk.get("filterReason"):
            failures.append(
                f"未进入 Prompt 的 Chunk 缺少 filterReason（documentName={chunk.get('documentName')}）"
            )

    if data.get("latency") is None:
        failures.append("缺少 latency 字段")

    return failures


def run_case(case_id: str, question: str, case_type: str | None) -> CaseResult:
    request = {"kbId": KB_ID, "question": question, "topK": TOP_K}
    result = CaseResult(
        case_id=case_id,
        question=question,
        case_type=case_type,
        passed=False,
        request=request,
    )

    data, status, err, elapsed = call_debug_query(question)
    result.http_status = status
    result.duration_ms = elapsed
    result.api_error = err
    result.response = data

    if err or data is None:
        result.failures.append(err or "无响应数据")
        return result

    if case_type == "prompt_isolation":
        result.failures = check_prompt_isolation(data)
        if not (data.get("contextChunks")):
            result.failures.append("contextChunks 为空")
        if not (data.get("retrievedChunks")):
            result.failures.append("retrievedChunks 为空")
    elif case_type == "field_completeness":
        result.failures = check_field_completeness(data)
    else:
        result.failures = evaluate_standard(data)

    result.passed = len(result.failures) == 0
    return result


def md_escape(s: str) -> str:
    return s.replace("|", "\\|").replace("\n", " ")


def chunk_table(chunks: list[dict[str, Any]] | None) -> str:
    if not chunks:
        return "_（无数据）_\n"
    lines = [
        "| documentName | score | usedInPrompt | filterReason |",
        "|--------------|-------|--------------|--------------|",
    ]
    for c in chunks:
        score = c.get("score")
        score_s = f"{score:.4f}" if isinstance(score, (int, float)) else str(score)
        lines.append(
            f"| {md_escape(str(c.get('documentName', '')))} | {score_s} | "
            f"{c.get('usedInPrompt')} | {c.get('filterReason') or '—'} |"
        )
    return "\n".join(lines) + "\n"


def context_chunks_list(chunks: list[dict[str, Any]] | None) -> str:
    if not chunks:
        return "_（无）_\n"
    lines = []
    for i, c in enumerate(chunks, 1):
        lines.append(
            f"{i}. `{c.get('documentName')}` (chunkId={c.get('chunkId')}, score={c.get('score')})"
        )
    return "\n".join(lines) + "\n"


def prompt_check_section(result: CaseResult) -> str:
    if result.api_error or not result.response:
        return "_接口调用失败，未执行 Prompt 检查_\n"

    data = result.response
    prompt = data.get("prompt") or ""
    filtered = [
        c for c in (data.get("retrievedChunks") or []) if not c.get("usedInPrompt")
    ]
    included = data.get("contextChunks") or []

    lines = [
        f"- Prompt 长度：{len(prompt)} 字符",
        f"- contextChunks 数量：{len(included)}",
        f"- 被过滤 Chunk 数量：{len(filtered)}",
    ]
    if result.case_type == "prompt_isolation" or result.case_id != "V3_TC_010":
        leaked = [
            c.get("documentName")
            for c in filtered
            if (c.get("content") or "").strip()
            and (
                (c.get("content") or "").strip() in prompt
                or normalize_content(c.get("content"))[:40] in prompt
            )
        ]
        if leaked:
            lines.append(f"- **泄漏检测**：被过滤文档正文出现在 Prompt 中 → `{leaked}`")
        else:
            lines.append("- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓")
    return "\n".join(lines) + "\n"


def build_markdown(results: list[CaseResult], run_at: str) -> str:
    passed = sum(1 for r in results if r.passed)
    total = len(results)
    overall = "通过" if passed == total else "未全部通过"

    lines = [
        "# V3 Context 过滤测试执行结果",
        "",
        "> 由 `scripts/run-v3-context-filter-tests.py` 自动生成，数据来自真实 `POST /api/debug/query` 调用。",
        "",
        "## 执行摘要",
        "",
        f"| 项 | 值 |",
        f"|----|-----|",
        f"| 执行时间（UTC） | {run_at} |",
        f"| API 地址 | `{BASE_URL}` |",
        f"| kbId | {KB_ID} |",
        f"| topK | {TOP_K} |",
        f"| 用例总数 | {total} |",
        f"| 通过 | {passed} |",
        f"| 失败 | {total - passed} |",
        f"| 总体结论 | **{overall}** |",
        "",
        "## 通过标准（脚本自动判定）",
        "",
        "1. 接口调用成功（HTTP + `code=200`）",
        "2. `retrievedChunks` 不为空",
        "3. `contextChunks` 不为空",
        "4. Prompt 不包含 `usedInPrompt=false` 的 Chunk 正文",
        "5. `usedInPrompt` / `filterReason` 字段符合约定",
        "6. `latency` 字段完整",
        "",
        "---",
        "",
    ]

    for r in results:
        status = "✅ 通过" if r.passed else "❌ 未通过"
        lines.append(f"## {r.case_id} — {status}")
        lines.append("")
        lines.append(f"**测试问题**：{r.question}")
        lines.append("")

        lines.append("### 请求参数")
        lines.append("")
        lines.append("```json")
        lines.append(json.dumps(r.request, ensure_ascii=False, indent=2))
        lines.append("```")
        lines.append("")

        if r.api_error:
            lines.append("### 接口状态")
            lines.append("")
            lines.append(f"- HTTP 状态：{r.http_status or '—'}")
            lines.append(f"- **失败原因**：{md_escape(r.api_error)}")
            lines.append(f"- 请求耗时：{r.duration_ms} ms")
            lines.append("")
            lines.append("### 是否通过")
            lines.append("")
            lines.append(f"**{status}**")
            lines.append("")
            lines.append("### 失败原因 / 建议")
            lines.append("")
            for f in r.failures:
                lines.append(f"- {f}")
            lines.append("- 请确认：后端已启动、PostgreSQL 可用、样例库已初始化、向量已 rebuild。")
            lines.append("")
            lines.append("---")
            lines.append("")
            continue

        data = r.response or {}
        lines.append("### 接口状态")
        lines.append("")
        lines.append(f"- HTTP 状态：{r.http_status}")
        lines.append(f"- queryLogId：`{data.get('queryLogId')}`")
        lines.append(
            f"- Embedding：`{data.get('embeddingProvider')}` / `{data.get('embeddingModel')}`"
        )
        lines.append(f"- Chat：`{data.get('chatProvider')}` / `{data.get('chatModel')}`")
        lines.append(f"- 请求耗时（脚本侧）：{r.duration_ms} ms")
        lines.append("")

        lines.append("### retrievedChunks")
        lines.append("")
        lines.append(chunk_table(data.get("retrievedChunks")))
        lines.append("")

        lines.append("### contextChunks")
        lines.append("")
        lines.append(context_chunks_list(data.get("contextChunks")))
        lines.append("")

        lines.append("### Prompt 检查")
        lines.append("")
        lines.append(prompt_check_section(r))
        prompt = data.get("prompt") or ""
        if prompt:
            preview = prompt[:400] + ("…" if len(prompt) > 400 else "")
            lines.append("<details><summary>Prompt 预览（前 400 字符）</summary>")
            lines.append("")
            lines.append("```")
            lines.append(preview)
            lines.append("```")
            lines.append("</details>")
            lines.append("")

        answer = data.get("answer") or ""
        lines.append("### Answer 摘要")
        lines.append("")
        summary = answer[:200] + ("…" if len(answer) > 200 else "")
        lines.append(f"> {md_escape(summary)}")
        lines.append("")

        lat = data.get("latency") or {}
        lines.append("### 耗时（latency）")
        lines.append("")
        lines.append(
            f"| retrievalTimeMs | generationTimeMs | totalTimeMs |"
        )
        lines.append(f"|-----------------|------------------|-------------|")
        lines.append(
            f"| {lat.get('retrievalTimeMs')} | {lat.get('generationTimeMs')} | {lat.get('totalTimeMs')} |"
        )
        lines.append("")

        lines.append("### 是否通过")
        lines.append("")
        lines.append(f"**{status}**")
        lines.append("")

        lines.append("### 失败原因 / 后续建议")
        lines.append("")
        if r.failures:
            for f in r.failures:
                lines.append(f"- {f}")
            if any("检索" in f or "recall" in f.lower() for f in r.failures):
                lines.append(
                    "- 检索效果待 V4 BM25 / 关键词检索优化（不在 V3 范围修复）。"
                )
        else:
            lines.append("- 无")
        if r.suggestions:
            for s in r.suggestions:
                lines.append(f"- {s}")
        lines.append("")
        lines.append("---")
        lines.append("")

    return "\n".join(lines)


def main() -> int:
    print(f"API: {BASE_URL}, kbId={KB_ID}, topK={TOP_K}")
    run_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    results: list[CaseResult] = []

    for case_id, question, case_type in TEST_CASES:
        print(f"Running {case_id}: {question[:40]}...")
        cr = run_case(case_id, question, case_type)
        results.append(cr)
        status = "PASS" if cr.passed else "FAIL"
        print(f"  -> {status}" + (f" ({cr.api_error})" if cr.api_error else ""))

    md = build_markdown(results, run_at)
    RESULT_PATH.parent.mkdir(parents=True, exist_ok=True)
    RESULT_PATH.write_text(md, encoding="utf-8")
    print(f"\nWrote {RESULT_PATH}")

    passed = sum(1 for r in results if r.passed)
    print(f"Summary: {passed}/{len(results)} passed")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
