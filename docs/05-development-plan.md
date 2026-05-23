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
| V5 | Hybrid Search | 🔄 **当前 / 启动中** |
| V6 | Reranker | 未开始 |
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

## 当前：V5 Hybrid Search 混合检索版

### 目标

在 V4 基础上，融合 Vector 与 BM25 两路召回，应用层融合排序（如 RRF），Debug 支持 `HYBRID` 模式并可观察融合后召回。

### V5 小任务拆分（单任务约 10～15 分钟）

| 任务 ID | 内容 | 状态 |
|---------|------|------|
| **V5-01** | 版本切换与 V5 范围锁定（文档：需求/架构/库表/计划/变更记录） | ✅ 本次 |
| **V5-02** | 实现 Hybrid 融合排序核心逻辑（如 RRF，纯 Java 单元可测） | 待做 |
| **V5-03** | 实现 `HybridSearchService` 编排 Vector + BM25 | 待做 |
| **V5-04** | Debug 查询支持 `searchMode=HYBRID` | 待做 |
| **V5-05** | 前端 Debug 页支持 HYBRID 模式展示 | 待做 |
| **V5-06** | 补充 V5 回归测试与验收脚本 | 待做 |

### V5 禁止事项

- 不做 Reranker
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

### V5 Hybrid（待 V5-06）

- 验收脚本待 `V5-06` 补充

### 环境探测

```bash
python3 scripts/probe-rag-services.py
```

本地开发说明见 `00-project-guideline.md`。
