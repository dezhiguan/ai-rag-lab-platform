# AI RAG Lab Platform

企业级 RAG 知识库实验平台。当前为 **V0 项目骨架版**，仅提供可运行的前后端基础工程，不包含任何 RAG 业务实现。

## 当前版本：V0 项目骨架版

### V0 目标

- 搭建 Spring Boot 3 + Vue 3 单体仓库结构
- 提供系统健康检查接口 `GET /api/system/health`
- 集成 PostgreSQL 数据源（无业务表）
- 集成 Knife4j 接口文档
- 前端 Dashboard 展示后端连接状态

### V0 不做

- Embedding、向量检索、PgVector、Elasticsearch
- Reranker、Evaluation、权限、多轮对话
- 知识库、文档、Chunk 等业务代码与表结构

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3、JDK 17、Maven、PostgreSQL、MyBatis-Plus、Lombok、Knife4j |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios |

## 目录结构

```
ai-rag-lab-platform/
├── README.md
├── docker-compose.yml
├── .env.example
├── docs/
├── backend/
└── frontend/
```

## 快速开始

### 1. 启动 PostgreSQL

```bash
docker compose up -d
```

默认配置：

- 数据库：`rag_lab`
- 用户：`rag_user`
- 密码：`rag_password`
- 端口：`5432`

### 2. 启动后端

要求：JDK 17、Maven 3.8+

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- 服务地址：http://localhost:8080
- Knife4j 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/system/health

### 3. 启动前端

要求：Node.js 18+

```bash
cd frontend
npm install
npm run dev
```

- 前端地址：http://localhost:5173
- Dashboard：http://localhost:5173/dashboard

前端通过 Vite 代理将 `/api` 请求转发到后端 `8080` 端口。

## V0 验收标准

- [ ] `docker compose up -d` 可以启动 PostgreSQL
- [ ] 后端可以启动成功
- [ ] Knife4j 可以访问（`/doc.html`）
- [ ] `GET /api/system/health` 可以正常返回（status=UP, version=V0）
- [ ] 前端可以启动成功
- [ ] Dashboard 页面可以显示「后端连接成功」
- [ ] 项目中不出现 RAG、Embedding、Vector、Rerank、Evaluation 等后续版本业务代码

## 版本规划

详见 [docs/version-plan.md](docs/version-plan.md)。
