# AGENTS.md

## 项目名称

ai-rag-lab-platform

## 项目定位

这是一个企业级 RAG 知识库实验平台。

项目目标不是一次性做一个大而全的系统，而是按版本逐步开发，让每个版本都能运行、能测试、能观察现象，最后用于学习、简历和面试展示。

## 版本路线

- V0：项目骨架版
- V1：文档导入与分块版
- V2：Naive RAG 问答版
- V3：RAG Debug 可观察版
- V4：关键词检索版
- V5：混合检索版
- V6：Reranker 重排版
- V7：评测中心版
- V8：工程化增强版

## 当前版本

V4：关键词检索版

详细允许/禁止与流水线见 `docs/current-version.md` 与 `docs/ai-context.md`。

## 当前开发原则

1. 只做当前版本要求的功能。
2. 不提前实现后续版本功能。
3. 不提前创建后续版本的空 Controller、Service、页面、表结构。
4. 不随意引入当前版本用不到的依赖。
5. 不重写已经能正常运行的代码。
6. 每次修改后，必须保证后端和前端都能正常启动。
7. 每次开发完成后，更新 `docs/current-version.md`。

## 当前技术栈

后端：

- Spring Boot 3
- JDK 17
- Maven
- PostgreSQL
- pgvector（向量存储）
- Embedding API、LLM API（具体厂商由实现阶段在配置中选定）
- MyBatis-Plus
- Lombok
- Swagger / Knife4j

前端：

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Axios

## 当前项目结构原则

当前阶段先使用简单单体结构，不使用 Maven 多模块。

后端包结构：

```text
com.guan.rag
├── common
├── config
└── module