# CURRENT_VERSION.md

## 当前版本

**V2：Naive RAG 问答版 — Chat 闭环已完成**

## 上一版本状态

V1：文档导入与分块版 — 已完成

## 当前进度

| 能力 | 状态 |
|------|------|
| MockEmbeddingProvider（确定性 n-gram 向量） | 已完成 |
| Chunk 向量化 / 重建 / 状态查询 | 已完成 |
| PgVector 基础向量检索 | 已完成 |
| `POST /api/retrieval/test` 检索验证 | 已完成 |
| `POST /api/chat` Naive RAG 问答 | 已完成 |
| 固定 Prompt + Mock Chat | 已完成 |
| 引用来源返回 + 会话持久化 | 已完成 |
| 前端 `/chat` 页面 | 已完成 |

## 流水线

```text
Chunk → Embedding（Mock n-gram）→ PgVector 存储
  → 用户提问 → 向量检索 TopK → Prompt 拼接 → ChatModel（Mock）→ 回答 + sources
```

## 本次完成内容（V2 Chat 闭环）

### 后端

1. **MockEmbeddingProvider**（已满足要求，未重写）
   - 确定性字符 bigram / trigram 哈希，384 维，L2 归一化
   - 同文本向量一致；共享 n-gram 的 query/chunk 相似度更高；支持中文；无 Random
   - Chunk 与 Query 共用同一 `EmbeddingProvider`

2. **`POST /api/retrieval/test`**
   - 仅做基础向量 TopK 检索，无阈值过滤、无 Debug/BM25/Hybrid/Reranker

3. **`POST /api/chat`**
   - 流程：query embedding → PgVector TopK → 相关性过滤 → context → 固定 Prompt → ChatModel → 保存 session/message → 返回 answer + sources
   - `ChatRelevanceFilter`：问答侧按 n-gram 重叠过滤无关 chunk（不影响 retrieval/test）
   - 无关问题（如「公司年终奖」）返回：`知识库中没有找到相关依据。`

4. **固定 Prompt**（`PromptBuilder`，无模板管理）

### 前端

- 路由 `/chat`：知识库选择、向量化状态、重建向量、提问、回答、引用来源（文档名 / Chunk 编号 / 相似度 / 内容）
- 快捷测试按钮、未向量化告警、会话 ID 展示

### 测试文件

- `http/retrieval-test.http` — 4 条检索用例
- `http/chat-test.http` — 5 条问答用例（含无关问题）

## 新增 / 修改接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/kb/{kbId}/embedding/rebuild` | 清空并重建知识库向量 |
| GET | `/api/kb/{kbId}/embedding/status` | 向量化状态 |
| POST | `/api/retrieval/test` | V2 检索基线验证 |
| POST | `/api/chat` | Naive RAG 问答 |
| GET | `/api/chat/sessions` | 会话列表 |
| GET | `/api/chat/sessions/{sessionId}/messages` | 会话消息 |

## 新增 / 修改前端

| 路径 | 说明 |
|------|------|
| `/chat` | V2 问答页（`ChatView.vue`） |
| `src/api/embedding.ts` | 向量化 API |
| `src/api/chat.ts` | 问答 API |
| `src/types/embedding.ts` | 向量化类型 |
| `src/types/chat.ts` | 问答类型 |

## 推荐测试用例

**先执行** `POST /api/kb/{kbId}/embedding/rebuild`，再测试：

| # | 问题 | retrieval/test 预期 Top1 | chat 预期 |
|---|------|--------------------------|-----------|
| 1 | 短信验证码发不出去怎么排查？ | `03-troubleshooting.md` | 有依据回答 + sources |
| 2 | SMS_429 是什么意思？ | `02-api-spec.md` | 有依据回答 + sources |
| 3 | send-code 接口路径是什么？ | `02-api-spec.md` | 有依据回答 + sources |
| 4 | 这个项目为什么后续会使用 PgVector？ | `01-project-guideline.md` | 有依据回答 + sources |
| 5 | 公司年终奖发几个月？ | 可返回任意 TopK | **知识库中没有找到相关依据。** |

## V2 禁止（未实现）

BM25、Elasticsearch、Hybrid Search、Reranker、Query Rewrite、Debug Console、Evaluation、权限、多轮问题重构

## 配置说明

```yaml
rag:
  embedding:
    provider: mock    # 当前使用 Mock，不接真实 Embedding
    dimension: 384
  chat:
    provider: mock    # 当前使用 Mock Chat
```

## 下一步建议（V3，不在本次范围）

1. RAG Debug 可观察版：展示召回 Chunk、Prompt、耗时、问答日志
2. 可选接入真实 Embedding / LLM API
3. 检索过程可视化（仍不做 BM25 / Hybrid / Reranker）

## 快速启动

```bash
docker compose up -d
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev
```

1. 创建知识库 → 样例初始化（V1）
2. `/chat` → 重建向量
3. `http/retrieval-test.http` 或 `/chat` 快捷测试验证
