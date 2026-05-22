# CURRENT_VERSION.md

## 当前版本

**V2.5：真实模型接入版**

在 V2 Naive RAG 问答版基础上，保留 Mock Provider，新增 Qwen Embedding 与 DeepSeek Chat，并支持配置切换。

> **暂停 V3**。本版本不接 BM25 / Hybrid / Reranker / Debug Console / Evaluation。

## 版本关系

| 版本 | 状态 |
|------|------|
| V1 文档导入与分块 | 已完成 |
| V2 Naive RAG 问答 | 已完成 |
| **V2.5 真实模型接入** | **当前** |
| V3 RAG Debug | 未开始 |

## V2.5 完成内容

### Provider 体系

| 类型 | 配置值 | 实现类 | 说明 |
|------|--------|--------|------|
| Embedding | `mock`（默认） | `MockEmbeddingProvider` | 确定性 n-gram，无 API Key 可运行 |
| Embedding | `qwen` | `QwenEmbeddingProvider` | DashScope OpenAI-compatible `/embeddings` |
| Chat | `mock`（默认） | `MockChatModelProvider` | 本地演示回答 |
| Chat | `deepseek` | `DeepSeekChatModelProvider` | DeepSeek `/chat/completions` |

路由：

- `EmbeddingProviderRouter`（`@Primary`）→ `rag.embedding.provider`
- `ChatModelProviderRouter`（`@Primary`）→ `rag.chat.provider`

不支持的 provider 会抛出明确 `BusinessException`（不会静默回退 mock）。

### 新增接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/model/providers` | 查看当前 Embedding / Chat Provider 配置 |

### 向量一致性

- `POST /api/kb/{kbId}/embedding/rebuild`：重建前**删除**该库全部 `chunk_embedding`，再用**当前** Provider 写入
- 检索 / 问答前校验：若库内向量 `model` 或 `dimension` 与当前配置不一致 → 明确错误，提示重建
- Chunk 与 Query **共用**同一 `EmbeddingProvider`（经 Router）

### 前端

- `/chat` 展示：Embedding Provider / Model / Dimension、Chat Provider / Model
- 重建向量旁提示：切换 Embedding Provider 或模型后须重建
- `frontend/src/api/model.ts`

## 配置说明

### application-dev.yml（推荐）

```yaml
rag:
  embedding:
    provider: mock          # mock | qwen
    model: mock-embedding # qwen 示例: text-embedding-v3
    dimension: 384          # qwen 常用: 1024（须与模型一致）
    api-key: ${DASHSCOPE_API_KEY:}
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  chat:
    provider: mock          # mock | deepseek
    model: mock-chat        # deepseek 示例: deepseek-chat
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com
```

### 环境变量（见 `.env.example`）

- `DASHSCOPE_API_KEY` — 通义 / DashScope Embedding
- `DEEPSEEK_API_KEY` — DeepSeek Chat

**不要把 API Key 写死在代码或提交到 Git。**

## 如何切换 Provider

### 1. 仅 Mock（默认，无需 Key）

```yaml
rag.embedding.provider: mock
rag.chat.provider: mock
```

启动后即可使用 V2 全部能力。

### 2. Qwen Embedding

```yaml
rag.embedding.provider: qwen
rag.embedding.model: text-embedding-v3
rag.embedding.dimension: 1024   # 与模型输出维度一致
```

设置环境变量 `DASHSCOPE_API_KEY`，**重启后端**，对每个知识库执行 **重建向量**。

### 3. DeepSeek Chat

```yaml
rag.chat.provider: deepseek
rag.chat.model: deepseek-chat
```

设置环境变量 `DEEPSEEK_API_KEY`，重启后端。Chat 使用真实模型；Embedding 仍可为 mock。

### 4. 组合示例

| Embedding | Chat | 需要 |
|-----------|------|------|
| mock | mock | 无 |
| qwen | mock | DASHSCOPE_API_KEY + 重建向量 |
| mock | deepseek | DEEPSEEK_API_KEY |
| qwen | deepseek | 两个 Key + 重建向量 |

## 切换 Embedding 后必须重建向量

修改以下任一项后，必须调用：

```http
POST /api/kb/{kbId}/embedding/rebuild
```

- `rag.embedding.provider`（mock ↔ qwen）
- `rag.embedding.model`
- `rag.embedding.dimension`

否则检索时会提示维度/模型不一致。

## V2 验收问题（保持不变）

| # | 问题 | mock 模式预期 |
|---|------|----------------|
| 1 | 短信验证码发不出去怎么排查？ | Top1 `03-troubleshooting.md` |
| 2 | SMS_429 是什么意思？ | Top1 `02-api-spec.md` |
| 3 | send-code 接口路径是什么？ | Top1 `02-api-spec.md` |
| 4 | 为什么后续会使用 PgVector？ | Top1 `01-project-guideline.md` |
| 5 | 公司年终奖发几个月？ | 无相关依据 |

测试文件：`http/retrieval-test.http`、`http/chat-test.http`

## 明确不做（V2.5）

DeepSeek Embedding、Qwen Chat、BM25、Elasticsearch、Hybrid Search、Reranker、Query Rewrite、Debug Console、Evaluation、权限、多轮对话

## 快速启动

```bash
docker compose up -d
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev
```

- 查看 Provider：`GET http://localhost:8080/api/model/providers`
- 问答页：http://localhost:5173/chat
- Swagger：http://localhost:8080/doc.html

## 下一步（V3，暂停）

RAG Debug 可观察版：召回/Prompt/耗时/日志可视化（仍不引入 BM25 / Hybrid / Reranker）。
