# AI RAG Lab Platform

企业级 RAG 知识库实验平台。当前为 **V1 文档导入与分块版**，支持知识库管理、Markdown/TXT 文档上传、解析与固定大小分块。

## 当前版本：V1 文档导入与分块版

### V1 目标

- 知识库 CRUD
- Markdown/TXT 文档上传、解析、固定大小分块（约 800 字，overlap 100）
- 样例数据一键初始化（3 个 Markdown 文档）
- 前端查看文档列表与 Chunk
- Dashboard 统计知识库/文档/Chunk 数量

### V1 不做

- Embedding、向量检索、PgVector、Elasticsearch、LLM 问答
- Reranker、Evaluation、权限、多轮对话

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
- 知识库 API：`/api/kb`
- 样例初始化：`POST /api/sample/init`

### 3. 启动前端

要求：Node.js 18+

```bash
cd frontend
npm install
npm run dev
```

- 前端地址：http://localhost:5173
- Dashboard：http://localhost:5173/dashboard
- 知识库：http://localhost:5173/kb

前端通过 Vite 代理将 `/api` 请求转发到后端 `8080` 端口。

**注意：** 不要用 IDE 的 Node 直接运行 `src/main.ts`。Vue 项目必须通过 Vite 启动，否则会出现 `Unknown file extension ".css"` 或 `.vue` 相关错误。

- IntelliJ IDEA：选择运行配置 **Frontend Dev**（npm run dev），不要对 `main.ts` 点 Run
- Cursor / VS Code：运行 **Frontend: Vite Dev Server**，或终端执行 `npm run dev`

## V1 验收标准

- [ ] `docker compose up -d` 可以启动 PostgreSQL
- [ ] 后端可以启动成功，Swagger/Knife4j 可访问
- [ ] 可以创建知识库、上传 Markdown/TXT，文档状态变为 COMPLETED
- [ ] 可以查看文档 Chunk 列表
- [ ] `POST /api/sample/init` 可导入 3 个样例文档
- [ ] Dashboard 显示知识库/文档/Chunk 统计
- [ ] 不出现 Embedding、Vector、Chat、LLM、Rerank、Evaluation 等后续版本代码

## 版本规划

详见 [docs/version-plan.md](docs/version-plan.md)。
