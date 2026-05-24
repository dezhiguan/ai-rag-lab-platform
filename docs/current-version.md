# current-version.md

## 当前版本

**V9：云部署与在线体验环境（进行中）**

**上一完成版本：V8 工程化增强版**

**阶段定位：第二阶段核心强化版**

## V9 进行中能力

### V9-01 生产环境部署准备

- `application-prod.yml` 后端生产 profile
- `.env.prod.example` 生产环境变量模板
- `frontend/.env.production.example` 前端生产 API 配置
- `deploy/nginx.conf.example` Nginx 反代模板（含 HTTPS 预留说明）
- `scripts/run-backend-prod.sh` 加载 env 并启动 jar
- `docs/09-production-deployment.md` 部署说明
- 项目总览页「线上部署准备」区域

### V9-02 阿里云 ECS 部署环境准备

- `docs/10-aliyun-ecs-setup.md` ECS 规格、安全组、初始化与环境检查
- `scripts/check-ecs-env.sh` 应用层环境检查脚本

### V9-03 双服务器部署配置落地

- 部署拓扑：轻量服务器（应用入口）+ ECS（数据检索）
- `docs/09-production-deployment.md`、`docs/10-aliyun-ecs-setup.md` 统一为双服务器方案
- `.env.prod.example`：`POSTGRES_HOST` / `ES_HOSTS` / `REDIS_HOST` 使用 `<ECS_PRIVATE_IP>`
- `deploy/nginx.conf.example`：仅反代本机 Java，不暴露 PG/ES/Redis
- 项目总览「线上部署准备」展示双服务器架构与备案策略

### V9-04 数据与检索层服务部署脚本准备

- `deploy/data-layer/docker-compose.data.yml`：PostgreSQL（PgVector）、Elasticsearch、Redis
- `deploy/data-layer/.env.data.example`、`deploy/data-layer/README.md`
- `scripts/check-data-layer.sh`：数据层健康检查（支持 `<ECS_PRIVATE_IP>` 参数）
- 文档与项目总览更新；Redis 仅基础设施，RAG 业务未接入

**说明：** 公开文档不含真实 IP。

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

### V8-04 慢查询分析

- `/slow-query-analysis` 慢查询分析与优化建议
- `GET /api/rag/query-logs/slow-analysis`：规则判定慢查询 + 原因/建议生成

### V8-05 RAG 参数实验台

- `/rag-experiment` 参数实验台：调整 topK / Context 过滤参数并观察 RAG 链路影响
- `POST /api/experiment/rag-query`：`maxChunks`、`minScore`、`maxScoreGap` 仅对本次请求生效

### V8-06 参数实验多组对比

- `/rag-experiment` 实验对比区：最多保留 5 组结果，支持展开详情、清空对比（页面状态，不落库）

### V8-07 项目总览与能力导航

- `/project-overview` 项目总览：版本路线图、功能入口、推荐使用流程、核心能力

### V8-08 项目收尾与开源说明

- `README.md` 完善：定位、能力、技术栈、启动、样例数据、页面入口、FAQ
- 项目总览页：快速开始、项目完成状态（V0～V8）、适用场景

## 版本关系

| 版本 | 状态 |
|------|------|
| V0～V8 | 已完成 |
| V9 | 进行中（V9-01、V9-02 文档与脚本已完成） |

详见 `05-development-plan.md`、`07-change-log.md`、`09-production-deployment.md`、`10-aliyun-ecs-setup.md`。
