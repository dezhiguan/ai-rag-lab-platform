# 数据与检索层部署（ECS）

在 **ECS 4C8G（数据检索层）** 上使用 Docker Compose 统一启动：

- PostgreSQL 16 + **PgVector**
- Elasticsearch 8.11
- Redis 7

应用入口层（轻量 2C4G）通过 **内网** `<ECS_PRIVATE_IP>` 访问，**不要**将 5432 / 9200 / 6379 对公网开放。

---

## 前置条件

| 项 | 说明 |
|----|------|
| 主机 | ECS 4C8G，Ubuntu 22.04 / Alibaba Cloud Linux 3 |
| Docker | Docker Engine + Compose v2 |
| 内核 | Elasticsearch 需 `vm.max_map_count >= 262144`（见下方） |
| 安全组 | 仅允许 `<LIGHT_SERVER_PRIVATE_IP>` 访问 5432、9200、6379 |

### 设置 vm.max_map_count（首次）

```bash
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

---

## 1. 配置环境变量

```bash
cd deploy/data-layer
cp .env.data.example .env.data
# 编辑 .env.data：至少修改 POSTGRES_PASSWORD
```

`.env.data` 已加入 `.gitignore`，勿提交仓库。

---

## 2. 启动数据层服务

```bash
cd deploy/data-layer
docker compose -f docker-compose.data.yml --env-file .env.data up -d
```

首次启动 Elasticsearch 可能需要 1～2 分钟，请耐心等待健康检查通过。

---

## 3. 查看服务状态

```bash
docker compose -f docker-compose.data.yml --env-file .env.data ps
```

期望三个服务均为 `running (healthy)` 或 `running`。

---

## 4. 查看日志

```bash
# 全部服务
docker compose -f docker-compose.data.yml --env-file .env.data logs -f

# 单个服务
docker compose -f docker-compose.data.yml --env-file .env.data logs -f postgres
docker compose -f docker-compose.data.yml --env-file .env.data logs -f elasticsearch
docker compose -f docker-compose.data.yml --env-file .env.data logs -f redis
```

---

## 5. 停止服务

```bash
docker compose -f docker-compose.data.yml --env-file .env.data down
```

保留数据卷（`postgres_data` / `es_data` / `redis_data`）：

```bash
docker compose -f docker-compose.data.yml --env-file .env.data down
# 默认不删除 volume
```

**危险：** 删除数据卷将清空数据：

```bash
docker compose -f docker-compose.data.yml --env-file .env.data down -v
```

---

## 6. 健康检查脚本

在 **ECS 本机** 或 **轻量服务器**（内网可达时）执行：

```bash
# ECS 本机
./scripts/check-data-layer.sh

# 轻量服务器：传入 ECS 内网 IP（勿写入公开文档提交）
./scripts/check-data-layer.sh <ECS_PRIVATE_IP>
```

---

## 7. 从应用入口层访问（内网）

在轻量服务器上验证（将占位符替换为实际内网 IP，仅写在本地）：

```bash
export ECS_HOST=<ECS_PRIVATE_IP>

# PostgreSQL
psql -h "$ECS_HOST" -U rag_user -d rag_lab -c "SELECT 1"

# Elasticsearch
curl -s "http://${ECS_HOST}:9200"

# Redis
redis-cli -h "$ECS_HOST" ping
```

应用层 `.env.prod` 中对应配置：

```bash
POSTGRES_HOST=<ECS_PRIVATE_IP>
ES_HOSTS=http://<ECS_PRIVATE_IP>:9200
REDIS_HOST=<ECS_PRIVATE_IP>
```

---

## 8. 初始化 PgVector 与数据库 Schema

首次部署后，在能访问 PostgreSQL 的机器上执行项目 `schema.sql`（通常在轻量服务器，已配置 `.env.prod`）：

```bash
set -a && source .env.prod && set +a
psql -h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -f backend/src/main/resources/db/schema.sql
```

`schema.sql` 内应包含 `CREATE EXTENSION IF NOT EXISTS vector;`（若尚未创建）。

---

## 9. Elasticsearch 内存说明（4C8G ECS）

| 组件 | 建议 |
|------|------|
| ES JVM | `.env.data` 中 `ES_JAVA_OPTS=-Xms1g -Xmx2g`（默认） |
| 调优 | 查询量大时可调至 `-Xmx2g` 上限；勿超过 ECS 可用内存一半 |
| 监控 | `docker stats rag-data-elasticsearch` 观察实际占用 |

PostgreSQL 与 Redis 与 ES 共享 8GiB 内存，需为操作系统与其它进程预留空间。

---

## 10. 数据持久化

Compose 使用命名卷：

| 卷名 | 服务 |
|------|------|
| `postgres_data` | PostgreSQL |
| `es_data` | Elasticsearch |
| `redis_data` | Redis |

查看卷：

```bash
docker volume ls | grep rag-data
```

---

## 相关文档

- [docs/10-aliyun-ecs-setup.md](../../docs/10-aliyun-ecs-setup.md) — 双服务器环境准备  
- [docs/09-production-deployment.md](../../docs/09-production-deployment.md) — 应用层部署  
- [.env.prod.example](../../.env.prod.example) — 应用层连接数据层  
