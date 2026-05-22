# 无 Docker 本地开发说明

本项目 **不强制** 使用 `docker-compose.yml`。`docker-compose` 仅描述一种可选的本地编排方式；**实际连接以项目根目录 `.env` 为准**。

## 配置优先级

| 来源 | 用途 |
|------|------|
| **`.env`** | 本机真实环境（推荐）：PostgreSQL、远程 ES、API Key |
| `application.yml` + `application-dev.yml` | Spring 默认与 `${ES_HOSTS}` 等占位符 |
| `docker-compose.yml` | 可选：本机能装 Docker 时一键起 PG/ES |

VS Code 调试后端已配置 `envFile: ${workspaceFolder}/.env`（见 `.vscode/launch.json`），启动 **RagApplication** 时会自动注入，**无需** Docker。

## 你当前典型配置（示例）

```env
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=postgres
POSTGRES_USER=amy
POSTGRES_PASSWORD=amy

ES_HOSTS=http://你的远程主机:9200
ES_USERNAME=elastic
ES_PASSWORD=***

DASHSCOPE_API_KEY=...
DEEPSEEK_API_KEY=...
```

- **PostgreSQL**：本机已安装的服务（非 compose 容器）
- **Elasticsearch**：远程服务器（非本机 Docker）

## Agent / 脚本如何探测环境（忽略 docker-compose）

1. 读取 **`.env`**，不要假设 `localhost:9200` 来自 compose  
2. 运行探测脚本：

```bash
python3 scripts/probe-rag-services.py
```

3. 后端启动后执行 V4 冒烟：

```bash
python3 scripts/run-v4-bm25-smoke-test.py
```

4. V3 Context 过滤回归（需后端已启动）：

```bash
python3 scripts/run-v3-context-filter-tests.py
```

脚本会通过 `scripts/lib/load_dotenv.py` 自动加载 `.env`。

## 启动顺序（无 Docker）

```bash
# 1. 确保本机 PostgreSQL 已运行，且 .env 中账号可连
# 2. 确保 .env 中 ES_HOSTS 指向可用的 Elasticsearch

# 3. 后端（VS Code: RagApplication，或命令行）
cd backend
export $(grep -v '^#' ../.env | xargs)   # bash 手动加载 .env
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 首次 BM25
curl -X POST http://localhost:8080/api/search/index/rebuild

# 5. 前端
cd frontend && npm run dev
```

## 与 docker-compose 的关系

| 服务 | 无 Docker 时 |
|------|----------------|
| PostgreSQL | 使用本机 PG，`.env` 指向 `localhost:5432` |
| Elasticsearch | 使用远程 ES，`.env` 中 `ES_HOSTS=http://ip:9200` |
| 后端 / 前端 | 与是否 Docker 无关，读 `.env` 即可 |

**结论**：测试与开发应 **以 `.env` + 已启动的后端** 为准；只有在你本机真的能跑 Docker 时，才需要 `docker compose up -d`。
