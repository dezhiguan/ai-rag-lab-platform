# CURRENT_VERSION.md

## 当前版本

**V4：关键词检索版**

在 V3 Debug 可观察能力基础上，引入 Elasticsearch + BM25 关键词检索，解决错误码、接口路径、专有名词等场景下纯向量检索不稳定的问题。

## 版本关系

| 版本 | 状态 |
|------|------|
| V1 文档导入与分块 | 已完成 |
| V2 Naive RAG 问答 | 已完成 |
| V2.5 真实模型接入 | 已完成 |
| V3 RAG Debug 可观察 | 已完成 |
| **V4 关键词检索** | **当前** |
| V5 混合检索 | 未开始 |

## V4 完成内容

### 基础设施

| 项 | 说明 |
|----|------|
| `docker-compose.yml` | 新增 Elasticsearch 8.11 单节点，端口 9200，开发模式关闭 xpack.security |
| `rag.elasticsearch` | `hosts` / `index` / 可选 `username` / `password`（`application.yml`） |

### 后端 search 模块

| 类型 | 说明 |
|------|------|
| `SearchController` | 检索 API |
| `EsIndexService` | 索引 `rag_document_chunk` 创建与全量重建 |
| `Bm25SearchService` | BM25 检索；Debug 模式下分数归一化后走 V3 Context 过滤 |
| `SearchMode` | `VECTOR` / `BM25`（不含 HYBRID） |

**索引字段：** `kbId`、`documentId`、`documentName`、`chunkId`、`chunkIndex`、`content`（text，standard analyzer）

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/search/index/rebuild` | 从 `document_chunk` 全量同步 ES |
| POST | `/api/search/bm25` | BM25 关键词检索 |
| POST | `/api/debug/query` | 新增 `searchMode`：`VECTOR`（默认）/ `BM25` |

**`POST /api/search/bm25` 请求示例：**

```json
{
  "kbId": 1,
  "query": "SMS_429 是什么意思？",
  "topK": 5
}
```

**`POST /api/debug/query` 新增字段：**

```json
{
  "kbId": 1,
  "question": "SMS_429 是什么意思？",
  "topK": 5,
  "searchMode": "BM25"
}
```

返回增加 `searchMode`；BM25 模式仍经 `ContextChunkFilter` → `PromptBuilder` → Chat。

### Debug 前端

| 项 | 说明 |
|----|------|
| `/debug` | 检索模式 Vector / BM25；BM25 下可「重建 ES 索引」 |
| 结果展示 | 检索模式、耗时、召回、Context、Prompt、Answer |
| V4 快捷问题 | SMS_429、send-code、`/api/sms/send-code`、验证码排查、PgVector |

### 复用 V1～V3

- 文档导入、分块、向量检索、Chat、Context 过滤、Debug 历史均保留
- `/api/chat` 仍仅向量检索（未改）

## V4 明确不做

Hybrid Search、RRF 融合、Reranker、Query Rewrite、Evaluation、权限、多轮对话、后续版本空实现

## V4 验收问题

在 `/debug` 分别用 **Vector** 与 **BM25** 测试：

| # | 问题 | BM25 预期 Top1 |
|---|------|----------------|
| 1 | SMS_429 是什么意思？ | `02-api-spec.md` |
| 2 | send-code 接口路径是什么？ | `02-api-spec.md` |
| 3 | /api/sms/send-code 是什么接口？ | `02-api-spec.md` |
| 4 | 短信验证码发不出去怎么排查？ | `03-troubleshooting.md` 或 api-spec |
| 5 | 这个项目为什么后续会使用 PgVector？ | `01-project-guideline.md` |

## 快速启动

**方式 A：无 Docker（推荐旧机器）** — 配置见项目根 `.env`，说明见 [LOCAL_DEV_WITHOUT_DOCKER.md](LOCAL_DEV_WITHOUT_DOCKER.md)

```bash
# PostgreSQL、ES 使用 .env 中的地址（可为远程 ES）
# VS Code 启动 RagApplication（launch.json 已加载 .env）
python3 scripts/probe-rag-services.py   # 探测 ES / 后端是否可用
cd frontend && npm run dev
```

**方式 B：Docker Compose（可选）**

```bash
docker compose up -d
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm run dev
```

**V4 首次使用 BM25 前：**

```bash
# 1. 确保样例库已有 Chunk（Dashboard 初始化样例或上传文档）
# 2. 全量重建 ES 索引
curl -X POST http://localhost:8080/api/search/index/rebuild

# 3. BM25 检索验证
curl -X POST http://localhost:8080/api/search/bm25 \
  -H 'Content-Type: application/json' \
  -d '{"kbId":1,"query":"SMS_429","topK":5}'
```

- Debug：http://localhost:5173/debug
- HTTP 用例：`http/search-test.http`

## V3 Context 过滤（仍生效）

```yaml
rag:
  context:
    max-chunks: 2
    min-score: 0.45
    max-score-gap: 0.35
```

BM25 原始分数量纲与向量不同，Debug 流程在过滤前将 BM25 分数按 Top1 归一化到 [0,1]，再应用上述规则。

## V2.5 配置（Embedding / Chat）

见 `application-dev.yml`；Vector 模式切换 Embedding 后须 `POST /api/kb/{kbId}/embedding/rebuild`。

## 环境变量（可选）

| 变量 | 说明 |
|------|------|
| `ES_HOSTS` | 默认 `http://localhost:9200` |
| `ES_USERNAME` / `ES_PASSWORD` | 生产 ES 鉴权（本地 compose 无需） |

## 下一步（V5）

混合检索（Vector + BM25、RRF），本版本未引入。
