# current-version.md

## 当前版本

**V10.5：线上体验与页面布局优化（已完成）**

**上一完成版本：V10 访问保护与只读体验模式**

**阶段定位：第二阶段核心强化版**

## V10.5 已完成能力

### V10.5-01 全局 UI/UX 优化 ✅

- 左侧分组 Sidebar + 精简顶栏（项目名、模式 Tag、用户、退出）
- `frontend/src/styles/theme.css` 统一视觉变量与 `rag-*` 通用 class
- 项目总览 Tab 化，首屏压缩
- 宽表格 `rag-table-scroll` 横向滚动（Debug / Evaluation / 实验台 / 日志 / 慢查询）

## V10 已完成能力

### V10-02 体验账号只读模式与危险操作保护 ✅

- guest **只读体验模式**：顶栏与项目总览展示「只读体验模式」
- 后端 `GuestWriteInterceptor`：拦截 guest 危险写 API（403 + 友好提示）
- 前端 `usePermission` / `requireWrite`：危险按钮禁用 + 点击提示
- guest 导航精简：隐藏 Dashboard、知识库、问答、About（可直接访问 URL 只读浏览）
- admin 保持完整权限

### V10-01 新增登录入口与体验账号访问保护 ✅

- 前端 `/login` 登录页；登录成功进入 `/project-overview`
- 内置账号：**admin**（管理员）、**guest**（体验用户）
- 后端：`POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout`
- 内存 Token 鉴权；未登录拦截 `/api/**`（`GET /api/system/health` 放行）
- 前端路由守卫、Bearer Token、布局栏显示用户/角色/退出
- 项目总览「当前访问身份」：当前用户、角色、模式（管理员 / 体验用户）
- 配置：`rag.auth.*`；生产密码 via `AUTH_ADMIN_PASSWORD` / `AUTH_GUEST_PASSWORD`

**说明：** guest 账号当前可访问页面与查询；危险写操作限制留待后续 V10 任务。

## V9 已完成能力

### V9-01～V9-06 云部署与实机联调 ✅

- 双服务器拓扑：轻量（应用入口）+ ECS（数据检索）
- `docs/09-production-deployment.md`、`docs/10-aliyun-ecs-setup.md`、`docs/11-dual-server-online-runbook.md`
- 部署脚本与 `verify-online-deployment.sh` 公网联调验收
- 实机部署完成；样例 KB 已初始化

## V8 已完成能力

### V8-01 系统运行状态看板

- `/system-status` 系统运行状态看板
- `GET /api/system/status`：后端 / PostgreSQL / ES / Provider / 核心能力状态

### V8-02 RAG 运行指标看板

- `/rag-metrics` RAG 运行指标看板
- `GET /api/rag/metrics`：基于 `rag_query_log` 聚合查询次数、耗时、检索模式、Reranker、慢查询 Top 10

### V8-03 RAG 查询日志中心

- `/rag-query-logs` 查询日志列表与筛选
- `GET /api/rag/query-logs`：grouped 列表

### V8-04 慢查询分析

- `/slow-query-analysis` 慢查询分析页
- `GET /api/rag/query-logs/slow-analysis`

### V8-05 参数实验台

- `/rag-experiment` 参数实验台
- `POST /api/experiment/rag-query` 多组参数对比

### V8-06 项目总览页

- `/project-overview` 项目总览、快速开始、版本路线图

### V8-07 开源 README 与 About

- README 快速开始、部署说明
- `/about` About 页

### V8-08 项目收尾与开源说明

- README、项目总览完成状态与适用场景
