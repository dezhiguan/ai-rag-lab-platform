# 05 - 开发计划

## 版本总览

| 版本 | 名称 | 状态 |
|------|------|------|
| V0 | 项目骨架版 | ✅ 已完成 |
| V1 | 文档导入与分块版 | ✅ 已完成 |
| V2 | Naive RAG 问答版 | ✅ 已完成 |
| V2.5 | 真实模型接入版 | ✅ 已完成 |
| V3 | RAG Debug 可观察版 | ✅ 已完成 |
| V4 | 关键词检索版 | ✅ 已完成 |
| V5 | Hybrid Search | ✅ 已完成 |
| V6 | Reranker | ✅ 已完成 |
| V7 | Evaluation 评测中心 | ✅ **已完成** |
| V8 | 工程化增强 | ✅ 已完成 |
| V9 | 云部署与在线体验 | ✅ 已完成 |
| V10 | 访问保护与只读体验模式 | 🔄 **进行中** |

---

## 当前：V10 访问保护与只读体验模式（进行中）

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| V10-01 | 登录入口与体验账号访问保护（/login、auth API、Token 拦截、admin/guest） | ✅ 已完成 |

---

## V9 云部署与在线体验（已完成）

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| V9-01 | 生产环境部署准备（prod 配置、env 模板、Nginx、部署文档、项目总览） | ✅ 已完成 |
| V9-02 | 阿里云 ECS 部署环境准备（ECS 文档、环境检查脚本、部署顺序说明） | ✅ 已完成 |
| V9-03 | 双服务器部署配置落地（拓扑文档、env、Nginx、项目总览） | ✅ 已完成 |
| V9-04 | 数据与检索层 Compose 与健康检查脚本 | ✅ 已完成 |
| V9-05 | 应用入口层部署脚本（deploy-backend/frontend、Nginx、check-app-layer） | ✅ 已完成 |
| V9-06 | 双服务器实机联调 Runbook 与验收脚本 | ✅ 已完成 |

---

## 已完成版本（摘要）

### V0～V3

详见 `07-change-log.md`。核心能力：骨架 → 文档分块 → 向量问答 → 真实模型 → Debug 可观察 + Context 过滤。

### V4：关键词检索版（已完成）

| 类别 | 内容 |
|------|------|
| 基础设施 | `docker-compose` 可选 ES；`rag.elasticsearch` 配置 |
| 后端 | `SearchController`、`EsIndexService`、`Bm25SearchService`、`SearchTermExtractor` |
| API | `POST /api/search/index/rebuild`、`POST /api/search/bm25`；Debug `searchMode` VECTOR/BM25 |
| 前端 | `/debug` 检索模式切换、「重建 ES 索引」、V4 快捷问题 |
| ES | `terms` keyword；BM25 bool 多路加权 |

**已修复：** 查询「SMS_429 是什么意思？」时 BM25 Top1 排序问题（`terms` 专有词 + 加权查询 + 有抽取词时 must 命中 `terms`），Top1 应为 `02-api-spec.md`。

**验收参考（V4 回归仍可用）：**

```bash
curl -X POST http://localhost:8080/api/search/index/rebuild
python3 scripts/run-v4-bm25-smoke-test.py
```

### V5：Hybrid Search（已完成）

- 应用层 RRF 融合（`HybridSearchMerger`）
- Debug `searchMode=HYBRID` 闭环
- 融合来源与三路分数可观察（V5-05）
- 查询历史 Drawer + 前端分页（V5-04）

### V6：Reranker（已完成）

| 能力 | 说明 |
|------|------|
| 轻量 Reranker | `RerankService` 本地规则（关键词 + 专有词），不接外部模型 |
| Debug 开关 | `enableRerank`，默认关闭 |
| 可观察性 | `originalRank`、`rerankRank`、`rerankScore`、排名变化（上升/下降/不变） |
| 检索模式 | VECTOR / BM25 / HYBRID 均支持 + Reranker |
| 下游 | Context / Prompt / Answer 使用重排后顺序 |
| 历史详情 | `rag_query_log.enable_rerank` + `rag_retrieval_log` 重排字段回放（V6-02） |

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| V6-01 | Debug 轻量 Reranker 闭环（`enableRerank` + 页面展示） | ✅ 已完成 |
| V6-02 | 重排可观察性增强 + 详情页同步 + V6 文档收尾 | ✅ 已完成 |

