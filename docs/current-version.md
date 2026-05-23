# current-version.md

## 当前版本

**V5：Hybrid Search 混合检索版**

在 V4 BM25 关键词检索已完成的基础上，融合 Vector 与 BM25 两路召回，在应用层做融合排序（如 RRF），并复用 Debug 链路展示 Hybrid 检索过程。

## 说明

- 在 V4 能力之上增加 **HYBRID** 检索模式（规划，代码待 V5-02～V5-05 实现）。
- **当前阶段不做**：Reranker、Evaluation、Query Rewrite、权限、多租户、多轮对话。
- **不新增** PostgreSQL 表、不新增 ES 索引；融合结果暂不落库。

## 版本关系

| 版本 | 状态 |
|------|------|
| V1 文档导入与分块 | 已完成 |
| V2 Naive RAG 问答 | 已完成 |
| V2.5 真实模型接入 | 已完成 |
| V3 RAG Debug 可观察 | 已完成 |
| V4 关键词检索 | 已完成 |
| **V5 混合检索** | **当前 / 启动中** |
| V6 Reranker | 未开始 |

## V5 开发任务（见 docs/05-development-plan.md）

| 任务 | 说明 |
|------|------|
| V5-01 | 版本切换与范围锁定（文档） |
| V5-02 | Hybrid 融合排序核心逻辑 |
| V5-03 | HybridSearchService 编排 |
| V5-04 | Debug 支持 HYBRID |
| V5-05 | 前端 Debug HYBRID |
| V5-06 | 回归测试与验收脚本 |

## V4 已完成能力（保留）

- Elasticsearch + 索引 `rag_document_chunk`
- BM25 + `terms` 专有词加权查询
- Debug `searchMode`：VECTOR / BM25
- BM25 Top1 排序问题已修复

详细 V4 接口与配置见历史记录或 `docs/03-api-design.md`、`docs/07-change-log.md`。

## 快速参考

- 开发计划：`docs/05-development-plan.md`
- 架构：`docs/02-architecture.md`
- 需求：`docs/01-requirements.md`
- AI 协作：`docs/08-ai-collaboration.md`
