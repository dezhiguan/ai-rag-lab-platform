# current-version.md

## 当前版本

**V7：Evaluation 评测中心（下一阶段，未开始）**

**上一完成版本：V6 Reranker** — Debug 轻量本地重排（`enableRerank` + `RerankService`），支持 VECTOR / BM25 / HYBRID。

## V6 已完成能力

- Debug「启用重排」开关
- `originalRank` / `rerankRank` / `rerankScore` / 排名变化展示
- Context / Prompt / Answer 使用重排后顺序
- 历史详情回放（`rag_query_log` / `rag_retrieval_log` 增补字段）

## 当前阶段不做

- Evaluation 评测中心（V7）
- 外部 Reranker、LLM 重排
- Query Rewrite、权限、多租户

## 版本关系

| 版本 | 状态 |
|------|------|
| V1～V5 | 已完成 |
| **V6 Reranker** | **已完成** |
| **V7 Evaluation** | **下一阶段** |
| V8 工程化 | 未开始 |

详见 `05-development-plan.md`、`07-change-log.md`。
