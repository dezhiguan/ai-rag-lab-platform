# 11 - 双服务器实机部署与联调验收 Runbook

> V9-06 · 在 ECS 与轻量服务器上执行实机部署与联调  
> **公开文档仅使用占位符**；真实 IP、API Key 写入本地 `deploy/PRIVATE-DEPLOYMENT-NOTES.local.md`（已 gitignore，勿提交）。

---

## 0. 验收目标

| 项 | 验收标准 |
|----|----------|
| 数据层 | ECS 上 PG / ES / Redis Compose 运行，`check-data-layer.sh` 通过 |
| 应用层 | 轻量服务器 jar + Nginx + dist 运行，`check-app-layer.sh` 通过 |
| 公网访问 | `http://<LIGHT_SERVER_PUBLIC_IP>/` 可打开前端 |
| API | `http://<LIGHT_SERVER_PUBLIC_IP>/api/system/health` 返回 200 |
| 内网 | 后端经 `<ECS_PRIVATE_IP>` 连接 PG / ES / Redis |
| 业务 | 样例数据、向量、ES 索引就绪；Debug / Evaluation / 实验台可用 |

---

## 1. ECS：启动数据与检索层

SSH 登录 **ECS 4C8G**，进入项目目录：

```bash
cd /path/to/ai-rag-lab-platform/deploy/data-layer
cp .env.data.example .env.data
# 编辑 POSTGRES_PASSWORD 等（与轻量服务器 .env.app 中数据库账号一致）

sudo sysctl -w vm.max_map_count=262144
docker compose -f docker-compose.data.yml --env-file .env.data up -d
docker compose -f docker-compose.data.yml --env-file .env.data ps
```

本机检查：

```bash
cd /path/to/ai-rag-lab-platform
./scripts/check-data-layer.sh
```

---

## 2. 轻量服务器：配置 `.env.app`

SSH 登录 **轻量 2C4G**：

```bash
cd /path/to/ai-rag-lab-platform
cp deploy/app-layer/.env.app.example deploy/app-layer/.env.app
```

编辑 `deploy/app-layer/.env.app`（**勿提交 Git**）：

```bash
POSTGRES_HOST=<ECS_PRIVATE_IP>
ES_HOSTS=http://<ECS_PRIVATE_IP>:9200
REDIS_HOST=<ECS_PRIVATE_IP>
# POSTGRES_PASSWORD / ES 等与 deploy/data-layer/.env.data 一致
DASHSCOPE_API_KEY=<your-key>
DEEPSEEK_API_KEY=<your-key>
```

验证内网连通：

```bash
./scripts/check-data-layer.sh <ECS_PRIVATE_IP>
```

---

## 3. 轻量服务器：部署后端

```bash
cd /path/to/ai-rag-lab-platform
cd backend && mvn -DskipTests clean package && cd ..
./scripts/deploy-backend-app.sh
curl -s http://127.0.0.1:8080/api/system/health
```

首次部署初始化数据库（在轻量服务器，已 source env 后）：

```bash
set -a && source deploy/app-layer/.env.app && set +a
psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f backend/src/main/resources/db/schema.sql
```

---

## 4. 轻量服务器：部署前端与 Nginx

```bash
./scripts/deploy-frontend-app.sh

sudo cp deploy/app-layer/nginx-rag.conf.example /etc/nginx/sites-available/rag-lab
sudo ln -sf /etc/nginx/sites-available/rag-lab /etc/nginx/sites-enabled/rag-lab
sudo nginx -t && sudo systemctl reload nginx
```

---

## 5. 联调验收

在轻量服务器或本机（可访问公网 IP 时）：

```bash
export PUBLIC_BASE_URL="http://<LIGHT_SERVER_PUBLIC_IP>"
export ECS_PRIVATE_IP="<ECS_PRIVATE_IP>"
./scripts/verify-online-deployment.sh
./scripts/check-app-layer.sh "$ECS_PRIVATE_IP"
```

---

