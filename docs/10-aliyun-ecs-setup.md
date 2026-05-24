# 10 - 双服务器环境准备

> V9 · 阿里云双服务器部署  
> **应用入口层**（轻量 2C4G）+ **数据检索层**（ECS 4C8G）。公开文档仅使用占位符；真实 IP 写入本地 `.env.prod`，勿提交仓库。

部署流程：先准备 **数据检索层 ECS**，再准备 **应用入口层轻量服务器**，最后按 [09-production-deployment.md](09-production-deployment.md) 部署应用。

---

## 1. 部署拓扑总览

```text
┌─ 应用入口层（轻量服务器 2C4G）────────────────────────┐
│ 公网：<LIGHT_SERVER_PUBLIC_IP>                        │
│ 内网：<LIGHT_SERVER_PRIVATE_IP>                        │
│ · Nginx（80 / 443 对外）                              │
│ · 前端 dist                                           │
│ · RAG Java 后端（8080 仅本机，由 Nginx 反代 /api）     │
│ · 预留 AI 求职 Agent 前端 / 后端                      │
└───────────────────────┬───────────────────────────────┘
                        │ VPC 内网（已验证互通）
┌─ 数据检索层（ECS 4C8G）───────────────────────────────┐
│ 内网：<ECS_PRIVATE_IP>（无 PG/ES/Redis 公网暴露）      │
│ · PostgreSQL + PgVector  :5432                        │
│ · Elasticsearch          :9200                        │
│ · Redis                    :6379                        │
└───────────────────────────────────────────────────────┘
```

| 占位符 | 含义 |
|--------|------|
| `<LIGHT_SERVER_PUBLIC_IP>` | 轻量服务器公网 IP（浏览器访问入口） |
| `<LIGHT_SERVER_PRIVATE_IP>` | 轻量服务器 VPC 内网 IP |
| `<ECS_PRIVATE_IP>` | 数据层 ECS 内网 IP（写入 `.env.prod`） |

**当前在线体验策略：** 暂不备案，通过 `<LIGHT_SERVER_PUBLIC_IP>` 访问；后续可绑定域名并配置 HTTPS。

---

## 2. 数据检索层（ECS 4C8G）

### 2.1 规格与系统

| 项 | 建议 |
|----|------|
| 规格 | 4 核 8 GiB |
| 系统 | Ubuntu 22.04 LTS 或 Alibaba Cloud Linux 3 |
| 系统盘 | ≥ 80 GiB（ES 与 PG 数据占用） |

### 2.2 安全组（数据层）

| 端口 | 服务 | 公网 | 内网访问来源 |
|------|------|------|--------------|
| 22 | SSH | 建议限制来源 IP | — |
| 5432 | PostgreSQL | ❌ 不开放 | 仅 `<LIGHT_SERVER_PRIVATE_IP>` / VPC |
| 9200 | Elasticsearch | ❌ 不开放 | 仅应用入口层内网 IP |
| 6379 | Redis | ❌ 不开放 | 仅应用入口层内网 IP |

**原则：** 数据层 **不对 0.0.0.0/0 开放** 5432、9200、6379。

### 2.3 使用 Docker Compose 启动数据层（推荐）

在 ECS 上安装 Docker 后，使用项目自带 Compose 一键启动 PG / ES / Redis：

```bash
cd deploy/data-layer
cp .env.data.example .env.data
# 编辑 POSTGRES_PASSWORD 等，勿提交 .env.data

sudo sysctl -w vm.max_map_count=262144   # Elasticsearch 必需（首次）

docker compose -f docker-compose.data.yml --env-file .env.data up -d
docker compose -f docker-compose.data.yml --env-file .env.data ps
```

详细说明：[deploy/data-layer/README.md](../deploy/data-layer/README.md)

### 2.4 Elasticsearch 内存（4C8G ECS）

| 项 | 说明 |
|----|------|
| 默认 JVM | `ES_JAVA_OPTS=-Xms1g -Xmx2g`（见 `.env.data.example`） |
| 原则 | ES 堆不宜超过 ECS 内存一半；为 PG、Redis、OS 预留 3～4GiB |
| 调优 | 负载升高可在 `.env.data` 中将 `-Xmx` 调至 `2g`；用 `docker stats` 观察 |
| 系统参数 | `vm.max_map_count=262144`，写入 `/etc/sysctl.conf` 持久化 |

### 2.5 数据层健康检查

**ECS 本机：**

```bash
chmod +x scripts/check-data-layer.sh
./scripts/check-data-layer.sh
```

**轻量服务器（内网）：**

