# AI RAG Lab Platform

企业级 RAG 知识库实验平台。按版本逐步构建完整 RAG 能力：从文档导入、向量与关键词检索、混合检索与重排，到评测中心与工程化可观测，适合个人学习、长期实验与开源参考。

**当前版本：V9 云部署与在线体验环境**（第二阶段 · V9-01 生产部署准备）

> V0～V8 已完成；V9 起进入第二阶段核心强化。

## 项目定位

- 以「可运行、可观察、可对比」为原则，分版本迭代 RAG 能力
- 支持知识库管理、多模式检索（Vector / BM25 / Hybrid）、轻量 Reranker、内置评测与参数实验
- 提供系统状态、运行指标、查询日志、慢查询分析等工程化能力
- 不追求一次性大而全，每个版本均可独立启动与验收

## 核心能力

| 能力 | 说明 |
|------|------|
| 文档导入与分块 | Markdown/TXT 上传、固定大小分块 |
| 向量检索 | PgVector + Embedding（Mock / 通义千问） |
| BM25 检索 | Elasticsearch 关键词检索 |
| Hybrid 混合检索 | Vector + BM25 应用层融合 |
| Reranker 重排 | 轻量本地重排，可观察排名变化 |
| Debug 可观察 | 召回、Context 过滤、Prompt、Answer、耗时 |
| Evaluation 评测 | 内置用例 Top1 文档命中、三模式对比 |
| 工程化可观测 | 系统状态、RAG 指标、查询日志、慢查询分析 |
| 参数实验台 | 请求级 Context 参数调整，最多 5 组结果对比 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3、JDK 17、Maven、PostgreSQL、PgVector、Elasticsearch、MyBatis-Plus、Knife4j |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios |
| 模型 | Mock / 通义千问 Embedding、Mock / DeepSeek Chat（可配置） |

## 版本路线图

| 版本 | 核心能力 |
|------|----------|
| V0 | 项目骨架、健康检查、Dashboard |
| V1 | 文档导入与分块、样例数据初始化 |
| V2 | Naive RAG：向量检索 + Chat 问答 |
| V2.5 | 真实模型 Provider（Qwen Embedding、DeepSeek Chat） |
| V3 | Debug 可观察：全链路召回与 Context |
| V4 | BM25 关键词检索（Elasticsearch） |
| V5 | Hybrid Search 混合检索 |
| V6 | 轻量 Reranker 重排 |
| V7 | Evaluation 评测中心 |
| V8 | 工程化增强：系统状态、指标、日志、慢查询、参数实验台、项目总览 |

详见 [docs/05-development-plan.md](docs/05-development-plan.md)、[docs/current-version.md](docs/current-version.md)。

## 本地启动

### 环境要求

- JDK 17、Maven 3.8+
- Node.js 18+
- Docker（推荐，用于 PostgreSQL + Elasticsearch）

### 1. 启动依赖服务

```bash
# 项目根目录
docker compose up -d
```

默认：PostgreSQL（`rag_lab`，端口 5432）、Elasticsearch（端口 9200）。

可复制 [.env.example](.env.example) 为 `.env` 并按需配置模型 API Key：

```bash
cp .env.example .env
# 编辑 DASHSCOPE_API_KEY、DEEPSEEK_API_KEY 等
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或使用 IDE 运行 `com.guan.rag.RagApplication`（`dev` profile）。

| 项 | 地址 |
|----|------|
| API | http://localhost:8080 |
| Knife4j | http://localhost:8080/doc.html |
| 健康检查 | http://localhost:8080/api/system/health |

本地开发若使用自有 PostgreSQL，可通过环境变量覆盖：`POSTGRES_HOST`、`POSTGRES_PORT`、`POSTGRES_DB`、`POSTGRES_USER`、`POSTGRES_PASSWORD`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

| 项 | 地址 |
|----|------|
| 前端 | http://localhost:5173 |

前端通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`。请使用 `npm run dev` 启动，不要直接用 Node 运行 `src/main.ts`。

## 生产环境部署

> **双服务器方案**：应用入口层（轻量 2C4G，Nginx + 前端 + Java 后端）+ 数据检索层（ECS 4C8G，PostgreSQL / Elasticsearch / Redis 内网）。  
> 详见 [docs/09-production-deployment.md](docs/09-production-deployment.md)、[docs/10-aliyun-ecs-setup.md](docs/10-aliyun-ecs-setup.md)。公开文档使用 `<ECS_PRIVATE_IP>` 等占位符，真实 IP 仅写在本地 `.env.prod`。

### 应用入口层（轻量服务器）

```bash
cp deploy/app-layer/.env.app.example deploy/app-layer/.env.app
./scripts/deploy-backend-app.sh
./scripts/deploy-frontend-app.sh
./scripts/check-app-layer.sh <ECS_PRIVATE_IP>
```

详见 [deploy/app-layer/README.md](deploy/app-layer/README.md)。

### 数据与检索层（ECS）

在 ECS 上使用 Docker Compose 启动 PostgreSQL、Elasticsearch、Redis：

```bash
cd deploy/data-layer
cp .env.data.example .env.data   # 编辑后勿提交
docker compose -f docker-compose.data.yml --env-file .env.data up -d
```

详见 [deploy/data-layer/README.md](deploy/data-layer/README.md)（启停、日志、状态、内网访问）。

```bash
# 健康检查（轻量服务器传入 ECS 内网 IP）
./scripts/check-data-layer.sh <ECS_PRIVATE_IP>
```

### 准备生产环境变量（应用层）

