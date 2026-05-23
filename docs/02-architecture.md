# 02 - 架构设计

## 当前整体架构

采用**前后端分离**的单体应用架构：

```text
┌─────────────┐     HTTP/JSON      ┌──────────────────────────────┐
│  Vue 3 前端  │ ◄──────────────► │  Spring Boot 后端（单体）      │
│  Vite :5173 │                    │  :8080                       │
└─────────────┘                    └───────────┬──────────────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    ▼                          ▼                          ▼
            ┌───────────────┐          ┌───────────────┐          ┌───────────────┐
            │  PostgreSQL   │          │   PgVector    │          │ Elasticsearch │
            │  元数据 + 日志 │          │  chunk_embedding│        │ rag_document_chunk │
            └───────────────┘          └───────────────┘          └───────────────┘
                                               │
                    ┌──────────────────────────┴──────────────────────────┐
                    ▼                          ▼                          ▼
            MockEmbeddingProvider      QwenEmbeddingProvider      MockChatModelProvider
            (确定性 n-gram 哈希)        (DashScope)               DeepSeekChatModelProvider
```

- **PostgreSQL**：知识库、文档、Chunk、会话、Debug 日志等关系数据。
- **PgVector**：`chunk_embedding` 表存储向量，支撑向量 TopK 检索。
- **Elasticsearch**：V4 起存储 Chunk 检索副本，支撑 BM25。
- **外部 API**：Embedding（Qwen）、Chat（DeepSeek），经 Provider 路由切换。

当前为**单体 Maven 工程**，不按模块拆成多个 Maven artifact；包内按 `module/*` 划分领域，后续是否拆 Maven 多模块待明确需要时再定。

---

## 后端模块结构

```text
com.guan.rag
├── common                 # ApiResponse、异常、工具
├── config                 # RagProperties、ES Client、MyBatis
├── controller             # System、Dashboard、Model
└── module
    ├── kb                 # 知识库
    ├── document           # 上传、解析、分块
    ├── embedding          # 向量化与重建
    ├── retrieval          # VectorRetrievalService
    ├── chat               # 问答、Prompt、Context 过滤（Chat 路径）
    ├── debug              # Debug 全链路可观察
    ├── search             # EsIndexService、Bm25SearchService（V4）；V5 Hybrid 融合与编排（规划）
    └── sample             # 样例数据初始化
```

---

## 前端模块结构

```text
frontend/src
├── api/                   # 按领域封装的 Axios API
├── layouts/               # BasicLayout
├── router/                # 路由
├── views/
│   ├── DashboardView      # V0/V1 统计
│   ├── KbListView         # 知识库列表
│   ├── DocumentListView   # 文档列表
│   ├── ChunkListView      # Chunk 列表
│   ├── ChatView           # V2 问答
│   ├── DebugView          # V3/V4/V5 Debug（含 searchMode：VECTOR / BM25 / HYBRID）
│   ├── DebugDetailView    # 历史详情
│   └── AboutView          # 版本说明
└── stores/                # Pinia（如有）
```

**主要路由：**

| 路径 | 页面 | 版本 |
|------|------|------|
| `/dashboard` | Dashboard | V0/V1 |
| `/kb` | 知识库列表 | V1 |
| `/kb/:kbId/documents` | 文档列表 | V1 |
| `/documents/:documentId/chunks` | Chunk 列表 | V1 |
| `/chat` | 问答 | V2 |
| `/debug` | RAG Debug | V3/V4/V5 |
| `/debug/:queryLogId` | Debug 历史详情 | V3 |
| `/about` | 关于 | - |

---

## RAG 主流程

### Chat 问答（`/api/chat`，向量检索）

```text
用户问题 → Embedding → PgVector TopK → ChatRelevanceFilter
         → PromptBuilder → ChatModelProvider → 回答 + sources
```

### Debug 查询（`/api/debug/query`）

**V4 已实现：**

```text
用户问题 → [VECTOR: 向量检索 | BM25: ES 检索 + 分数归一化]
         → retrievedChunks（全量展示）
         → ContextChunkFilter → contextChunks
         → PromptBuilder → ChatModelProvider → answer
         → 写入 rag_query_log / rag_retrieval_log
```

