# 06 - Bug 修复记录

本文档记录各版本 Bug、现象、原因与修复（或待修复方案）。按时间倒序或版本分组维护。

---

## V4：BM25 排序异常（进行中）

| 项 | 内容 |
|----|------|
| **现象** | 查询「SMS_429 是什么意思？」时，BM25 将 `01-project-guideline.md` 排第一 |
| **期望** | Top1 应为 `02-api-spec.md`（含错误码表） |
| **初步原因** | standard analyzer + 中文问句分词 + `match` OR 导致普通词干扰；专有词权重不足 |
| **待修复/已实施方向** | ① `SearchTermExtractor` 抽取专有词；② ES `terms` keyword；③ 重建索引写入 terms；④ 查询 bool should 多路加权（terms 精确高 boost、match_phrase 次高、全文降权） |
| **验证** | `/debug` BM25 模式 + `scripts/run-v4-bm25-smoke-test.py`；验收表见 `05-development-plan.md` |
| **状态** | 修复方案已落地代码侧，需重建 ES 索引后回归验收 |

---

## V3：Context 噪声进入 Prompt

| 项 | 内容 |
|----|------|
| **现象** | 低相关 Chunk 进入 Prompt，回答夹杂无关内容（如 guideline 背景段落） |
| **原因** | 检索 TopK 全量拼接 Prompt，无分数阈值与条数上限 |
| **修复** | 引入 `ContextChunkFilter`：双轨 `retrievedChunks`（全量展示）vs `contextChunks`（进 Prompt）；规则 `minScore`、`maxScoreGap`、`maxChunks`；全部被滤时保底 Top1；字段 `usedInPrompt`、`filterReason` |
| **配置** | `rag.context.max-chunks: 2`、`min-score: 0.45`、`max-score-gap: 0.35` |
| **状态** | ✅ 已修复 |

### V3 Context 过滤测试结论（迁移自 test-cases）

**执行时间：** 2026-05-22（UTC）  
**脚本：** `scripts/run-v3-context-filter-tests.py`  
**环境：** `kbId=1`，Embedding qwen，Chat deepseek  
**结果：** **10/10 通过**

| 编号 | 问题 | 要点 |
|------|------|------|
| TC-01 | 短信验证码发不出去怎么排查？ | Top2 进入 Prompt；guideline `SCORE_TOO_LOW` |
| TC-02 | SMS_429 是什么意思？ | api-spec + troubleshooting 进入；guideline 过滤 |
| TC-03 | send-code 接口路径是什么？ | api-spec Top1；2 条进入 Prompt |
| TC-04 | 公司年终奖发几个月？ | 保底 Top1；Answer 拒答 |
| TC-05 | 短信验证码接口有哪些限制？ | `maxChunks=2` 生效 |
| TC-06 | 为什么后续会使用 PgVector？ | 仅 guideline 进入 |
| TC-07 | Redis 缓存异常怎么排查？ | 仅 troubleshooting 进入 |
| TC-08 | 公司股票期权归属规则是什么？ | 全低分保底 Top1 + 拒答 |
| TC-09/10 | Prompt 泄漏检测 / 页面字段 | `usedInPrompt=false` 的正文不出现在 prompt |

**通过标准（脚本自动判定）：**

1. HTTP + `code=200`
2. `retrievedChunks`、`contextChunks` 非空
3. Prompt 不包含 `usedInPrompt=false` 的 Chunk 正文
4. `usedInPrompt` / `filterReason` 符合约定
5. `latency` 字段完整

**filterReason 枚举：**

| 值 | 含义 |
|----|------|
| `SCORE_TOO_LOW` | score < minScore |
| `SCORE_GAP_TOO_LARGE` | 与 Top1 分差 > maxScoreGap |
| `EXCEED_MAX_CONTEXT_CHUNKS` | 超过 maxChunks 排位 |

---

## V2：MockEmbeddingProvider 检索不准

| 项 | 内容 |
|----|------|
| **现象** | Mock 向量若随机或不可重复，TopK 不稳定，检索几乎不可用 |
| **原因** | 非确定性向量导致相同文本每次 Embedding 不同 |
| **修复** | 改为**确定性**字符 n-gram 哈希向量，并 **L2 normalize**，保证同文本同向量、可重复排序 |
| **状态** | ✅ 已修复 |

---

## 记录模板（新增 Bug 时使用）

```markdown
## Vx：标题

| 项 | 内容 |
|----|------|
| **现象** | |
| **原因** | |
| **修复** | |
| **状态** | 待修复 / 已修复 |
```
