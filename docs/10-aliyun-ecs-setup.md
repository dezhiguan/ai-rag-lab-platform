# 10 - 阿里云 ECS 环境准备

> V9-02 · 阿里云 ECS 部署环境准备  
> 本文说明 ECS 规格建议、安全组、初始化步骤与环境检查；**不在本次任务中实际创建或连接云资源**。

部署 RAG 服务前，请先完成本文步骤，再按 [09-production-deployment.md](09-production-deployment.md) 执行后端部署、前端部署与 Nginx 配置。

---

## 1. 服务器推荐配置

| 项 | 说明 |
|----|------|
| **最低规格** | 2 核 CPU、4 GiB 内存 |
| **推荐规格** | 4 核 CPU、8 GiB 内存（含 Elasticsearch 客户端连接、构建前端时更从容） |
| **操作系统** | Ubuntu 22.04 LTS **或** Alibaba Cloud Linux 3 |
| **系统盘** | 建议 ≥ 40 GiB（日志、构建缓存、上传文件预留空间） |

### 安全组与端口

| 端口 | 方向 | 用途 | 是否公网开放 |
|------|------|------|--------------|
| **22** | 入站 | SSH 运维 | ✅ 建议限制来源 IP |
| **80** | 入站 | HTTP（Nginx） | ✅ |
| **443** | 入站 | HTTPS（Nginx） | ✅ |
| **8080** | — | Spring Boot 后端 | ❌ **仅本机或内网**，由 Nginx 反代 `/api` |
| **5432** | — | PostgreSQL | ❌ **不要公网暴露**（使用 RDS 内网或 VPC 内自建） |
| **9200** | — | Elasticsearch | ❌ **不要公网暴露**（使用内网 ES 或云服务内网地址） |

原则：**对外只暴露 Nginx（80/443）**；数据库与 ES 通过 VPC 内网或安全组白名单访问。

---

## 2. 目录规划（RAG 服务）

| 路径 | 用途 | 属主建议 |
|------|------|----------|
| `/opt/rag-lab` | 项目代码、jar、`.env.prod`、构建产物 | 部署用户 `raglab` |
| `/var/www/rag-lab/frontend` | Nginx 静态资源（`dist` 部署目录） | `www-data` 或 Nginx 用户，部署用户可写 |
| `/var/lib/rag-lab/uploads` | 文档上传存储（对应 `RAG_STORAGE_PATH`） | 运行后端的用户 |

---

## 3. ECS 初始化步骤

以下命令以 **Ubuntu 22.04** 为例；Alibaba Cloud Linux 3 可将 `apt` 换为 `yum` / `dnf`，包名略有差异。

### 3.1 创建部署用户

```bash
sudo adduser raglab
sudo usermod -aG sudo raglab   # 如需 sudo；生产可改为最小权限
sudo su - raglab
```

后续部署与构建建议在该用户下执行，避免长期使用 root。

### 3.2 安装基础工具

```bash
sudo apt update
sudo apt install -y curl git unzip rsync ca-certificates gnupg
```

### 3.3 安装 JDK 17

```bash
sudo apt install -y openjdk-17-jdk
java -version   # 应显示 17.x
```

### 3.4 安装 Node.js 18+

推荐使用 NodeSource（Ubuntu 22.04）：

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v && npm -v
```

### 3.5 安装 Nginx

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 3.6 安装 PostgreSQL 客户端

用于首次执行 `schema.sql`、运维排查（数据库本身建议 RDS 内网）：

```bash
sudo apt install -y postgresql-client
psql --version
```

### 3.7 创建部署目录与上传目录

```bash
sudo mkdir -p /opt/rag-lab
sudo mkdir -p /var/www/rag-lab/frontend
sudo mkdir -p /var/lib/rag-lab/uploads

sudo chown -R raglab:raglab /opt/rag-lab
sudo chown -R raglab:raglab /var/lib/rag-lab/uploads
# 静态目录：部署用户可写，Nginx 可读（按实际 Nginx 用户调整）
sudo chown -R raglab:www-data /var/www/rag-lab/frontend
sudo chmod -R 775 /var/www/rag-lab/frontend
sudo chmod -R 750 /var/lib/rag-lab/uploads
```

### 3.8 克隆项目（示例）

```bash
cd /opt/rag-lab
git clone <your-repo-url> app
cd app
```

### 3.9 运行环境检查

```bash
chmod +x scripts/check-ecs-env.sh
./scripts/check-ecs-env.sh
```

全部 **PASS** 或仅剩 **WARN**（如 80/443 尚未配置站点）后，继续 [09-production-deployment.md](09-production-deployment.md) 中的部署流程。

---

## 4. 环境检查脚本

| 文件 | 说明 |
|------|------|
| [scripts/check-ecs-env.sh](../scripts/check-ecs-env.sh) | 检查 Java、Node、Nginx、curl、目录、写权限、80/443 端口 |

可覆盖默认路径：

```bash
DEPLOY_ROOT=/opt/rag-lab/app \
UPLOAD_DIR=/var/lib/rag-lab/uploads \
FRONTEND_DIR=/var/www/rag-lab/frontend \
./scripts/check-ecs-env.sh
```

---

## 5. 与同机其他服务的部署隔离（预留）

未来 **AI 求职 Agent** 可与 **RAG Lab** 部署在同一台 ECS，但须严格隔离，避免端口冲突、配置混用与相互影响：

| 隔离维度 | RAG Lab（本项目） | AI 求职 Agent（未来，非本仓库范围） |
|----------|-------------------|-------------------------------------|
| **服务** | Spring Boot RAG API | Agent 独立进程 / 独立应用 |
| **端口** | 8080（本机） | 独立端口，如 8081、3000 等 |
| **目录** | `/opt/rag-lab`、`/var/lib/rag-lab` | 独立目录，如 `/opt/agent-job` |
| **配置** | `.env.prod`、`application-prod.yml` | 独立 env 与配置文件 |
| **域名 / 路径** | `rag.example.com` 或 `/` | 子域名 `agent.example.com` 或路径前缀 `/agent` |
| **Nginx** | 独立 `server` 或 `location` 块 | 与 RAG 分开反代，不共用 jar 与静态目录 |

**本项目 V9 范围：** 仅完成 RAG 服务的 ECS 环境准备与部署说明，**不实现** AI 求职 Agent 的功能、代码或部署脚本。

---

## 6. 下一步

1. 在阿里云控制台创建 ECS、配置安全组（22 / 80 / 443）  
2. 按本文完成初始化与环境检查  
3. 按 [09-production-deployment.md](09-production-deployment.md) 部署后端 jar、前端 `dist`、Nginx  
4. 配置 RDS / ES 内网连接，填写 `.env.prod`  

---

## 相关文档

- [09-production-deployment.md](09-production-deployment.md) — 生产环境部署（打包、构建、Nginx）
- [deploy/nginx.conf.example](../deploy/nginx.conf.example) — Nginx 模板
- [.env.prod.example](../.env.prod.example) — 生产环境变量模板
