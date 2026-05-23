# current-version.md

## 当前版本

**V8：工程化增强（进行中）**

**上一完成版本：V7 Evaluation 评测中心**

## V8 已完成能力（V8-01）

- `/system-status` 系统运行状态看板
- `GET /api/system/status`：后端 / PostgreSQL / ES / Provider / 核心能力状态
- 「刷新状态」按钮；依赖异常时页面展示错误而非白屏

## V7 已完成能力

- `/evaluation` 评测中心：单模式评测 + 三模式对比（VECTOR / BM25 / HYBRID）
- `enableRerank`：评测 Top1 基于轻量 Reranker 重排后结果
- `POST /api/evaluation/run`、`POST /api/evaluation/compare`

## 当前阶段不做

- Recall@K、MRR、自定义评测集持久化
- 权限、多租户（V8 后续）

## 版本关系

| 版本 | 状态 |
|------|------|
| V1～V7 | 已完成 |
| **V8 工程化** | **进行中（V8-01 系统状态看板）** |

详见 `05-development-plan.md`、`07-change-log.md`。
