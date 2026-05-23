# 03 - 接口设计

## 统一响应格式

所有接口返回 `ApiResponse<T>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 200 表示成功 |
| message | string | 提示信息 |
| data | T | 业务数据 |

---

## 系统接口

### GET /api/system/health

| 项 | 说明 |
|----|------|
| **用途** | 健康检查 |
| **版本** | V0 |
| **请求参数** | 无 |
| **返回 data** | `status`（UP）、`appName`、`version`（来自 `rag.app.version`）、`timestamp` |

### GET /api/system/status

| 项 | 说明 |
|----|------|
| **用途** | 系统运行状态看板数据 |
| **版本** | V8-01 |
| **请求参数** | 无 |
| **返回 data** | `backend`、`postgresql`、`elasticsearch`、`modelProvider`、`features[]` |
| **backend** | `status`、`appName`、`version`、`serverTime`、`message` |
| **postgresql** | `name`、`status`（UP/DOWN）、`message` |
| **elasticsearch** | `status`、`indexName`、`message` |
| **modelProvider** | `embeddingProvider`、`embeddingModel`、`chatProvider`、`chatModel` |
| **features 单条** | `key`、`label`、`status`（SUPPORTED） |

依赖检查失败时对应节点 `status=DOWN`，`message` 为异常摘要；接口仍返回 200，便于前端展示。

### GET /api/rag/metrics

| 项 | 说明 |
|----|------|
| **用途** | RAG 运行指标看板（基于 `rag_query_log` 聚合，无新表） |
| **版本** | V8-02 |
| **请求参数** | 无 |
| **返回 data** | `totalQueryCount`、`todayQueryCount`、`avgTotalTimeMs`、`avgRetrievalTimeMs`、`avgGenerationTimeMs`、`searchModeStats`、`rerankStats`、`slowQueries[]` |
| **searchModeStats** | `vectorCount`、`bm25Count`、`hybridCount` |
| **rerankStats** | `enabledCount`、`disabledCount` |
| **slowQueries 单条** | `queryLogId`、`question`、`searchMode`、`enableRerank`、`totalTimeMs`、`createdAt` |

慢查询按 `total_time_ms` 降序取 Top 10；详情页跳转已有 `/debug/{queryLogId}`。

### GET /api/rag/query-logs

| 项 | 说明 |
|----|------|
| **用途** | RAG 查询日志中心分页列表（基于 `rag_query_log`） |
| **版本** | V8-03 |
| **请求参数** | `page`（默认 1）、`pageSize`（默认 10）、`keyword`（问题模糊搜索）、`searchMode`（VECTOR/BM25/HYBRID）、`enableRerank`（true/false）、`slowOnly`（true 时 `total_time_ms > 3000`） |
| **返回 data** | `total`、`page`、`pageSize`、`records[]` |
| **records 单条** | `queryLogId`、`question`、`searchMode`、`enableRerank`、`totalTimeMs`、`retrievalTimeMs`、`generationTimeMs`、`createdAt` |

详情跳转已有 `GET /api/debug/query-logs/{queryLogId}` 与 `/debug/{queryLogId}` 页面。

### GET /api/rag/query-logs/slow-analysis

| 项 | 说明 |
|----|------|
| **用途** | 慢查询分析与规则化优化建议 |
| **版本** | V8-04 |
| **请求参数** | `limit`（可选，默认 50，最大 100） |
| **慢查询判定** | `totalTimeMs >= 3000` 或 `retrievalTimeMs >= 1000` 或 `generationTimeMs >= 2000` |
| **返回 data** | `totalCount`（符合慢查询规则的总数）、`records[]` |
| **records 单条** | `queryLogId`、`question`、`searchMode`、`enableRerank`、各耗时、`retrievalChunkCount`（来自 `rag_retrieval_log`）、`slowReasons[]`、`suggestions[]`、`createdAt` |

---

## 知识库接口

### POST /api/kb

| 项 | 说明 |
|----|------|
| **用途** | 创建知识库 |
| **版本** | V1 |
| **请求 Body** | `name`（必填）、`description`（可选） |
| **返回 data** | `id`、`name`、`description`、`status`、`createdAt`、`updatedAt` |

### GET /api/kb

| 项 | 说明 |
|----|------|
| **用途** | 知识库列表 |
| **版本** | V1 |
| **请求参数** | 无 |
| **返回 data** | `KnowledgeBaseResponse[]` |

### GET /api/kb/{id}

| 项 | 说明 |
|----|------|
| **用途** | 知识库详情 |
| **版本** | V1 |
| **路径参数** | `id` |
| **返回 data** | 同创建响应 |

### DELETE /api/kb/{id}

| 项 | 说明 |
|----|------|
| **用途** | 删除知识库（逻辑删除） |
| **版本** | V1 |
| **路径参数** | `id` |
| **返回 data** | null |

---

## 文档接口

### POST /api/kb/{kbId}/documents/upload

| 项 | 说明 |
|----|------|
| **用途** | 上传 Markdown/TXT，解析并分块 |
| **版本** | V1 |
| **路径参数** | `kbId` |
| **请求** | `multipart/form-data`，字段 `file` |
| **返回 data** | `DocumentResponse`：`id`、`kbId`、`fileName`、`fileType`、`fileSize`、`contentHash`、`status`、`errorMessage`、`createdAt`、`updatedAt` |

### GET /api/kb/{kbId}/documents

| 项 | 说明 |
|----|------|
| **用途** | 知识库下文档列表 |
| **版本** | V1 |
| **路径参数** | `kbId` |
| **返回 data** | `DocumentResponse[]` |

### GET /api/documents/{documentId}

| 项 | 说明 |
|----|------|
| **用途** | 文档详情 |
| **版本** | V1 |
| **路径参数** | `documentId` |
| **返回 data** | `DocumentResponse` |

### GET /api/documents/{documentId}/chunks

| 项 | 说明 |
|----|------|
| **用途** | 文档 Chunk 列表 |
| **版本** | V1 |
| **路径参数** | `documentId` |
| **返回 data** | `DocumentChunkResponse[]`：`id`、`kbId`、`documentId`、`chunkIndex`、`titlePath`、`content`、`tokenCount`、`contentHash`、`createdAt` |

---

## 样例数据接口

### POST /api/sample/init

| 项 | 说明 |
|----|------|
| **用途** | 一键初始化样例知识库与 3 个 Markdown 文档 |
| **版本** | V1 |
| **请求参数** | 无 |
| **返回 data** | `initialized`、`knowledgeBaseId`、`documentCount`、`message` |

### GET /api/sample/status

| 项 | 说明 |
|----|------|
| **用途** | 查询样例是否已初始化 |
| **版本** | V1 |
| **返回 data** | 样例状态字段（是否已初始化、kbId 等） |

---

## Embedding 接口

### POST /api/kb/{kbId}/embedding/rebuild

| 项 | 说明 |
|----|------|
| **用途** | 对知识库下全部 Chunk 重新向量化 |
| **版本** | V2 |
| **路径参数** | `kbId` |
| **返回 data** | 重建结果（处理条数、模型信息等） |

### GET /api/kb/{kbId}/embedding/status

| 项 | 说明 |
|----|------|
| **用途** | 向量化进度统计 |
| **版本** | V2 |
| **路径参数** | `kbId` |
| **返回 data** | `totalChunks`、`embeddedChunks`、`notEmbeddedChunks` |

---

## Chat 接口

### POST /api/chat

| 项 | 说明 |
|----|------|
| **用途** | Naive RAG 问答（**仅向量检索**，未接 BM25） |
| **版本** | V2 |
| **请求 Body** | `kbId`（必填）、`question`（必填）、`sessionId`（可选）、`topK`（默认 5） |
| **返回 data** | `sessionId`、`answer`、`sources[]`（`documentId`、`documentName`、`chunkId`、`chunkIndex`、`score`、`content`） |

### GET /api/chat/sessions

| 项 | 说明 |
|----|------|
| **用途** | 会话列表 |
| **版本** | V2 |
| **Query** | `kbId`（可选） |
| **返回 data** | `ChatSessionResponse[]`：`id`、`kbId`、`title`、`createdAt`、`updatedAt` |

### GET /api/chat/sessions/{sessionId}/messages

| 项 | 说明 |
|----|------|
| **用途** | 会话消息列表 |
| **版本** | V2 |
| **路径参数** | `sessionId` |
| **返回 data** | `ChatMessageResponse[]`：`id`、`sessionId`、`role`、`content`、`sourceChunks`、`latencyMs`、`createdAt` 等 |

---

## 模型 Provider 接口

### GET /api/model/providers

| 项 | 说明 |
|----|------|
| **用途** | 当前生效的 Embedding / Chat Provider 与模型信息 |
| **版本** | V2.5 |
| **返回 data** | `embeddingProvider`、`embeddingModel`、`embeddingDimension`、`embeddingDelegate`、`chatProvider`、`chatModel`、`chatDelegate` |

---

## Debug 接口

### POST /api/debug/query

| 项 | 说明 |
|----|------|
| **用途** | 执行完整 Debug 查询（召回 → Context 过滤 → Prompt → 回答） |
| **版本** | V3（`searchMode` 为 V4 扩展） |
| **请求 Body** | `kbId`（必填）、`question`（必填）、`topK`（默认 5）、`searchMode`（`VECTOR` 默认 / `BM25` / `HYBRID`）、`enableRerank`（可选，默认 false，V6） |
| **返回 data** | `queryLogId`、`kbId`、`question`、`searchMode`、`enableRerank`、`embeddingProvider`、`embeddingModel`、`chatProvider`、`chatModel`、`retrievedChunks[]`、`contextChunks[]`、`context`、`prompt`、`answer`、`latency`（`retrievalTimeMs`、`generationTimeMs`、`totalTimeMs`） |

**retrievedChunks / contextChunks 单条字段：**

`documentId`、`documentName`、`chunkId`、`chunkIndex`、`score`、`rankPosition`、`content`、`usedInPrompt`、`filterReason`（`SCORE_TOO_LOW` / `SCORE_GAP_TOO_LARGE` / `EXCEED_MAX_CONTEXT_CHUNKS`）

**HYBRID 模式下 `retrievedChunks` 额外字段（V5，用于融合可观察性）：**

| 字段 | 说明 |
|------|------|
| `matchedByVector` | 是否出现在向量召回路 |
| `matchedByBm25` | 是否出现在 BM25 召回路 |
| `vectorScore` | 向量原始相似度 |
| `bm25Score` | BM25 分数（融合前已按 Top1 归一化到 [0,1]） |
| `hybridScore` | RRF 融合分；`score` 字段同 `hybridScore` |

VECTOR / BM25 模式下上述字段为 `null`。历史详情若从 DB 回放且无持久化融合字段，详情页仅展示通用列。

**启用 Reranker 时（`enableRerank=true`，V6）`retrievedChunks` 额外字段：**

| 字段 | 说明 |
|------|------|
| `originalRank` | 检索原始排名（1-based） |
| `rerankRank` | 重排后排名（1-based）；`rankPosition` 同此值 |
| `rerankScore` | 本地规则重排分（关键词 + 专有词命中） |

未启用重排时上述字段为 `null`。重排后 Context 过滤 / Prompt / Answer 基于重排顺序。

**排名变化（V6-02，前端计算）：** `originalRank - rerankRank`，正数为上升（如 `+2`），负数为下降，0 为不变。

**历史回放（V6-02）：** `enableRerank` 写入 `rag_query_log.enable_rerank`；各条 `original_rank` / `rerank_rank` / `rerank_score` 写入 `rag_retrieval_log`。V6-02 之前的历史无上述字段，详情接口仍返回 200，前端隐藏重排列。

### GET /api/debug/query-logs

| 项 | 说明 |
|----|------|
| **用途** | Debug 查询历史列表 |
| **版本** | V3 |
| **Query** | `kbId`（可选） |
| **返回 data** | 摘要列表（含 `queryLogId`、`question`、`createdAt` 等） |

### GET /api/debug/query-logs/{queryLogId}

| 项 | 说明 |
|----|------|
| **用途** | 单次 Debug 查询详情 |
| **版本** | V3 |
| **路径参数** | `queryLogId` |
| **返回 data** | 同 `POST /api/debug/query` 完整结构 |

---

## Evaluation 接口（V7）

### GET /api/evaluation/cases

| 项 | 说明 |
|----|------|
| **用途** | 返回内置评测用例列表 |
| **版本** | V7 |
| **返回 data** | `caseId`、`question`、`expectedDocument` |

### POST /api/evaluation/run

| 项 | 说明 |
|----|------|
| **用途** | 按检索模式批量执行内置用例，校验 Top1 文档是否命中期望 |
| **版本** | V7 |
| **请求 Body** | `kbId`（必填）、`searchMode`（`VECTOR` 默认 / `BM25` / `HYBRID`）、`topK`（默认 5）、`enableRerank`（可选，默认 false，V7-02） |
| **返回 data** | `kbId`、`searchMode`、`enableRerank`、`totalCount`、`passedCount`、`failedCount`、`passRate`（0～1）、`totalLatencyMs`、`avgLatencyMs`、`results[]` |
| **results 单条** | `caseId`、`question`、`expectedDocument`、`actualTop1Document`、`passed`、`searchMode`、`latencyMs`、`message` |

**通过规则：** `actualTop1Document` 包含 `expectedDocument` 子串（与 V4 冒烟脚本一致）。无召回时 `passed=false`。

**Reranker（V7-02）：** `enableRerank=true` 时先检索再经 `RerankService` 重排后取 Top1 判定。

### POST /api/evaluation/compare

| 项 | 说明 |
|----|------|
| **用途** | 一次性对 VECTOR / BM25 / HYBRID 执行内置用例评测并返回对比结果 |
| **版本** | V7-02 |
| **请求 Body** | `kbId`（必填）、`topK`（默认 5）、`enableRerank`（可选，默认 false） |
| **返回 data** | `kbId`、`enableRerank`、`modeSummaries[]`、`cases[]` |
| **modeSummaries 单条** | `searchMode`、`totalCount`、`passedCount`、`failedCount`、`passRate`、`totalLatencyMs`、`avgLatencyMs` |
| **cases 单条** | `caseId`、`question`、`expectedDocument`、`vector` / `bm25` / `hybrid`（各含 `actualTop1Document`、`passed`、`latencyMs`） |

---

## V4 Search 接口

### POST /api/search/index/rebuild

| 项 | 说明 |
|----|------|
| **用途** | 从 `document_chunk` 全量重建 ES 索引 `rag_document_chunk` |
| **版本** | V4 |
| **请求参数** | 无 |
| **返回 data** | `indexName`、`syncedCount` |

### POST /api/search/bm25

| 项 | 说明 |
|----|------|
| **用途** | BM25 关键词检索（不经过 Chat） |
| **版本** | V4 |
| **请求 Body** | `kbId`（必填）、`query`（必填）、`topK`（默认 5） |
| **返回 data** | `query`、`extractedTerms[]`、`results[]` |

**results 单条：**

| 字段 | 说明 |
|------|------|
| documentId | 文档 ID |
| documentName | 文档名 |
| chunkId | Chunk ID |
| chunkIndex | 块序号 |
| score | BM25 相关分数 |
| content | 正文 |
| matchedTerms | 与查询抽取词在 content 中的匹配项 |

**BM25 查询结构（bool should + filter kbId，`minimum_should_match=1`）：**

| 子句 | boost |
|------|-------|
| `terms` 精确匹配 `terms` 字段 | 20 |
| `content` match_phrase（每个抽取词） | 15 |
| `content` match 全句，`operator=and` | 2 |
| `content` match 全句，`minimum_should_match=70%` | 1 |

**请求示例：**

```json
{
  "kbId": 1,
  "query": "SMS_429 是什么意思？",
  "topK": 5
}
```

---

## 补充说明（已有但未列入最小清单的接口）

| 方法 | 路径 | 版本 | 说明 |
|------|------|------|------|
| GET | `/api/dashboard/stats` | V1 | Dashboard 统计 |
| POST | `/api/retrieval/test` | V2 | 向量检索测试（开发调试用） |

Knife4j 文档：http://localhost:8080/doc.html

HTTP 用例文件：`http/` 目录下 `*.http`
