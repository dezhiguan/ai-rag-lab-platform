# 07 - 变更记录

按版本记录核心变更。完成时间可在发版或验收时补全。

---

## V0：项目骨架版

| 项 | 内容 |
|----|------|
| **完成时间** | （待填） |
| **核心变更** | Spring Boot + Vue 3 骨架；统一响应与异常；健康检查；基础布局与 Dashboard |
| **新增接口** | `GET /api/system/health`、`GET /api/dashboard/stats` |
| **新增页面** | `/dashboard` |
| **新增表/索引** | 无业务表 |
| **备注** | PostgreSQL 数据源配置 |

---

## V1：文档导入与分块版

| 项 | 内容 |
|----|------|
| **完成时间** | （待填） |
| **核心变更** | 知识库 CRUD；Markdown/TXT 上传解析；固定大小分块；样例数据初始化 |
| **新增接口** | `/api/kb/*`、`/api/kb/{kbId}/documents/*`、`/api/documents/*`、`/api/sample/*` |
| **新增页面** | `/kb`、`/kb/:kbId/documents`、`/documents/:documentId/chunks` |
| **新增表** | `knowledge_base`、`document`、`document_chunk` |
| **备注** | 不含 Embedding、Chat |

---

## V2：Naive RAG 问答版

| 项 | 内容 |
|----|------|
| **完成时间** | （待填） |
| **核心变更** | PgVector 向量化；向量 TopK；Chat 问答；Prompt 与引用来源；会话消息 |
| **新增接口** | `/api/kb/{kbId}/embedding/*`、`POST /api/chat`、`GET /api/chat/sessions/*` |
| **新增页面** | `/chat` |
| **新增表** | `chunk_embedding`、`chat_session`、`chat_message` |
| **备注** | Mock Embedding / Mock Chat |

---

## V2.5：真实模型接入版

| 项 | 内容 |
|----|------|
| **完成时间** | （待填） |
| **核心变更** | Qwen Embedding、DeepSeek Chat；Provider 路由与配置切换；向量一致性校验 |
| **新增接口** | `GET /api/model/providers` |
| **新增页面** | （无独立页，配置驱动） |
| **新增表** | 无 |
| **备注** | 切换 Embedding 后需 `embedding/rebuild` |

---

## V3：RAG Debug 可观察版

| 项 | 内容 |
|----|------|
| **完成时间** | （待填） |
| **核心变更** | Debug 全链路展示；Context 过滤；查询历史；耗时统计；`usedInPrompt` / `filterReason` |
| **新增接口** | `POST /api/debug/query`、`GET /api/debug/query-logs`、`GET /api/debug/query-logs/{id}` |
| **新增页面** | `/debug`、`/debug/:queryLogId` |
| **新增表** | `rag_query_log`、`rag_retrieval_log`（含过滤字段） |
| **备注** | Context 过滤 10 用例自动化通过（见 `06-debug-log.md`） |

---

## V4：关键词检索版

| 项 | 内容 |
|----|------|
| **状态** | ✅ 已完成 |
| **完成时间** | （待填） |
| **核心变更** | Elasticsearch 集成；索引 `rag_document_chunk`；`terms` 专有词字段；BM25 加权查询；Debug `searchMode` 支持 VECTOR / BM25；BM25「SMS_429 是什么意思？」Top1 排序问题已修复 |
| **新增接口** | `POST /api/search/index/rebuild`、`POST /api/search/bm25`；Debug 请求/响应增 `searchMode` |
| **新增页面** | Debug 页增强（模式切换、重建索引、快捷问题） |
| **新增表/索引** | **无** 新 PG 表；**ES 索引** `rag_document_chunk`；`rag_query_log.search_mode` |
| **备注** | `/api/chat` 仍为纯向量检索；V4 验收通过后项目进入 V5 |

---

## V5：Hybrid Search 混合检索版

| 项 | 内容 |
|----|------|
| **状态** | 🔄 当前 / 启动中 |
| **完成时间** | （未开始） |
| **规划核心变更** | Vector + BM25 应用层融合（如 RRF）；`HybridSearchService` 编排；Debug `searchMode=HYBRID`；融合结果不落库 |
| **规划接口** | Debug `HYBRID`（及可选独立 Hybrid API，待 V5-03/04 定稿） |
| **规划页面** | Debug 页 HYBRID 模式（V5-05） |
| **新增表/索引** | **无**（复用 V4 ES 与既有 PG 表） |
| **备注** | V5-01 文档切换；V5-02 见下 |

### V5-02：融合排序核心逻辑（2026-05）

| 项 | 内容 |
|----|------|
| **变更** | 新增 `module/search/hybrid/`：`HybridSearchMerger`（RRF）、`HybridSearchMergeItem`、`HybridSearchResult` |
| **逻辑** | 按 chunkId 合并 Vector/BM25；BM25 分数按 Top1 归一化到 [0,1] 写入 `bm25Score`；RRF 计算 `hybridScore` 并 topK 截断 |
| **测试** | `HybridSearchMergerTest`：合并、归一化、排序、截断、空输入；`mvn test -Dtest=HybridSearchMergerTest` |
| **未做** | 前端、Controller、HYBRID searchMode、DB、ES mapping |
| **原因** | 验证 V5 Hybrid 融合排序核心逻辑，为 V5-03 双路召回编排做准备 |

### V5-03：Debug HYBRID 闭环

| 项 | 内容 |
|----|------|
| **变更** | `HybridSearchService`；`SearchMode.HYBRID`；`DebugService` HYBRID 分支；`DebugView` / `DebugDetailView` 增加 Hybrid 选项 |
| **流程** | Vector + BM25 → RRF 融合 → Context 过滤 → Prompt → Answer |
| **页面验证** | `/debug` 选择 HYBRID，问题如「SMS_429 是什么意思？」，展示 retrievedChunks / context / prompt / answer |

---

## 后续版本（占位，未实现）

| 版本 | 计划核心变更 |
|------|----------------|
| V6 | Reranker |
| V7 | Evaluation 评测中心 |
| V8 | 权限、多租户、工程化 |

**禁止**在未达对应版本前写入「已完成」或提前建表/接口。
