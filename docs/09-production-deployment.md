# 09 - 生产环境部署说明

> V9 云部署与在线体验环境 · 双服务器生产部署  
> 应用入口与数据检索分层部署；公开文档使用占位符，真实 IP 仅写在本地 `.env.prod`（勿提交 Git）。

## 部署顺序

```text
1. 双服务器环境准备     → docs/10-aliyun-ecs-setup.md
2. 数据层 Compose 启动  → deploy/data-layer/README.md（ECS 上执行）
3. 数据层健康检查       → scripts/check-data-layer.sh <ECS_PRIVATE_IP>
4. 应用层环境检查       → scripts/check-ecs-env.sh（轻量服务器）
5. 配置 .env.prod       → POSTGRES / ES / REDIS 填 <ECS_PRIVATE_IP>
6. 后端打包与部署       → 轻量服务器运行 jar
7. 前端构建与部署       → dist → 轻量服务器 Nginx
8. Nginx 反代           → 对外 80/443，/api → 本机 8080
9. 验证                 → <LIGHT_SERVER_PUBLIC_IP>
```

---

## 双服务器部署拓扑

```text
                    用户浏览器
                         │
                         ▼
          ┌──────────────────────────────────────┐
          │  应用入口层 · 轻量服务器 2C4G          │
          │  公网：<LIGHT_SERVER_PUBLIC_IP>       │
          │  内网：<LIGHT_SERVER_PRIVATE_IP>      │
          │  ┌────────────────────────────────┐  │
          │  │ Nginx :80 / :443（对外唯一入口）│  │
          │  │   /      → frontend/dist       │  │
          │  │   /api/* → 127.0.0.1:8080     │  │
          │  └────────────────────────────────┘  │
          │  Spring Boot RAG（本机 8080）         │
          │  预留：AI 求职 Agent 前后端           │
          └──────────────┬───────────────────────┘
                         │ VPC 内网
                         ▼
          ┌──────────────────────────────────────┐
          │  数据检索层 · ECS 4C8G               │
          │  内网：<ECS_PRIVATE_IP>               │
          │  PostgreSQL + PgVector  :5432        │
          │  Elasticsearch        :9200        │
          │  Redis                  :6379        │
          │  （5432 / 9200 / 6379 不公网暴露）    │
          └──────────────────────────────────────┘
```

| 层级 | 规格 | 部署内容 | 公网暴露 |
|------|------|----------|----------|
| **应用入口层** | 轻量服务器 2C4G | Nginx、前端 `dist`、RAG Java 后端 | 80、443（及 SSH 22） |
| **数据检索层** | ECS 4C8G | PostgreSQL、Elasticsearch、Redis | **不暴露** PG / ES / Redis |

| 连接方式 | 说明 |
|----------|------|
| Java 后端 → 数据层 | 通过 **ECS 内网 IP**（`POSTGRES_HOST`、`ES_HOSTS` 等） |
| 浏览器 → 应用 | 访问轻量服务器公网 IP 或后续域名；前端 **同域** 请求 `/api` |
| 内网互通 | 两台机器 VPC 内网已互通后，在轻量服务器上 `curl` / `psql` 验证连通 |

**备案策略（当前）：** 暂不备案，先使用公网 IP `<LIGHT_SERVER_PUBLIC_IP>` 提供在线体验；后续可切换域名并配置 HTTPS。

---

## 数据与检索层部署（ECS）

PG、Elasticsearch、Redis **仅部署在 ECS**，通过 Docker Compose 统一启停；**不对公网开放** 5432 / 9200 / 6379，应用入口层经 **VPC 内网** `<ECS_PRIVATE_IP>` 访问。

| 文件 | 说明 |
|------|------|
| [deploy/data-layer/docker-compose.data.yml](../deploy/data-layer/docker-compose.data.yml) | PostgreSQL（PgVector）、Elasticsearch、Redis |
| [deploy/data-layer/.env.data.example](../deploy/data-layer/.env.data.example) | 数据层环境变量模板 |
| [deploy/data-layer/README.md](../deploy/data-layer/README.md) | 启动、停止、日志、状态、内网访问说明 |
| [scripts/check-data-layer.sh](../scripts/check-data-layer.sh) | 健康检查（支持传入 `<ECS_PRIVATE_IP>`） |

### 快速命令（在 ECS 上）

```bash
cd deploy/data-layer
cp .env.data.example .env.data    # 编辑密码，勿提交 Git
docker compose -f docker-compose.data.yml --env-file .env.data up -d
docker compose -f docker-compose.data.yml --env-file .env.data ps
```

### 从轻量服务器验证内网

```bash
./scripts/check-data-layer.sh <ECS_PRIVATE_IP>
```

Elasticsearch JVM 默认 `-Xms1g -Xmx2g`，适配 4C8G ECS，详见 `deploy/data-layer/README.md`。

**Redis：** 已作为数据层服务启动；RAG 业务代码尚未接入，无需改应用配置。

---

## 1. 后端生产配置

### 运行位置

- **jar 运行在轻量服务器**（应用入口层），监听 `127.0.0.1:8080` 或本机网卡，由 Nginx 反代。
- 数据库与检索服务在 **ECS 内网**，通过 `.env.prod` 中的 `<ECS_PRIVATE_IP>` 连接。

### 配置文件

