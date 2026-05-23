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
| V6 | Reranker | 🔄 **当前** |
| V7 | Evaluation 评测中心 | 未开始 |
| V8 | 工程化增强 | 未开始 |

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

---

## 当前：V6 Reranker 重排版

### 目标

在 V5 Hybrid 基础上，Debug 支持轻量本地 Reranker：关键词与专有词命中规则重排，页面可观察重排前后排名。

### V6 小任务

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| **V6-01** | Debug 轻量 Reranker 闭环（`enableRerank` + 页面展示） | ✅ 已完成 |
| **V6-02+** | 后续增强（待规划） | 未开始 |

### V6 禁止事项（V6-01）

- 不接外部 Reranker 模型
- 不用 LLM 做重排
- 不做 Evaluation
- 不做 Query Rewrite
- 不做权限、多租户、多轮对话
- 不新增 PostgreSQL 表、不新增 ES 索引（V5-01～06 均遵守，除非架构师另开任务）
- 不提前创建 V6+ 空类、空接口、空页面、空表

### V5 验收方向（规划，V5-06 细化）

- Debug `HYBRID` 下专有词问句（如 SMS_429）融合结果优于单路 Vector 或单路 BM25 的不稳定情况
- 融合后仍经 Context 过滤与 Prompt 生成
- 现有 V4 BM25、V3 Context 回归不被破坏

---

## 后续版本

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
- **最近结果：** 10/10 通过，详见 `06-debug-log.md`

### V4 BM25 冒烟

- **脚本：** `python3 scripts/run-v4-bm25-smoke-test.py`

### V6-01 Debug 轻量 Reranker

- 请求：`enableRerank`（默认 false）
- 规则：`RerankService` 本地关键词 + 专有词（`SearchTermExtractor`）加分
- 返回：`originalRank`、`rerankRank`、`rerankScore`；Context/Prompt/Answer 使用重排后顺序
- 前端：Debug 页「启用重排」开关 + 表格列展示
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

### V5 Hybrid 端到端（待 V5-06）

- 验收脚本待 `V5-06` 补充

### 环境探测

```bash
python3 scripts/probe-rag-services.py
```

本地开发说明见 `00-project-guideline.md`。
