# CURRENT_VERSION.md

## 当前版本

**V2：Naive RAG 问答版 — 收尾完成（Mock Embedding + Mock Chat）**

> 本阶段 **不接** DeepSeek / Qwen / OpenAI 真实 Embedding 与 LLM，**不进入 V3**。

## 上一版本

V1：文档导入与分块版 — 已完成

## V2 完成状态

| 模块 | 状态 | 说明 |
|------|------|------|
| MockEmbeddingProvider | 已完成 | 默认 provider，`mock`，384 维，确定性 n-gram + sqrt(tf) + L2 |
| Chunk 向量化 / 重建 / 状态 | 已完成 | 重建时清空旧向量；标题行双重加权 |
| PgVector 检索 | 已完成 | cosine distance，`ORDER BY distance ASC`，score = 1 - distance |
| POST /api/retrieval/test | 已完成 | 仅 TopK，无阈值过滤 |
| POST /api/chat | 已完成 | answer + sources + 会话持久化 |
| PromptBuilder | 已完成 | 固定 Prompt 模板 |
| MockChatModelProvider | 已完成 | 基于上下文生成回答；无关问题返回固定文案 |
| 前端 /chat | 已完成 | 向量化状态、重建、验收快捷问题、引用来源 |
| 单元测试 | 已完成 | `V2RetrievalRankingTest` 覆盖 4 条验收检索排序 |

## 默认 Provider 配置

```yaml
rag:
  embedding:
    provider: mock          # 默认，不接 DeepSeek Embedding
    model: mock-embedding-ngram
    dimension: 384
  chat:
    provider: mock          # 默认，不接真实 LLM
    model: mock-chat
```

启动日志会输出当前激活的 Provider 实现类（`RagProviderConfig`）。

## 流水线

```text
Chunk → Mock Embedding → PgVector (chunk_embedding)
  → 用户提问 → 向量 TopK → Chat 相关性过滤 → Prompt → Mock Chat → answer + sources
```

## 接口清单

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/api/kb/{kbId}/embedding/rebuild` | 清空并重建知识库向量 |
| GET | `/api/kb/{kbId}/embedding/status` | 向量化状态 |
| POST | `/api/retrieval/test` | V2 检索基线验证（非 Debug Console） |
| POST | `/api/chat` | Naive RAG 问答 |
| GET | `/api/chat/sessions` | 会话列表 |
| GET | `/api/chat/sessions/{id}/messages` | 会话消息 |

### POST /api/chat 请求示例

```json
{
  "kbId": 1,
  "sessionId": null,
  "question": "短信验证码发不出去怎么排查？",
  "topK": 5
}
```

### 响应示例

```json
{
  "sessionId": 1001,
  "answer": "根据知识库内容：...",
  "sources": [
    {
      "documentId": 14,
      "documentName": "03-troubleshooting.md",
      "chunkId": 23,
      "chunkIndex": 0,
      "score": 0.42,
      "content": "..."
    }
  ]
}
```

## 前端页面

| 路由 | 文件 | 功能 |
|------|------|------|
| `/chat` | `ChatView.vue` | 知识库选择、向量化状态、重建向量、TopK、V2 验收快捷问题、回答与引用来源 |

API：`src/api/embedding.ts`、`src/api/chat.ts`

## V2 验收用例（需先重建向量）

使用 `http/retrieval-test.http` 或 `http/chat-test.http`，或在 `/chat` 点击快捷按钮。

| # | 问题 | retrieval Top1 | chat 预期 |
|---|------|----------------|-----------|
| 1 | 短信验证码发不出去怎么排查？ | `03-troubleshooting.md` | 有回答 + sources |
| 2 | SMS_429 是什么意思？ | `02-api-spec.md` | 有回答 + sources |
| 3 | send-code 接口路径是什么？ | `02-api-spec.md` | 有回答 + sources |
| 4 | 这个项目为什么后续会使用 PgVector？ | `01-project-guideline.md` | 有回答 + sources |
| 5 | 公司年终奖发几个月？ | 任意 TopK | **知识库中没有找到相关依据。** |

验收步骤：

1. `docker compose up -d`（`pgvector/pgvector:pg16`）
2. 启动后端 / 前端
3. V1 样例初始化或上传文档
4. **重启后端**（代码变更后必须重启，否则仍使用旧 Embedding 逻辑）
5. `/chat` → **重建向量**
6. 执行上表 5 条问题验证

> 若 Top1 文档不符合预期，请确认已重启后端并重新 rebuild；可用 `mvn test -Dtest=V2RetrievalRankingTest` 在本地验证 Mock 排序。

## V2 明确不做

BM25、Elasticsearch、Hybrid Search、Reranker、Query Rewrite、Debug Console、Evaluation、权限、多轮对话、DeepSeek/Qwen 真实 Embedding

## 下一步建议（V3，不在本阶段）

1. RAG Debug 可观察版：召回列表、Prompt、耗时、问答日志 UI
2. 可选接入稳定官方 Embedding / Chat API（非 DeepSeek Embedding）
3. 检索可视化（仍不引入 BM25 / Hybrid / Reranker）

## 快速启动

```bash
docker compose up -d
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev
```

- 前端：http://localhost:5173/chat  
- Swagger：http://localhost:8080/doc.html
