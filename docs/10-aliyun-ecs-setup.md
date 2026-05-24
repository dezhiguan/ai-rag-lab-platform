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

### 2.3 数据层组件安装（概要）

在 ECS 上安装并配置（具体命令按所选发行版调整）：

1. **PostgreSQL 16 + PgVector 扩展**  
2. **Elasticsearch 8.x**（单节点，生产注意 `vm.max_map_count`）  
3. **Redis**（供后续 Agent / 缓存场景预留；RAG 当前业务未接入）

创建数据库与用户示例：

```bash
# 在 ECS 上执行（示例）
sudo -u postgres psql -c "CREATE USER rag_user WITH PASSWORD '***';"
sudo -u postgres psql -c "CREATE DATABASE rag_lab OWNER rag_user;"
# 安装 pgvector 扩展后：
psql -d rag_lab -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 2.4 内网连通验证（在轻量服务器执行）

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
2. 完成数据层 PG / ES / Redis 安装与安全组  
3. 完成应用层初始化与 `check-ecs-env.sh`  
4. 按 [09-production-deployment.md](09-production-deployment.md) 部署 jar、dist、Nginx  
5. 通过 `<LIGHT_SERVER_PUBLIC_IP>` 验证在线体验  

---

## 相关文档

- [09-production-deployment.md](09-production-deployment.md) — 打包、构建、Nginx、检查清单  
- [deploy/nginx.conf.example](../deploy/nginx.conf.example) — 应用入口层 Nginx  
- [.env.prod.example](../.env.prod.example) — 环境变量模板  
