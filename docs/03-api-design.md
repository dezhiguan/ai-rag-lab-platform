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
| **返回 data** | `status`（UP）、`appName`、`version`、`timestamp` |

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
| **请求 Body** | `kbId`（必填）、`question`（必填）、`topK`（默认 5）、`searchMode`（`VECTOR` 默认 / `BM25`） |
| **返回 data** | `queryLogId`、`kbId`、`question`、`searchMode`、`embeddingProvider`、`embeddingModel`、`chatProvider`、`chatModel`、`retrievedChunks[]`、`contextChunks[]`、`context`、`prompt`、`answer`、`latency`（`retrievalTimeMs`、`generationTimeMs`、`totalTimeMs`） |

**retrievedChunks / contextChunks 单条字段：**

`documentId`、`documentName`、`chunkId`、`chunkIndex`、`score`、`rankPosition`、`content`、`usedInPrompt`、`filterReason`（`SCORE_TOO_LOW` / `SCORE_GAP_TOO_LARGE` / `EXCEED_MAX_CONTEXT_CHUNKS`）

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
