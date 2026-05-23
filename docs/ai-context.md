# AI_CONTEXT.md

**当前开发版本：V4（关键词检索版）**

## 项目背景

ai-rag-lab-platform 是一个 RAG 知识库实验平台。

这个项目用于学习和展示 RAG 系统的完整演进过程，包括：

1. 文档导入
2. 文档解析
3. 文本分块
4. Naive RAG
5. 检索过程可视化（V3）
6. BM25 关键词检索（V4，当前）
7. Hybrid Search 混合检索（V5+）
8. Reranker 重排（V6+）
9. 评测指标（V7+）
10. 工程化增强（V8+）

## 项目开发方式

项目采用版本化开发。

每个版本只做当前版本应该做的事情，不提前做后面版本的功能。

## 版本规划

### V0：项目骨架版

状态：已完成

---

### V1：文档导入与分块版

状态：已完成

V1 允许：知识库、文档、Chunk、样例数据、Dashboard

V1 禁止：Embedding、向量检索、LLM、Chat

---

### V2：Naive RAG 问答版

状态：已完成

V2 允许：Embedding、PgVector、向量检索、Chat、Prompt、引用来源

V2 禁止：BM25、Elasticsearch、Hybrid、Reranker、Debug Console、Evaluation、权限、多轮对话

---

### V2.5：真实模型接入版

状态：已完成

在 V2 基础上增加 Qwen Embedding、DeepSeek Chat，配置切换，向量一致性校验。

---

### V3：RAG Debug 可观察版

状态：已完成

目标：让用户在前端看到一次 RAG 问答的完整内部过程。

V3 允许：Debug 查询、召回/Context/Prompt/Answer 展示、Context 过滤、查询历史、耗时统计

V3 禁止：BM25、Elasticsearch、Hybrid、Reranker、Evaluation 等（已在 V4 单独实现 BM25）

---

### V4：关键词检索版（当前）

目标：引入 Elasticsearch + BM25，解决错误码、接口路径、专有名词等场景下纯向量检索不稳定的问题。

V4 允许：

- Elasticsearch（本地 docker-compose 单节点）
- BM25 关键词检索（`POST /api/search/bm25`）
- Chunk 同步 ES（`POST /api/search/index/rebuild`）
- ES 索引 `rag_document_chunk` 全量重建
- Debug 页 `searchMode`：`VECTOR` / `BM25` 切换
- Debug 流程在 BM25 模式下仍走 V3 Context 过滤、Prompt、回答生成

V4 禁止：

- Hybrid Search
- RRF 融合
- Reranker
- Query Rewrite
- Evaluation
- 权限控制
- 多轮对话
- 为 V5+ 创建空类、空接口、空页面、空表

---

### V5：混合检索版

目标：Vector + BM25、RRF（未开始）

---

### V6：Reranker 重排版

目标：粗召回后重排（未开始）

---

### V7：评测中心版

目标：测试用例、批量评测、Recall@K、MRR（未开始）

---

### V8：工程化增强版

目标：权限、多轮对话、Token 成本等（未开始）

## 当前技术栈

后端：Spring Boot 3、JDK 17、PostgreSQL、PgVector、Elasticsearch、MyBatis-Plus

前端：Vue 3、TypeScript、Vite、Element Plus

## 开发原则

1. 只做当前版本功能
2. 不提前实现后续版本
3. 保证 V1～V3 既有能力不受影响
4. 完成后更新 `docs/CURRENT_VERSION.md`