## 6. 初始化线上数据

```bash
export PUBLIC_BASE_URL="http://<LIGHT_SERVER_PUBLIC_IP>"
./scripts/init-online-data.sh
```

脚本将依次：

1. `POST /api/sample/init`  
2. `POST /api/kb/{kbId}/embedding/rebuild`  
3. `POST /api/search/index/rebuild`  

然后在浏览器验证：

- `/debug` — Hybrid 查询  
- `/evaluation` — 评测  
- `/rag-experiment` — 参数实验  
- `/rag-metrics`、`/rag-query-logs` — 指标与日志  

---

## 7. 部署检查清单（实机）

| # | 位置 | 操作 | 验证 |
|---|------|------|------|
| 1 | ECS | Compose up | `check-data-layer.sh` 本机 PASS |
| 2 | 轻量 | `.env.app` 填 ECS 内网 | `check-data-layer.sh <ECS_PRIVATE_IP>` |
| 3 | 轻量 | schema.sql（首次） | psql 无报错 |
| 4 | 轻量 | deploy-backend-app.sh | curl 127.0.0.1:8080/api/system/health |
| 5 | 轻量 | deploy-frontend-app.sh + Nginx | curl 127.0.0.1/ 有 index.html |
| 6 | 公网 | 浏览器 | `<LIGHT_SERVER_PUBLIC_IP>` 打开页面 |
| 7 | 公网 | API | `/api/system/health`、system/status 中 PG/ES UP |
| 8 | 公网 | init-online-data.sh | 样例 KB、向量、ES 索引 |
| 9 | 公网 | Debug / Evaluation | 核心页面可用 |

---

## 8. 常见问题与排查

### 轻量服务器 80 端口 Connection refused

- 检查 Nginx：`sudo systemctl status nginx`  
- 安全组是否开放 80  
- `sudo nginx -t` 配置语法  

### `/api/system/health` 502

- 后端是否运行：`cat /opt/rag-lab/app/rag-backend.pid`  
- 日志：`tail -f /opt/rag-lab/app/logs/backend.log`  
- Nginx `proxy_pass` 是否指向 8080  

### 后端启动失败：数据库连接

- `.env.app` 中 `POSTGRES_HOST` 必须为 **ECS 内网 IP**  
- ECS 安全组是否允许 **轻量内网 IP** 访问 5432  
- 数据层 Compose 是否 healthy  

### BM25 / Hybrid 无结果

- `POST /api/search/index/rebuild` 是否成功  
- `ES_HOSTS` 是否可达：`curl http://<ECS_PRIVATE_IP>:9200`（从轻量服务器）  

### Chat / Embedding 失败

- 检查 `DASHSCOPE_API_KEY`、`DEEPSEEK_API_KEY`  
- `/system-status` 页 Provider 状态  

### Elasticsearch 内网 9200 无法访问（安全组）

- 在 ECS 安全组放行 **轻量服务器内网 IP** → **9200/tcp**（推荐）  
- 临时方案：轻量服务器 SSH 隧道 + `systemd` 服务 `rag-es-tunnel`，`.env.app` 中 `ES_HOSTS=http://127.0.0.1:9200`  
- 验证：`curl http://127.0.0.1:9200`（在轻量服务器上）

### Elasticsearch OOM

- 降低 `.env.data` 中 `ES_JAVA_OPTS`，如 `-Xms512m -Xmx1g`  
- `docker stats` 观察内存  

---

## 9. 本地私有部署笔记（勿提交）

```bash
cp deploy/PRIVATE-DEPLOYMENT-NOTES.local.example deploy/PRIVATE-DEPLOYMENT-NOTES.local.md
# 记录真实公网/内网 IP、SSH 用户、部署日期、验收结果
```

---

## 相关文档

- [09-production-deployment.md](09-production-deployment.md)  
- [deploy/data-layer/README.md](../deploy/data-layer/README.md)  
- [deploy/app-layer/README.md](../deploy/app-layer/README.md)  
