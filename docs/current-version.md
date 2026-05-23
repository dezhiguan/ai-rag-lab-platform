# current-version.md

## 当前版本

**V8：工程化增强（进行中）**

**上一完成版本：V7 Evaluation 评测中心**

## V8 已完成能力

### V8-01 系统运行状态看板

- `/system-status` 系统运行状态看板
- `GET /api/system/status`：后端 / PostgreSQL / ES / Provider / 核心能力状态

### V8-02 RAG 运行指标看板

- `/rag-metrics` RAG 运行指标看板
- `GET /api/rag/metrics`：基于 `rag_query_log` 聚合查询次数、耗时、检索模式、Reranker、慢查询 Top 10

### V8-03 RAG 查询日志中心

- `/rag-query-logs` 查询日志中心：分页、筛选、跳转 Debug 详情
- `GET /api/rag/query-logs`：关键词 / 检索模式 / 重排 / 慢查询筛选

## V7 已完成能力

- `/evaluation` 评测中心：单模式评测 + 三模式对比（VECTOR / BM25 / HYBRID）
- `POST /api/evaluation/run`、`POST /api/evaluation/compare`

## 当前阶段不做

- Recall@K、MRR、自定义评测集持久化
- 权限、多租户（V8 后续）

## 版本关系

| 版本 | 状态 |
|------|------|
| V1～V7 | 已完成 |
| **V8 工程化** | **进行中（V8-01～V8-03 已完成）** |

详见 `05-development-plan.md`、`07-change-log.md`。
