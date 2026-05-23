# 05 - 开发计划

## 版本总览

| 版本 | 名称 | 状态 |
|------|------|------|
| V0 | 项目骨架版 | ✅ 已完成 |
| V1 | 文档导入与分块版 | ✅ 已完成 |
| V2 | Naive RAG 问答版 | ✅ 已完成 |
| V2.5 | 真实模型接入版 | ✅ 已完成 |
| V3 | RAG Debug 可观察版 | ✅ 已完成 |
| V4 | 关键词检索版 | 🔄 **当前** |
| V5 | Hybrid Search | 未开始 |
| V6 | Reranker | 未开始 |
| V7 | Evaluation 评测中心 | 未开始 |
| V8 | 工程化增强 | 未开始 |

---

## 已完成版本（摘要）

### V0～V3

详见 `07-change-log.md`。核心能力：骨架 → 文档分块 → 向量问答 → 真实模型 → Debug 可观察 + Context 过滤。

---

## 当前：V4 关键词检索版

### 目标

引入 Elasticsearch + BM25，改善错误码、API 路径、专有名词等场景的检索排序；Debug 支持 Vector / BM25 切换。

### 已完成（开发中项）

| 类别 | 内容 |
|------|------|
| 基础设施 | `docker-compose` 可选 ES；`rag.elasticsearch` 配置 |
| 后端 | `SearchController`、`EsIndexService`、`Bm25SearchService`、`SearchTermExtractor` |
| API | `POST /api/search/index/rebuild`、`POST /api/search/bm25`；Debug `searchMode` |
| 前端 | `/debug` 检索模式切换、「重建 ES 索引」、V4 快捷问题 |
| ES 字段 | `terms` keyword；索引时抽取专有词 |

### V4 当前问题

**现象：** 查询「SMS_429 是什么意思？」时，BM25 曾将 `01-project-guideline.md` 排第一。

**期望：** Top1 应为 `02-api-spec.md`。

**初步原因：**

- `content` 使用 standard analyzer
- 中文问句经分词后产生大量普通词
- `match` 默认 OR 语义下，普通词（如「什么」「意思」）干扰评分
- 专有词 `SMS_429` 未获得足够权重

**修复方向（进行中/待验收）：**

1. 抽取专有词 `terms`（错误码、API 路径、短名）
2. ES mapping 增加 `terms` keyword 字段
3. 重建索引时写入 `terms`
4. 查询时提取 query terms
5. bool `should` 多路加权：
   - `terms` 精确匹配：**高 boost（20）**
   - `content` match_phrase：**次高 boost（15）**
   - `content` match `operator=and`：中等（2）
   - `content` match 全文降权：`minimum_should_match=70%`（1）

### V4 验收用例

在 `/debug` 分别用 **Vector** 与 **BM25** 测试：

| # | 问题 | BM25 预期 Top1 |
|---|------|----------------|
| 1 | SMS_429 是什么意思？ | `02-api-spec.md` |
| 2 | send-code 接口路径是什么？ | `02-api-spec.md` |
| 3 | /api/sms/send-code 是什么接口？ | `02-api-spec.md` |
| 4 | 短信验证码发不出去怎么排查？ | `03-troubleshooting.md` 或 api-spec |
| 5 | 这个项目为什么后续会使用 PgVector？ | `01-project-guideline.md` |

**快速验证：**

```bash
curl -X POST http://localhost:8080/api/search/index/rebuild
curl -X POST http://localhost:8080/api/search/bm25 \
  -H 'Content-Type: application/json' \
  -d '{"kbId":1,"query":"SMS_429","topK":5}'
python3 scripts/run-v4-bm25-smoke-test.py
```

### V4 明确不做

Hybrid、RRF、Reranker、Query Rewrite、Evaluation、权限、多轮对话、V5+ 空实现。

---

## 后续版本

### V5：Hybrid Search

- Vector + BM25 融合（如 RRF）
- 不提前建空服务/表

### V6：Reranker

- 粗召回后精排

### V7：Evaluation

- 评测数据集、Recall@K、MRR 等

### V8：工程化增强

- 权限、多租户、监控、部署等

---

## 测试说明摘要

### V3 Context 过滤回归

- **脚本：** `python3 scripts/run-v3-context-filter-tests.py`
- **单元测试：** `cd backend && mvn test -Dtest=ContextChunkFilterTest`
- **配置：** `max-chunks: 2`、`min-score: 0.45`、`max-score-gap: 0.35`
- **最近结果（2026-05-22）：** 10/10 通过（qwen + deepseek），详见 `06-debug-log.md`

### V4 BM25 冒烟

- **脚本：** `python3 scripts/run-v4-bm25-smoke-test.py`
- **前置：** 样例库已初始化、已 `POST /api/search/index/rebuild`

### 环境探测

```bash
python3 scripts/probe-rag-services.py
```

本地开发说明见 `00-project-guideline.md`（`.env` 优先，不强制 Docker）。
