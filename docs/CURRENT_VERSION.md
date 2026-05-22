# CURRENT_VERSION.md

## 当前版本

**V3：RAG Debug 可观察版（含轻量级 Context 过滤）**

在 V2 / V2.5 基础上提供 Debug 可观察能力，并对进入 Prompt 的 Chunk 做分数与 Top1 差距过滤，降低弱相关噪声。

## 版本关系

| 版本 | 状态 |
|------|------|
| V1 文档导入与分块 | 已完成 |
| V2 Naive RAG 问答 | 已完成 |
| V2.5 真实模型接入 | 已完成 |
| **V3 RAG Debug 可观察** | **当前** |
| V4 关键词检索 | 未开始 |

## V3 完成内容

### 轻量级 Context 组装过滤（本次增强）

| 组件 | 说明 |
|------|------|
| `ContextChunkFilter` | 按 score / Top1 差距 / maxChunks 过滤进入 Prompt 的 Chunk |
| `ContextTextBuilder` | 仅拼接 contextChunks 为 Context 文本 |
| `ContextFilterReason` | `SCORE_TOO_LOW` / `SCORE_GAP_TOO_LARGE` / `EXCEED_MAX_CONTEXT_CHUNKS` |

**规则：**

1. `retrievedChunks`：完整 TopK 召回，供 Debug 表格展示
2. `contextChunks`：过滤后实际进入 Prompt 的片段
3. 按 score 降序 → 过滤 `score < minScore` → 过滤 `top1Score - score > maxScoreGap` → 最多 `maxChunks` 个
4. 若全部被过滤，保留 Top1，避免 Context 为空
5. Prompt 与持久化 Context **仅**使用 `contextChunks`

**默认配置（`application.yml`）：**

```yaml
rag:
  context:
    max-chunks: 2
    min-score: 0.45
    max-score-gap: 0.35
```

**示例（短信验证码排查）：**

| 文档 | score | 进入 Prompt | 原因 |
|------|-------|-------------|------|
| 03-troubleshooting.md | 0.82 | 是 | — |
| 02-api-spec.md | 0.62 | 是 | — |
| 01-project-guideline.md | 0.27 | 否 | SCORE_TOO_LOW |

### 后端 debug 模块

| 类型 | 说明 |
|------|------|
| `DebugController` | Debug API |
| `DebugService` | 检索 → Context 过滤 → Prompt → 回答 → 落库 |
| `rag_query_log` / `rag_retrieval_log` | 查询与召回日志 |

`rag_retrieval_log` 新增字段：

- `used_in_prompt`：是否进入 Prompt
- `filter_reason`：未进入时的过滤原因

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/debug/query` | 执行 Debug 查询 |
| GET | `/api/debug/query-logs` | 历史列表 |
| GET | `/api/debug/query-logs/{queryLogId}` | 详情 |

**`POST /api/debug/query` 返回新增/增强：**

- `retrievedChunks[]`：含 `usedInPrompt`、`filterReason`
- `contextChunks[]`：进入 Prompt 的片段
- `context` / `prompt`：仅基于 `contextChunks`

### Prompt 模板（轻微优化）

- 优先使用最相关片段
- 多片段综合回答，不引入无关内容
- 无依据时回答：「知识库中没有找到相关依据。」

### 前端 Debug 页

| 路由 | 说明 |
|------|------|
| `/debug` | 召回表格：是否进入 Prompt、过滤原因；Context/Prompt 仅展示过滤后内容 |
| `/debug/:queryLogId` | 历史详情同上 |

- `frontend/src/utils/contextFilter.ts` — 过滤原因中文标签

### 复用 V2 / V2.5

- `VectorRetrievalService`、`PromptBuilder`、`ChatModelProvider`、`EmbeddingProviderRouter`

> Debug 流程已改用 `ContextChunkFilter`（按分数过滤），不再使用 `ChatRelevanceFilter`（n-gram 过滤）。`/chat` 问答页仍使用 `ChatRelevanceFilter`。

## V3 明确不做

BM25、Elasticsearch、Hybrid Search、Reranker、Query Rewrite、Evaluation、权限、多轮对话

## V3 验收问题

| # | 问题 | Context 预期 |
|---|------|----------------|
| 1 | 短信验证码发不出去怎么排查？ | troubleshooting + api-spec（不含 guideline） |
| 2 | SMS_429 是什么意思？ | api-spec 相关 |
| 3 | send-code 接口路径是什么？ | api-spec 相关 |
| 4 | 为什么后续会使用 PgVector？ | guideline 相关 |
| 5 | 公司年终奖发几个月？ | 可能为空或 Top1（视召回分数） |

## 快速启动

```bash
docker compose up -d
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev
```

- Debug：http://localhost:5173/debug
- 单元测试：`mvn test -Dtest=ContextChunkFilterTest`

## V2.5 配置（Embedding / Chat）

见 `application-dev.yml`：`rag.embedding.provider`、`rag.chat.provider`  
切换 Embedding 后须 `POST /api/kb/{kbId}/embedding/rebuild`

## 下一步（V4）

关键词检索（Elasticsearch / BM25），本版本未引入。
