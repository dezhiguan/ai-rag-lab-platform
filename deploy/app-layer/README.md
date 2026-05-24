# 应用入口层部署（轻量服务器）

在 **轻量服务器 2C4G** 上标准化部署：

- **Nginx** — 对外 80 / 443，统一入口  
- **前端** `dist` — 静态资源  
- **RAG Java 后端** — 本机 `8080`，由 Nginx 反代 `/api`  

数据服务（PostgreSQL、Elasticsearch、Redis）在 **ECS 内网** `<ECS_PRIVATE_IP>`，由后端经 VPC 访问，**不经过 Nginx 暴露**。

---

## 1. 依赖安装

| 依赖 | 用途 |
|------|------|
| OpenJDK 17 | 运行后端 jar |
| Node.js 18+、npm | 构建前端（可在 CI 或本机构建后 rsync） |
| Nginx | 静态资源 + `/api` 反代 |
| curl、rsync | 健康检查与同步 dist |
| Maven（可选） | 在服务器上打包后端 |

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk nginx curl rsync
# Node.js 见 docs/10-aliyun-ecs-setup.md
```

---

## 2. 目录规划

| 路径 | 说明 |
|------|------|
| `/opt/rag-lab/app` | 后端 jar、`.env.app`、PID、日志（可由 `APP_DEPLOY_DIR` 覆盖） |
| `/var/www/rag-lab/frontend` | Nginx `root`，前端 `dist` |
| `/var/lib/rag-lab/uploads` | `RAG_STORAGE_PATH` 上传目录 |

```bash
sudo mkdir -p /opt/rag-lab/app /var/www/rag-lab/frontend /var/lib/rag-lab/uploads
sudo chown -R "$USER" /opt/rag-lab /var/lib/rag-lab/uploads
```

---

## 3. 配置环境变量

```bash
cd /path/to/ai-rag-lab-platform
cp deploy/app-layer/.env.app.example deploy/app-layer/.env.app
# 编辑：POSTGRES_HOST、ES_HOSTS、REDIS_HOST → <ECS_PRIVATE_IP>
#       DASHSCOPE_API_KEY、DEEPSEEK_API_KEY、数据库密码
```

`.env.app` 已加入 `.gitignore`，勿提交仓库。

也可继续使用项目根目录 `.env.prod`（内容字段相同）；`deploy-backend-app.sh` 优先加载 `deploy/app-layer/.env.app`。

---

## 4. 部署后端

```bash
# 本地或服务器先打包
cd backend && mvn -DskipTests clean package && cd ..

# 部署并后台启动（自动停止旧进程）
chmod +x scripts/deploy-backend-app.sh
./scripts/deploy-backend-app.sh
```

脚本将：

1. 创建 `APP_DEPLOY_DIR`（默认 `/opt/rag-lab/app`）  
2. 复制 `backend/target/backend-0.0.1-SNAPSHOT.jar` → `backend.jar`  
3. 检查 `deploy/app-layer/.env.app`  
4. 停止已有后端进程（PID 文件）  
5. 以 `prod` profile 启动并输出健康检查地址  

本机健康检查：

```bash
curl -s http://127.0.0.1:8080/api/system/health
```

---

## 5. 部署前端

```bash
chmod +x scripts/deploy-frontend-app.sh
./scripts/deploy-frontend-app.sh
```

脚本将：

1. `npm install` + `npm run build`（可用 `SKIP_BUILD=1` 跳过）  
2. `rsync` `frontend/dist/` → `NGINX_STATIC_DIR`（默认 `/var/www/rag-lab/frontend`）  

浏览器访问（占位符）：

```text
http://<LIGHT_SERVER_PUBLIC_IP>/
```

前端同域请求 `/api`，构建时 `VITE_API_BASE_URL` 留空。

---

## 6. 配置 Nginx

```bash
sudo cp deploy/app-layer/nginx-rag.conf.example /etc/nginx/sites-available/rag-lab
sudo ln -sf /etc/nginx/sites-available/rag-lab /etc/nginx/sites-enabled/rag-lab
sudo nginx -t && sudo systemctl reload nginx
```

经 Nginx 健康检查：

```bash
curl -s http://127.0.0.1/api/system/health
# 或
curl -s http://<LIGHT_SERVER_PUBLIC_IP>/api/system/health
```

---

## 7. 应用层健康检查

```bash
chmod +x scripts/check-app-layer.sh

# 本机检查
./scripts/check-app-layer.sh

# 同时检查到 ECS 数据层端口（传入内网 IP，勿写入公开文档）
./scripts/check-app-layer.sh <ECS_PRIVATE_IP>
```

---

## 8. 连接 ECS 数据层

部署后端前，确认数据层已启动：

```bash
./scripts/check-data-layer.sh <ECS_PRIVATE_IP>
```

`.env.app` 中数据层地址必须使用 **ECS 内网 IP**，与 [deploy/data-layer/README.md](../data-layer/README.md) 中 Compose 配置的用户名、密码一致。

---

## 9. 停止后端

```bash
kill "$(cat /opt/rag-lab/app/rag-backend.pid)"   # 默认路径
# 或重新执行 deploy-backend-app.sh（会先停止旧进程）
```

---

## 相关脚本与文档

| 路径 | 说明 |
|------|------|
| [scripts/deploy-backend-app.sh](../../scripts/deploy-backend-app.sh) | 后端部署 |
| [scripts/deploy-frontend-app.sh](../../scripts/deploy-frontend-app.sh) | 前端构建与同步 |
| [scripts/check-app-layer.sh](../../scripts/check-app-layer.sh) | 应用层检查 |
| [docs/09-production-deployment.md](../../docs/09-production-deployment.md) | 总览 |
| [deploy/data-layer/README.md](../data-layer/README.md) | 数据层 Compose |