```bash
./scripts/check-data-layer.sh <ECS_PRIVATE_IP>
```

可选：设置 `POSTGRES_PASSWORD` 等环境变量后执行，以启用 `psql` / `redis-cli` 深度检查。

### 2.6 内网连通验证（在轻量服务器执行）

将 `<ECS_PRIVATE_IP>` 替换为实际内网地址（仅写在本地，勿写入公开文档提交）：

```bash
# PostgreSQL
psql -h <ECS_PRIVATE_IP> -U rag_user -d rag_lab -c "SELECT 1"

# Elasticsearch
curl -s "http://<ECS_PRIVATE_IP>:9200"

# Redis（可选）
redis-cli -h <ECS_PRIVATE_IP> ping
```

---

## 3. 应用入口层（轻量服务器 2C4G）

### 3.1 规格与安全组

| 项 | 建议 |
|----|------|
| 规格 | 2 核 4 GiB |
| 公网开放 | 22（SSH）、80（HTTP）、443（HTTPS 预留） |
| 8080 | ❌ **不对公网开放**；仅本机监听，由 Nginx 反代 `/api` |

### 3.2 目录规划

| 路径 | 用途 |
|------|------|
| `/opt/rag-lab/app` | 代码、jar、`.env.prod` |
| `/var/www/rag-lab/frontend` | Nginx 静态资源（`dist`） |
| `/var/lib/rag-lab/uploads` | 文档上传（`RAG_STORAGE_PATH`） |
| `/opt/agent-job`（预留） | 未来 AI 求职 Agent，与 RAG 目录隔离 |

### 3.3 初始化步骤（轻量服务器）

```bash
# 部署用户
sudo adduser raglab
sudo su - raglab

# 基础工具 + JDK 17 + Node 18+ + Nginx + PostgreSQL 客户端
sudo apt update
sudo apt install -y curl git unzip rsync ca-certificates openjdk-17-jdk nginx postgresql-client

# Node.js（示例：NodeSource 20.x）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 目录
sudo mkdir -p /opt/rag-lab/app /var/www/rag-lab/frontend /var/lib/rag-lab/uploads
sudo chown -R raglab:raglab /opt/rag-lab /var/lib/rag-lab/uploads
sudo chown -R raglab:www-data /var/www/rag-lab/frontend
sudo chmod -R 775 /var/www/rag-lab/frontend
```

### 3.4 环境检查

在轻量服务器项目目录执行：

```bash
chmod +x scripts/check-ecs-env.sh
./scripts/check-ecs-env.sh
```

### 3.5 配置 `.env.prod`

```bash
cp .env.prod.example .env.prod
# 编辑：POSTGRES_HOST、ES_HOSTS、REDIS_HOST 使用 <ECS_PRIVATE_IP>
```

详见 [.env.prod.example](../.env.prod.example)。

---

## 4. 服务隔离（同机多项目预留）

RAG 与 **AI 求职 Agent** 可共用 **应用入口层轻量服务器**，须隔离：

| 维度 | RAG Lab | AI 求职 Agent（未来） |
|------|---------|------------------------|
| 端口 | 8080 | 独立端口（如 8081） |
| 目录 | `/opt/rag-lab` | `/opt/agent-job` |
| Nginx | `/`、`/api` | 独立 `location` 或子域名 |
| 配置 | `.env.prod` | 独立 env 文件 |

数据层 Redis 可先部署在 ECS，供 Agent 与后续 RAG 缓存共用，通过 **不同 DB index** 或 key 前缀隔离（实施时再定）。

**本项目 V9-03 范围：** 仅文档与配置体现双服务器 RAG 部署，**不实现** Agent 功能代码。

---

## 5. 下一步

1. 确认两台机器 **VPC 内网互通**  
2. ECS：按 [deploy/data-layer/README.md](../deploy/data-layer/README.md) 启动 Compose 并通过 `check-data-layer.sh`  
3. 轻量服务器：初始化与 `check-ecs-env.sh`  
4. 按 [09-production-deployment.md](09-production-deployment.md) 部署 jar、dist、Nginx  
5. 通过 `<LIGHT_SERVER_PUBLIC_IP>` 验证在线体验  

---

## 相关文档

- [09-production-deployment.md](09-production-deployment.md) — 应用层部署  
- [deploy/data-layer/README.md](../deploy/data-layer/README.md) — 数据层 Compose  
- [deploy/nginx.conf.example](../deploy/nginx.conf.example) — 应用入口层 Nginx  
- [.env.prod.example](../.env.prod.example) — 环境变量模板  
