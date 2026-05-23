# current-version.md

## 当前版本

**V7：Evaluation 评测中心**

在 V6 Reranker 已完成的基础上，提供评测中心页面：选择检索模式、执行内置用例批量评测、展示 Top1 命中与整体通过率。

## V7-01 已完成

- 前端 `/evaluation`（菜单：评测中心）
- `GET /api/evaluation/cases`、`POST /api/evaluation/run`
- VECTOR / BM25 / HYBRID 批量 Top1 文档验收

## 当前阶段不做

- 评测 PostgreSQL 表、历史持久化
- Recall@K、MRR、自定义数据集（V7-02+）
- 评测链路接入 Reranker

## 版本关系

| 版本 | 状态 |
|------|------|
| V1～V6 | 已完成 |
| **V7 Evaluation** | **进行中（V7-01 已完成）** |
| V8 工程化 | 未开始 |

详见 `05-development-plan.md`、`07-change-log.md`。
