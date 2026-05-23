# 08 - AI 协作规范

## 协作角色

| 角色 | 职责 |
|------|------|
| **Cursor（Agent）** | 代码执行者：读文档、改代码、跑测试、更新 docs |
| **ChatGPT 架构师** | 需求分析、架构设计、任务拆解、版本边界把关 |
| **ChatGPT Debug 助手** | Bug 现象分析、根因推断、修复步骤与验收建议 |

人类开发者做最终决策、合并代码与发版。

---

## 开发前：必读文档

每次开发或新开会话，**先阅读**（不要立刻写代码）：

1. `AGENTS.md`（项目根，速览）
2. `docs/00-project-guideline.md`
3. `docs/01-requirements.md`
4. `docs/05-development-plan.md`（当前版本与验收）

按需阅读：`02-architecture.md`、`03-api-design.md`、`04-database-design.md`、`06-debug-log.md`、`07-change-log.md`。

阅读后先总结：

1. 当前项目是什么
2. 当前开发到哪个版本
3. 当前版本已完成什么、允许什么、禁止什么
4. 下一步应该做什么

**总结确认之前，不要修改代码。**

---

## 开发后：必须更新的文档

| 文档 | 更新内容 |
|------|----------|
| `05-development-plan.md` | 当前版本进度、验收项、已知问题 |
| `06-debug-log.md` | 新 Bug、修复记录、测试结论摘要 |
| `07-change-log.md` | 版本完成时的接口/表/页面变更 |

其他文档（如 `03-api-design.md`）在接口或表结构变更时同步更新。

---

## 版本边界（强制）

1. **只做当前版本**范围内的功能（当前为 **V4**）。
2. **不允许**提前实现 V5 Hybrid、V6 Reranker、V7 Evaluation、V8 工程化等。
3. **不允许**生成未来版本的空类、空接口、空页面、空表、空 ES 索引。
4. **不允许**为「以后可能用到」引入无关依赖。
5. 保证 V0～当前版本−1 的既有能力不被破坏。

---

## 安全与配置

1. **不允许**提交真实 API Key 到 Git。
2. `.env` **必须**在 `.gitignore` 中。
3. `.env.example` **只能**放占位符。
4. 本地/远程 ES、PG 以 `.env` 为准，不假设一定使用 docker-compose。

---

## 会话恢复提示词（复制给 Cursor）

```text
请先恢复当前项目上下文，不要立刻写代码。

请阅读以下文件：

1. AGENTS.md
2. docs/00-project-guideline.md
3. docs/01-requirements.md
4. docs/05-development-plan.md

阅读后请先总结：

1. 当前项目是什么
2. 当前开发到哪个版本
3. 当前版本已经完成什么
4. 当前版本允许做什么
5. 当前版本禁止做什么
6. 下一步应该做什么

在你总结并确认之前，不要修改任何代码。
```

---

## 测试与脚本

- 环境探测：`python3 scripts/probe-rag-services.py`
- V3 Context 过滤：`python3 scripts/run-v3-context-filter-tests.py`
- V4 BM25 冒烟：`python3 scripts/run-v4-bm25-smoke-test.py`
- 后端单测：`cd backend && mvn test -Dtest=ContextChunkFilterTest`（示例）

脚本自动加载 `.env`（`scripts/lib/load_dotenv.py`）。

---

## 与旧文档的对应关系

原 `session-resume.md`、`ai-context.md`、`current-version.md` 内容已并入本规范及 `00`/`01`/`05`/`07` 等文档；会话恢复请以本节「必读文档」为准。
