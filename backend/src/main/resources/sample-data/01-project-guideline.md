# 项目背景

AI RAG Lab Platform 是企业级 RAG 知识库实验平台，用于逐步验证文档导入、分块、检索与问答等能力。

## 技术栈

- 后端：Spring Boot 3、JDK 17、Maven、PostgreSQL、MyBatis-Plus
- 前端：Vue 3、TypeScript、Vite、Element Plus
- 基础设施：Docker Compose 启动 PostgreSQL

## 为什么选择 PostgreSQL

PostgreSQL 是成熟的关系型数据库，适合存储知识库、文档与 Chunk 元数据，并具备良好的事务与扩展能力。

## 为什么后续会使用 PgVector

在后续版本中，向量 Embedding 将存储在 PostgreSQL 的 PgVector 扩展中，以便在同一数据库内完成元数据与向量检索，简化运维。

## 接口返回规范

统一使用 `ApiResponse` 包装：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

业务错误返回非 200 的 code 与明确 message。

## 日志规范

- 使用 SLF4J 记录关键业务节点
- 上传、解析、分块需记录 documentId 与耗时
- 错误日志需包含 requestId（如有）与堆栈摘要
