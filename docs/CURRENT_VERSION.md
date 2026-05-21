# CURRENT_VERSION.md

## 当前版本

V2：Naive RAG 问答版（已实现）

## 上一版本状态

V1：文档导入与分块版 — 已完成

## 当前目标（流水线）

当前版本实现以下 Naive RAG 流水线：

```text
Chunk
  ↓
Embedding
  ↓
向量存储（PgVector / chunk_embedding）
  ↓
用户提问
  ↓
向量检索（VectorRetrievalService）
  ↓
Prompt 拼接（PromptBuilder）
  ↓
LLM 回答（ChatModelProvider）
  ↓
返回引用来源
```

## V2 已实现能力

### 后端

- `embedding` 模块：Chunk 向量化、重建向量、向量化状态查询
- `retrieval` 模块：PgVector 基础向量检索（TopK）
- `chat` 模块：Naive RAG 问答、会话与消息持久化
- Embedding / Chat Provider 可配置（默认 `mock`，支持 `openai` 占位）
- 新增表：`chunk_embedding`、`chat_session`、`chat_message`
- 新增 API：
  - `POST /api/kb/{kbId}/embedding/rebuild`
  - `GET /api/kb/{kbId}/embedding/status`
  - `POST /api/chat`
  - `GET /api/chat/sessions`
  - `GET /api/chat/sessions/{sessionId}/messages`

### 前端

- `/chat` 问答页：选择知识库、向量化状态、重建向量、提问、展示回答与引用来源
- Dashboard 增加：已向量化 Chunk 数、Chat 会话数、Chat 消息数

### 配置

- `rag.embedding.*`：Embedding Provider（默认 mock）
- `rag.chat.*`：Chat Provider（默认 mock）
- Docker PostgreSQL 镜像：`pgvector/pgvector:pg16`

## V2 允许

### 向量化与存储

- Embedding
- PgVector
- Chunk 向量化

### 检索与问答

- 基础向量检索
- Chat 问答
- Prompt 构造
- LLM 调用

### 结果展示

- 引用来源

## V2 禁止（未实现）

- BM25
- Elasticsearch
- Hybrid Search
- Reranker
- Query Rewrite
- Debug Console
- Evaluation
- 权限控制
- 多轮问题重构

## 使用说明

1. 启动带 pgvector 的 PostgreSQL（`docker compose up -d`）
2. 启动后端与前端
3. 在 V1 流程中创建知识库并导入文档 / 初始化样例数据
4. 打开 `/chat`，选择知识库，点击「重建向量」
5. 输入问题并发送，查看回答与引用来源

## 切换真实模型（可选）

在 `application.yml` 或环境变量中配置：

```yaml
rag:
  embedding:
    provider: openai   # 需配置 api-key；当前为占位实现
  chat:
    provider: openai     # 需配置 api-key；当前为占位实现
```

无 API Key 时请保持 `provider: mock`，以保证项目可启动与演示。
