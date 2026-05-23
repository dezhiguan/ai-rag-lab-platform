# current-version.md

## 当前版本

**V8：工程化增强（下一阶段，未开始）**

**上一完成版本：V7 Evaluation 评测中心**

## V7 已完成能力

- `/evaluation` 评测中心：单模式评测 + 三模式对比（VECTOR / BM25 / HYBRID）
- `enableRerank`：评测 Top1 基于轻量 Reranker 重排后结果
- `POST /api/evaluation/run`、`POST /api/evaluation/compare`
- 通过率、失败数、平均耗时、分用例分模式明细

## 当前阶段不做

- Recall@K、MRR、自定义评测集持久化
- 权限、多租户（V8）

## 版本关系

| 版本 | 状态 |
|------|------|
| V1～V7 | 已完成 |
| **V8 工程化** | **下一阶段** |

详见 `05-development-plan.md`、`07-change-log.md`。
