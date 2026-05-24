# 08 - 生产环境部署说明

> V9 云部署与在线体验环境 · 部署前准备文档  
> 本次仅做配置与说明准备，不涉及实际阿里云资源创建。

## 架构概览

```text
用户浏览器
    │
    ▼
Nginx（80/443）
    ├── /          → frontend/dist 静态资源
    └── /api/*     → Spring Boot jar（127.0.0.1:8080）
                          │
                          ├── PostgreSQL（RDS / 自建）
                          └── Elasticsearch（云服务 / 自建）
```

推荐：**同域部署**（Nginx 反代 `/api`），前端 `VITE_API_BASE_URL` 留空即可。

---

## 1. 后端生产配置

### 配置文件

| 文件 | 说明 |
|------|------|
| `backend/src/main/resources/application-prod.yml` | 生产 profile：关闭 SQL 日志与 schema 自动初始化、关闭 API 文档 |
| `.env.prod.example` | 生产环境变量模板，复制为 `.env.prod` 后填入真实值 |

### 与 dev 的差异

| 项 | dev | prod |
|----|-----|------|
| Profile | `dev` | `prod` |
| SQL 日志 | 控制台输出 | 关闭 |
| schema.sql | 每次启动执行 | `never`（首次需手动初始化） |
| Knife4j / Swagger | 开启 | 默认关闭 |
| 上传目录 | `./data/uploads` | `/var/lib/rag-lab/uploads`（可 env 覆盖） |

### Maven 打包

```bash
cd backend
mvn -DskipTests clean package
```

产物路径：

```text
backend/target/backend-0.0.1-SNAPSHOT.jar
```

### 加载 `.env.prod` 并启动 jar

**方式一：启动脚本（推荐）**

```bash
chmod +x scripts/run-backend-prod.sh
./scripts/run-backend-prod.sh
```

**方式二：手动加载环境变量**

```bash
set -a && source .env.prod && set +a
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

**方式三：仅指定 profile（变量已在 systemd / 云平台注入）**

```bash
SPRING_PROFILES_ACTIVE=prod java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

### 首次部署数据库初始化

生产 profile 不会自动执行 `schema.sql`。首次部署请任选其一：

```bash
# 方式 A：临时用 dev profile 启动一次（仅初始化，慎用生产库）
# 方式 B：手动执行 schema
psql -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB -f backend/src/main/resources/db/schema.sql
```

### 健康检查

```bash
curl http://127.0.0.1:8080/api/system/health
```

---

## 2. 前端生产构建

### 环境变量

| 文件 | 说明 |
|------|------|
| `frontend/.env.production.example` | 生产构建 API 地址模板 |

复制并按需修改：

```bash
cd frontend
cp .env.production.example .env.production
```

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | 后端 API 根地址。**同域 Nginx 反代时留空**；前后端分域时填完整 URL，如 `https://api.example.com` |

配置读取位置：`frontend/src/config/api.ts` → 所有 Axios 实例统一使用。

### 构建命令

```bash
cd frontend
npm install
npm run build
```

### dist 目录说明

构建完成后生成 `frontend/dist/`：

```text
frontend/dist/
├── index.html          # SPA 入口
├── assets/             # JS / CSS / 静态资源（带 hash）
└── favicon.ico         # 如有
```

部署时将 **dist 目录内全部文件** 复制到 Nginx `root` 所指路径，例如：

```bash
sudo mkdir -p /var/www/rag-lab/frontend
sudo rsync -av --delete frontend/dist/ /var/www/rag-lab/frontend/
```

本地预览构建结果（可选）：

```bash
cd frontend
npm run preview
```

---

## 3. Nginx 反向代理

模板文件：[deploy/nginx.conf.example](../deploy/nginx.conf.example)

| 路径 | 行为 |
|------|------|
| `/` | 静态资源 + `try_files` 支持 Vue Router history |
| `/api/` | 转发到 `127.0.0.1:8080` |

### HTTPS（预留）

1. 在阿里云 SSL 证书服务申请或上传证书  
2. 下载 Nginx 格式，放置于 `/etc/nginx/ssl/your_domain.com/`  
3. 参考 `nginx.conf.example` 底部 HTTPS `server` 块注释，取消注释并替换域名与证书路径  
4. HTTP 可配置 301 跳转至 HTTPS  

---

## 4. 部署检查清单

| 步骤 | 命令 / 操作 |
|------|-------------|
| 1. 准备 `.env.prod` | `cp .env.prod.example .env.prod` 并填写 |
| 2. 初始化数据库 | 执行 `schema.sql`（首次） |
| 3. 打包后端 | `cd backend && mvn -DskipTests package` |
| 4. 启动后端 | `./scripts/run-backend-prod.sh` |
| 5. 构建前端 | `cd frontend && npm install && npm run build` |
| 6. 部署静态资源 | 复制 `dist/` 到 Nginx root |
| 7. 配置 Nginx | 参考 `deploy/nginx.conf.example` |
| 8. 验证 | 访问站点、`/api/system/health`、初始化样例数据 |

---

## 5. 常见问题

### 生产启动报数据库连接失败

- 检查 RDS 白名单是否包含 ECS 内网 IP  
- 确认 `.env.prod` 中 `POSTGRES_*` 与 RDS 一致  

### 前端页面正常但 API 404

- 确认 Nginx `location /api/` 已配置且后端在 8080 监听  
- 若前后端分域部署，检查 `VITE_API_BASE_URL` 是否指向正确 API 地址  

### BM25 / Hybrid 无结果

- 确认 ES 可达且 `.env.prod` 中 `ES_HOSTS` 正确  
- 部署后需执行 ES 索引重建：`POST /api/search/index/rebuild`  

### 上传文件失败

- 确认 `RAG_STORAGE_PATH` 目录存在且 Java 进程有读写权限  

---

## 相关文件索引

| 路径 | 用途 |
|------|------|
| `.env.prod.example` | 生产环境变量模板 |
| `backend/src/main/resources/application-prod.yml` | 后端 prod profile |
| `frontend/.env.production.example` | 前端生产 API 配置模板 |
| `deploy/nginx.conf.example` | Nginx 反代模板 |
| `scripts/run-backend-prod.sh` | 加载 env 并启动 jar |
