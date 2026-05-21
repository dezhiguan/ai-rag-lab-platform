
---

# 二、docs/AI_CONTEXT.md

```markdown
# AI_CONTEXT.md

## 项目背景

ai-rag-lab-platform 是一个 RAG 知识库实验平台。

这个项目用于学习和展示 RAG 系统的完整演进过程，包括：

1. 文档导入
2. 文档解析
3. 文本分块
4. Naive RAG
5. 检索过程可视化
6. BM25 关键词检索
7. Hybrid Search 混合检索
8. Reranker 重排
9. 评测指标
10. 工程化增强

## 项目开发方式

项目采用版本化开发。

每个版本只做当前版本应该做的事情，不提前做后面版本的功能。

## 版本规划

### V0：项目骨架版

目标：

- 后端 Spring Boot 项目能启动
- 前端 Vue 项目能启动
- PostgreSQL 能启动
- 前端能调用后端 health 接口

状态：已完成

---

### V1：文档导入与分块版

目标：

- 知识库 CRUD
- Markdown / TXT 文档上传
- 样例数据初始化
- 文档解析
- 固定大小分块
- Chunk 查看页面

V1 允许：

- knowledge_base 表
- document 表
- document_chunk 表
- kb 模块
- document 模块
- sample 模块
- dashboard 统计

V1 禁止：

- Embedding
- 向量检索
- PgVector
- Elasticsearch
- BM25
- Hybrid Search
- LLM
- Chat
- Prompt
- Reranker
- Evaluation
- 权限控制
- 多轮对话

---

### V2：Naive RAG 问答版

目标：

- 接入 Embedding
- 存储 Chunk 向量
- 实现基础向量检索
- 接入 LLM
- 实现基础问答
- 返回引用来源

---

### V3：RAG Debug 可观察版

目标：

- 展示召回 Chunk
- 展示 Prompt
- 展示引用来源
- 展示耗时
- 记录问答日志

---

### V4：关键词检索版

目标：

- 接入 Elasticsearch
- 实现 BM25 检索
- 对比向量检索和关键词检索

---

### V5：混合检索版

目标：

- 实现 Vector + BM25 混合检索
- 实现 RRF 融合

---

### V6：Reranker 重排版

目标：

- 实现粗召回后重排
- 展示重排前后结果变化

---

### V7：评测中心版

目标：

- 维护测试用例
- 批量运行评测
- 统计命中率、Recall@K、MRR、耗时

---

### V8：工程化增强版

目标：

- 文档版本管理
- 增量更新
- 权限过滤
- 多轮对话问题重构
- Token 成本统计
- 用户反馈