**V5 目标（当前版本，待实现）：**

```text
用户问题
  → VectorRetrievalService（向量召回）
  → Bm25SearchService（关键词召回）
  → Hybrid 融合服务（应用层融合排序，如 RRF；不新增独立存储）
  → retrievedChunks（全量展示，含融合后 score）
  → ContextChunkFilter
  → PromptBuilder
  → ChatModelProvider
  → answer
  → Debug 展示（searchMode=HYBRID）
```

V5 **复用**现有 `retrieval`、`search`、`debug` 模块；融合逻辑为**应用层服务**，不新增 PostgreSQL 表、不新增 ES 索引。

**Context 过滤配置（`application.yml`）：**

```yaml
rag:
  context:
    max-chunks: 2
    min-score: 0.45
    max-score-gap: 0.35
```

BM25 模式下先将原始 BM25 分数按 Top1 归一化到 [0,1]，再应用上述规则。

### V4 BM25 独立检索（`/api/search/bm25`）

```text
query → SearchTermExtractor 抽取专有词
      → ES bool should（terms 精确 + content match_phrase + content match 降权）
      → filter kbId → TopK 结果
```

---

## V0 到 V5 架构演进

| 版本 | 架构增量 | 状态 |
|------|----------|------|
| V0 | 前后端骨架、健康检查、Dashboard 占位 | 已完成 |
| V1 | PostgreSQL 业务表；文档解析分块流水线 | 已完成 |
| V2 | PgVector + Embedding + Chat 闭环 | 已完成 |
| V2.5 | Provider 抽象；Qwen / DeepSeek 真实 API | 已完成 |
| V3 | Debug 可观察；双轨召回 vs Context；查询日志表 | 已完成 |
| V4 | Elasticsearch 索引副本；BM25 检索；Debug `searchMode` VECTOR/BM25 | 已完成 |
| **V5** | **Vector + BM25 应用层融合；Debug `searchMode` HYBRID** | **当前版本** |

---

## 已接入组件与 Provider

| 组件 | 说明 | 版本 |
|------|------|------|
| PostgreSQL | 关系型存储 | V0+ |
| PgVector | `vector` 类型与相似度检索 | V2+ |
| Elasticsearch | 索引 `rag_document_chunk` | V4 |
| MockEmbeddingProvider | 确定性字符 n-gram 哈希向量，L2 归一化 | V2 |
| QwenEmbeddingProvider | DashScope `text-embedding-v4` 等 | V2.5 |
| MockChatModelProvider | 本地 Mock 回答 | V2 |
| DeepSeekChatModelProvider | DeepSeek Chat API | V2.5 |

Provider 路由：`EmbeddingProviderRouter`、`ChatModelProviderRouter`，配置见 `application-dev.yml`。

---

## 配置与环境

- 连接信息优先来自 **`.env`**（POSTGRES_*、ES_*、API Key）。
- `docker-compose.yml` 为可选本地 PG/ES，非强制。
- ES 索引名默认 `rag_document_chunk`（`rag.elasticsearch.index`）。

---

## 当前版本：V5 Hybrid Search（架构要点）

| 要点 | 说明 |
|------|------|
| 融合位置 | 应用层服务（如 `HybridFusionService` + `HybridSearchService` 编排），**非**独立存储引擎 |
| 复用模块 | `module/retrieval`（向量）、`module/search`（BM25）、`module/debug`（可观察链路） |
| 数据复用 | `document_chunk`、`chunk_embedding`、ES `rag_document_chunk`、`rag_query_log` / `rag_retrieval_log` |
| 融合结果 | 查询链路内实时计算，**暂不落库** |
| 不做 | Reranker、Evaluation、Query Rewrite、权限、多租户、多轮对话 |

## 后续架构方向（V6+，未实现）

- **V6+**：Reranker、评测、工程化等待对应版本再设计，**本文档不展开 V6 Reranker 详细架构**。
