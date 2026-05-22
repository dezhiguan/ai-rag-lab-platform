# CURRENT_VERSION.md

## 当前版本

**V3：RAG Debug 可观察版**

在 V2 Naive RAG 与 V2.5 真实模型接入基础上，新增 Debug 查询与历史记录，让用户在前端看到一次 RAG 问答的完整内部过程。

## 版本关系

| 版本 | 状态 |
|------|------|
| V1 文档导入与分块 | 已完成 |
| V2 Naive RAG 问答 | 已完成 |
| V2.5 真实模型接入 | 已完成 |
| **V3 RAG Debug 可观察** | **当前** |
| V4 关键词检索 | 未开始 |

## V3 完成内容

### 后端 debug 模块

包路径：`com.guan.rag.module.debug`

| 类型 | 说明 |
|------|------|
| `DebugController` | Debug API |
| `DebugService` | 检索 → Context → Prompt → 回答 → 落库 |
| `DebugQueryLog` / `DebugRetrievalLog` | 实体 |
| `DebugQueryLogMapper` / `DebugRetrievalLogMapper` | MyBatis-Plus |

### 数据库表

| 表 | 说明 |
|----|------|
| `rag_query_log` | Debug 查询主记录（问题、Context、Prompt、回答、Provider、耗时） |
| `rag_retrieval_log` | 单次查询的召回 Chunk 明细（分数、排名、内容） |

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/debug/query` | 执行 Debug 查询，返回完整过程 |
| GET | `/api/debug/query-logs` | 最近 Debug 查询列表（可选 `kbId`） |
| GET | `/api/debug/query-logs/{queryLogId}` | 单次 Debug 查询详情 |

### Debug 查询返回内容

1. 原始问题
2. Embedding Provider / Model
3. Chat Provider / Model
4. 召回 Chunk（含相似度、排名）
5. 注入 Prompt 的 Context
6. 完整 Prompt
7. 模型回答
8. 检索 / 生成 / 总耗时

### 前端

| 路由 | 说明 |
|------|------|
| `/debug` | Debug 查询页（执行查询、展示过程、历史列表） |
| `/debug/:queryLogId` | 单次 Debug 查询详情 |

- `frontend/src/api/debug.ts`
- `frontend/src/types/debug.ts`

### 复用 V2 / V2.5 能力

- `VectorRetrievalService` 向量检索
- `ChatRelevanceFilter` 相关性过滤（Context 与 Chat 一致）
- `PromptBuilder` Prompt 构造
- `ChatModelProvider` / `EmbeddingProviderRouter` 模型调用与 Provider 信息

## V3 允许内容

- Debug 查询接口
- 召回 Chunk 展示
- Context 展示
- Prompt 展示
- Answer 展示
- Provider 信息展示
- 耗时统计
- Debug 查询历史

## V3 明确不做

- BM25
- Elasticsearch
- Hybrid Search
- Reranker
- Query Rewrite
- Evaluation
- 权限
- 多轮对话
- 新增真实模型 Provider（继续使用 V2.5 已有 Provider）

**不要**为上述功能创建空类、空接口、空页面、空表。

## V3 验收问题

在 `/debug` 页面可测试：

| # | 问题 |
|---|------|
| 1 | 短信验证码发不出去怎么排查？ |
| 2 | SMS_429 是什么意思？ |
| 3 | send-code 接口路径是什么？ |
| 4 | 这个项目为什么后续会使用 PgVector？ |
| 5 | 公司年终奖发几个月？ |

## 快速启动

```bash
docker compose up -d
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev
```

- Debug 页：http://localhost:5173/debug
- 问答页：http://localhost:5173/chat
- Swagger：http://localhost:8080/doc.html
- Provider：`GET http://localhost:8080/api/model/providers`

## V2.5 配置说明（保持不变）

见 `application-dev.yml` 与 `.env.example`：

- `DASHSCOPE_API_KEY` — Qwen Embedding
- `DEEPSEEK_API_KEY` — DeepSeek Chat

切换 Embedding Provider / 模型后须对每个知识库执行 `POST /api/kb/{kbId}/embedding/rebuild`。

## 下一步（V4，未开始）

关键词检索版：Elasticsearch、BM25（本版本未引入）。