```bash
cp .env.prod.example .env.prod
# 在轻量服务器编辑：POSTGRES_HOST、ES_HOSTS、REDIS_HOST 填 ECS 内网 IP
# 勿将 .env.prod 提交到 Git
```

后端使用 `prod` profile，配置见 `backend/src/main/resources/application-prod.yml`。

### 后端打包与启动

```bash
cd backend
mvn -DskipTests clean package

# 加载 .env.prod 并启动（项目根目录）
chmod +x scripts/run-backend-prod.sh
./scripts/run-backend-prod.sh
```

或手动加载环境变量：

```bash
set -a && source .env.prod && set +a
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

### 前端生产构建

```bash
cd frontend
cp .env.production.example .env.production   # 同域 Nginx 反代时 VITE_API_BASE_URL 留空
npm install
npm run build
```

构建产物在 `frontend/dist/`，部署到**轻量服务器** Nginx 静态目录（如 `/var/www/rag-lab/frontend/`）。

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | 前后端**同域**（Nginx 反代 `/api`）时留空；**分域**时填 API 根地址 |

### Nginx 反向代理

模板：[deploy/nginx.conf.example](deploy/nginx.conf.example)

- `/` → 前端 `dist` 静态资源（支持 Vue Router history）
- `/api/` → 后端 `127.0.0.1:8080`
- 文件内含 HTTPS 证书配置说明（预留）

## 样例数据初始化

**方式一（推荐）：页面操作**

1. 打开 http://localhost:5173/kb ，进入任一知识库的「文档」列表
2. 点击「初始化样例数据」，导入 3 个内置 Markdown 文档

**方式二：API**

```bash
curl -X POST http://localhost:8080/api/sample/init
```

初始化后建议：

1. **向量重建**：打开「问答」页，选择知识库后点击「重建向量」
2. **ES 索引重建**（BM25 / Hybrid 需要）：打开「Debug」页，选择 BM25 或 Hybrid 模式后点击「重建 ES 索引」

```bash
# 或调用 API 重建 ES 全量索引
curl -X POST http://localhost:8080/api/search/index/rebuild
```

## 常用页面入口

| 页面 | 路径 | 说明 |
|------|------|------|
| 项目总览 | `/project-overview` | 能力导航与快速开始 |
| Dashboard | `/dashboard` | 平台统计 |
| 知识库 | `/kb` | 知识库与文档管理 |
| 问答 | `/chat` | Chat 问答与向量重建 |
| Debug 控制台 | `/debug` | 多模式检索与可观察链路 |
| Evaluation 评测中心 | `/evaluation` | 批量评测与三模式对比 |
| 系统状态 | `/system-status` | 后端 / DB / ES / Provider |
| RAG 指标 | `/rag-metrics` | 查询统计与慢查询 Top10 |
| 查询日志中心 | `/rag-query-logs` | 历史查询分页与筛选 |
| 慢查询分析 | `/slow-query-analysis` | 慢查询原因与优化建议 |
| 参数实验台 | `/rag-experiment` | Context 参数实验与多组对比 |

## 常见问题

### 后端启动失败，无法连接数据库

- 确认 `docker compose up -d` 已执行且 PostgreSQL 健康
- 检查 `application-dev.yml` 或环境变量中的数据库连接信息是否与 docker-compose 一致

### Vector / Hybrid 查询无结果

- 在「问答」页对当前知识库执行「重建向量」
- 确认 Chunk 已向量化（知识库文档页或 Debug 页可查看向量化状态）

### BM25 / Hybrid 查询无结果

- 在 Debug 页点击「重建 ES 索引」，或调用 `POST /api/search/index/rebuild`
- 确认 Elasticsearch 已启动：`curl http://localhost:9200`

### Chat 回答为 Mock 或调用模型失败

- 未配置 API Key 时使用 Mock Provider（见 `application.yml` / `application-dev.yml`）
- 配置 `DASHSCOPE_API_KEY`、`DEEPSEEK_API_KEY` 后重启后端，并在「系统状态」页确认 Provider

### 前端接口 404 或跨域

- 确认后端已在 8080 端口运行
- 使用 `npm run dev` 启动前端以启用 Vite 代理

### Evaluation / 指标 / 日志无数据

- 指标与日志数据来自 Debug 查询记录，请先在 Debug 页执行若干次查询

## 文档

- [docs/00-project-guideline.md](docs/00-project-guideline.md) — 项目规范
- [docs/02-architecture.md](docs/02-architecture.md) — 架构说明
- [docs/03-api-design.md](docs/03-api-design.md) — 接口设计
- [docs/05-development-plan.md](docs/05-development-plan.md) — 开发计划
- [docs/09-production-deployment.md](docs/09-production-deployment.md) — 生产环境部署
- [docs/10-aliyun-ecs-setup.md](docs/10-aliyun-ecs-setup.md) — 双服务器环境准备
- [deploy/app-layer/README.md](deploy/app-layer/README.md) — 应用入口层部署

## 目录结构

```
ai-rag-lab-platform/
├── README.md
├── docker-compose.yml
├── .env.example
├── .env.prod.example      # 生产环境变量模板
├── deploy/
│   ├── nginx.conf.example
│   ├── data-layer/
│   └── app-layer/           # 轻量服务器应用层
│       ├── .env.app.example
│       ├── nginx-rag.conf.example
│       └── README.md
├── scripts/
│   ├── run-backend-prod.sh
│   ├── check-ecs-env.sh
│   ├── check-data-layer.sh
│   ├── deploy-backend-app.sh
│   ├── deploy-frontend-app.sh
│   └── check-app-layer.sh
├── docs/
├── backend/          # Spring Boot
└── frontend/         # Vue 3 + Vite
```

## License

开源学习使用，详见仓库说明。