**V6 禁止事项（已遵守）：** 不接外部 Reranker、不用 LLM 重排、不做 Evaluation / Query Rewrite、不新增业务表（仅在既有日志表增补可空列）。

---

## V7：Evaluation 评测中心（已完成）

| 能力 | 说明 |
|------|------|
| 单模式评测 | `POST /api/evaluation/run`，VECTOR / BM25 / HYBRID |
| 多模式对比 | `POST /api/evaluation/compare`，三种模式一次返回 |
| Reranker | `enableRerank` 开关，评测 Top1 基于重排后顺序 |
| 页面 | `/evaluation` 对比统计 + 分模式明细表 |

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| V7-01 | 评测中心最小闭环 | ✅ 已完成 |
| **V7-02** | 多模式对比 + Reranker 评测 + V7 文档收尾 | ✅ 已完成 |

**未纳入 V7：** Recall@K、MRR、自定义数据集、评测历史落库（可 V8+ 规划）。

---

## 当前：V8 工程化增强（已完成）

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| V8-01 | 系统运行状态看板（`/system-status`、`GET /api/system/status`） | ✅ 已完成 |
| V8-02 | RAG 运行指标看板（`/rag-metrics`、`GET /api/rag/metrics`） | ✅ 已完成 |
| V8-03 | RAG 查询日志中心（`/rag-query-logs`、`GET /api/rag/query-logs`） | ✅ 已完成 |
| V8-04 | 慢查询分析与优化建议（`/slow-query-analysis`、`GET /api/rag/query-logs/slow-analysis`） | ✅ 已完成 |
| V8-05 | RAG 参数实验台（`/rag-experiment`、`POST /api/experiment/rag-query`） | ✅ 已完成 |
| V8-06 | 参数实验台多组对比（页面状态，最多 5 组） | ✅ 已完成 |
| V8-07 | 项目总览与能力导航（`/project-overview`） | ✅ 已完成 |
| V8-08 | 项目收尾与开源说明（README、项目总览快速开始/完成状态/适用场景） | ✅ 已完成 |

---

## 后续版本

### V9：云部署与在线体验（续）

- 阿里云 ECS / RDS / ES 实际部署
- 域名、HTTPS、在线体验环境

---

## 测试说明摘要

### V3 Context 过滤回归

- **脚本：** `python3 scripts/run-v3-context-filter-tests.py`
- **最近结果：** 10/10 通过，详见 `06-debug-log.md`

### V4 BM25 冒烟

- **脚本：** `python3 scripts/run-v4-bm25-smoke-test.py`

### V6 Debug 轻量 Reranker

- 请求：`enableRerank`（默认 false）
- 规则：`RerankService` 本地关键词 + 专有词（`SearchTermExtractor`）加分
- 返回：`originalRank`、`rerankRank`、`rerankScore`；Context/Prompt/Answer 使用重排后顺序
- 前端：Debug 页「启用重排」、排名变化 Tag、Context 顺序提示；详情页同步
- 支持 VECTOR / BM25 / HYBRID + enableRerank

### V5-05 Hybrid 结果可观察性

- Debug `retrievedChunks` 在 HYBRID 模式返回：`matchedByVector`、`matchedByBm25`、`vectorScore`、`bm25Score`、`hybridScore`。
- `DebugView` / `DebugDetailView`（有字段时）展示来源标签与三路分数列。
- HYBRID 查询只调用一次 `HybridSearchService.search`，避免双路重复检索。

### V5-04 Debug 页面体验优化

- 主页面不再默认展开「查询历史」大表格。
- 查询区增加 **查询历史** 按钮，点击后以 **Drawer** 展示历史列表。
- 历史列表 **前端分页**（默认每页 10 条，可选 5/10/20），显示总条数；保留刷新与详情跳转。
- 未改后端 `/api/debug/query-logs` 接口。

### V5-02 Hybrid 融合（后端单元测试）

- **类：** `HybridSearchMerger`、`HybridSearchMergeItem`、`HybridSearchResult`
- **命令：** `cd backend && mvn test -Dtest=HybridSearchMergerTest`
- **覆盖：** chunkId 合并、BM25 按 Top1 归一化、hybridScore 排序、topK 截断、空输入与 topK≤0
- **说明：** 无页面测试、无 HYBRID searchMode、无接口变更

### 环境探测

```bash
python3 scripts/probe-rag-services.py
```

本地开发说明见 `00-project-guideline.md`。