| 文件 | 说明 |
|------|------|
| `backend/src/main/resources/application-prod.yml` | 生产 profile |
| `.env.prod.example` | 双服务器环境变量模板（复制为 `.env.prod`） |

### `.env.prod` 关键项（示例占位）

```bash
# 在轻量服务器上编辑，勿提交 Git
POSTGRES_HOST=<ECS_PRIVATE_IP>
ES_HOSTS=http://<ECS_PRIVATE_IP>:9200
REDIS_HOST=<ECS_PRIVATE_IP>
REDIS_PORT=6379
# Redis 当前仅基础设施预留，RAG 业务尚未接入
```

### Maven 打包

在**开发机或轻量服务器**执行：

```bash
cd backend
mvn -DskipTests clean package
```

产物：`backend/target/backend-0.0.1-SNAPSHOT.jar` → 上传至轻量服务器 `/opt/rag-lab/app`。

### 启动（轻量服务器）

```bash
chmod +x scripts/run-backend-prod.sh
./scripts/run-backend-prod.sh
```

`run-backend-prod.sh` 会加载项目根目录 `.env.prod` 并启用 `prod` profile。

### 首次数据库初始化

在**能访问 ECS 内网 PostgreSQL 的机器**（通常为轻量服务器）执行：

```bash
set -a && source .env.prod && set +a
psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f backend/src/main/resources/db/schema.sql
```

### 健康检查

```bash
# 轻量服务器本机
curl http://127.0.0.1:8080/api/system/health

# 经 Nginx（公网或本机）
curl http://<LIGHT_SERVER_PUBLIC_IP>/api/system/health
```

---

## 2. 前端生产构建

### 同域 `/api`（推荐）

前端与 API 均由轻量服务器 Nginx 对外提供，构建时：

```bash
cd frontend
cp .env.production.example .env.production
# VITE_API_BASE_URL 留空
npm install
npm run build
```

将 `frontend/dist/` 同步至轻量服务器 `/var/www/rag-lab/frontend/`。

| 变量 | 双服务器推荐值 |
|------|----------------|
| `VITE_API_BASE_URL` | **留空**（浏览器请求同域 `/api`） |

---

## 3. Nginx（仅应用入口层）

模板：[deploy/nginx.conf.example](../deploy/nginx.conf.example)

| 路径 | 目标 |
|------|------|
| `/` | 轻量服务器本地 `frontend/dist` |
| `/api/` | 轻量服务器本机 `127.0.0.1:8080` |

**禁止**在 Nginx 中将 PostgreSQL、Elasticsearch、Redis 端口反代到公网。

对外访问示例：`http://<LIGHT_SERVER_PUBLIC_IP>/`（备案前临时方案）。

---

## 4. 部署检查清单

| 步骤 | 操作 |
|------|------|
| 0 | 按 [10-aliyun-ecs-setup.md](10-aliyun-ecs-setup.md) 完成双机与安全组 |
| 1 | ECS：`deploy/data-layer` 启动 Compose；`check-data-layer.sh` 本机通过 |
| 2 | 轻量服务器：`check-data-layer.sh <ECS_PRIVATE_IP>` 内网通过 |
| 3 | 轻量服务器：`check-ecs-env.sh` 无 FAIL；`cp .env.prod.example .env.prod` 并填写 |
| 4 | 初始化 `schema.sql`（首次） |
| 5 | 轻量服务器：打包并启动 jar |
| 6 | 构建前端 `dist` 并 rsync 到 Nginx root |
| 7 | 配置 Nginx，重载 |
| 8 | 浏览器访问 `<LIGHT_SERVER_PUBLIC_IP>`，初始化样例数据、重建向量与 ES 索引 |

---

## 5. 常见问题

### 后端无法连接 PostgreSQL / ES

- 确认 `.env.prod` 使用 **ECS 内网 IP**，非公网 IP  
- 确认 ECS 安全组允许 **轻量服务器内网 IP** 访问 5432 / 9200 / 6379  
- 数据层安全组 **不要** 对 `0.0.0.0/0` 开放上述端口  

### 公网能打开页面但 API 失败

- 检查轻量服务器 Nginx `location /api/` 与后端 8080  
- 检查后端日志中的数据库 / ES 连接错误  

### Redis

- 已在 ECS 部署 Redis 时，`.env.prod` 可预先填写 `REDIS_HOST` / `REDIS_PORT`  
- **当前 RAG 业务代码未使用 Redis**，无需为 Redis 改应用配置  

---

## 相关文件

| 路径 | 用途 |
|------|------|
| [deploy/data-layer/README.md](../deploy/data-layer/README.md) | 数据层 Compose 启停与日志 |
| [deploy/data-layer/docker-compose.data.yml](../deploy/data-layer/docker-compose.data.yml) | PG / ES / Redis 定义 |
| [scripts/check-data-layer.sh](../scripts/check-data-layer.sh) | 数据层健康检查 |
| [10-aliyun-ecs-setup.md](10-aliyun-ecs-setup.md) | 双服务器环境准备 |
| `.env.prod.example` | 生产环境变量（含内网占位符） |
| `deploy/nginx.conf.example` | 应用入口层 Nginx |
| `scripts/check-ecs-env.sh` | 轻量服务器环境检查 |
| `scripts/run-backend-prod.sh` | 启动后端 jar |
