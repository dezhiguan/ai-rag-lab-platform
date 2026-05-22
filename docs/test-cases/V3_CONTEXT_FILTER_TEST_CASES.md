# V3 Context 过滤测试用例

> **适用范围**：V3 RAG Debug 轻量级 Context 组装过滤（`ContextChunkFilter`）  
> **不在范围**：BM25、Elasticsearch、Hybrid Search、Reranker、Query Rewrite、Evaluation、权限、多轮对话  
> **关联实现**：`ContextChunkFilter`、`DebugService`、`POST /api/debug/query`、Debug 前端页

---

## 1. 测试目标

验证 V3 Debug 流程在**不改变完整召回展示**的前提下，正确区分「展示用召回」与「进入 Prompt 的 Context」：

| 目标 | 说明 |
|------|------|
| 双轨数据 | `retrievedChunks` 保留 TopK 全量；`contextChunks` 仅为过滤后进入 Prompt 的片段 |
| 过滤规则 | 按 score 降序 → `minScore` → `maxScoreGap` → `maxChunks`；全部被过滤时保底 Top1 |
| 可观察性 | 每条召回含 `usedInPrompt`、`filterReason`；Debug 页与 API 一致 |
| Prompt 隔离 | `context`、`prompt` 仅拼接 `contextChunks`，不含被过滤 Chunk 正文 |
| 拒答行为 | 无相关知识时，模型应倾向回答「知识库中没有找到相关依据。」（受 Context 与模型影响，作观察项） |

---

## 2. 当前过滤配置

来源：`backend/src/main/resources/application.yml`

```yaml
rag:
  context:
    max-chunks: 2      # 最多 2 个 Chunk 进入 Prompt
    min-score: 0.45    # 低于该分数不进入（保底 Top1 除外）
    max-score-gap: 0.35 # 与 Top1 分差超过该值不进入
```

**过滤原因枚举**（API / DB）：

| filterReason | 中文标签（前端） | 含义 |
|--------------|------------------|------|
| `SCORE_TOO_LOW` | 分数过低 | `score < minScore` |
| `SCORE_GAP_TOO_LARGE` | 与 Top1 差距过大 | `top1Score - score > maxScoreGap`（且 score ≥ minScore） |
| `EXCEED_MAX_CONTEXT_CHUNKS` | 超出 Context 上限 | 通过 minScore / gap 但仍超过 `maxChunks` 排位 |

**判定优先级**（`resolveFilterReason`）：`SCORE_TOO_LOW` → `SCORE_GAP_TOO_LARGE` → `EXCEED_MAX_CONTEXT_CHUNKS`

---

## 3. 前置条件

| 项 | 要求 |
|----|------|
| 环境 | 后端 `dev` profile 已启动；PostgreSQL + PgVector 正常 |
| 样例库 | 已执行样例初始化（`kbId=1`，含 `01-project-guideline.md`、`02-api-spec.md`、`03-troubleshooting.md`） |
| Embedding | 与验收时一致（建议 **mock**，与下表实测分数一致；切换 qwen 后分数可能偏移，需重测） |
| 向量 | 样例文档已向量化（`POST /api/kb/1/embedding/rebuild` 若检索为空） |
| 入口 | Debug 页 `http://localhost:5173/debug` 或 `POST /api/debug/query` |
| 参数 | 默认 `topK=5`（样例库仅 3 个 Chunk 时，召回条数 ≤ 3） |

---

## 4. 测试用例列表

| 编号 | 测试问题 | 主要验证点 |
|------|----------|------------|
| TC-V3-CF-01 | 短信验证码发不出去怎么排查？ | 强相关 + 补充相关 + 噪声 `SCORE_TOO_LOW` |
| TC-V3-CF-02 | SMS_429 是什么意思？ | 错误码 / api-spec 相关 |
| TC-V3-CF-03 | send-code 接口路径是什么？ | 接口路径 / api-spec 相关 |
| TC-V3-CF-04 | 公司年终奖发几个月？ | 无答案 + 低分保底 Top1 + 拒答观察 |
| TC-V3-CF-05 | 短信验证码接口有哪些限制？ | `maxChunks=2` 上限 |
| TC-V3-CF-06 | 为什么后续会使用 PgVector？ | 弱相关抑制（含 `maxScoreGap` 规则） |
| TC-V3-CF-07 | Redis 缓存异常怎么排查？ | `minScore` 过滤 |
| TC-V3-CF-08 | 公司股票期权归属规则是什么？ | 全部分数低于 minScore 时保底 Top1 |
| TC-V3-CF-09 | （任意问题，建议 TC-V3-CF-01） | Prompt 仅含 `contextChunks` |
| TC-V3-CF-10 | （任意问题，建议 TC-V3-CF-01） | Debug 页全字段展示 |

---

## 5. 测试用例明细

> **分数说明**：下列「实测分数」在 **mock Embedding + 样例库 kbId=1** 下于本地 Debug API 验证（2026-05）。若更换 Embedding Provider，分数与排序可能变化，但**过滤逻辑与字段结构不变**；需以实际返回为准核对 `usedInPrompt` / `filterReason`。

---

### TC-V3-CF-01 短信验证码发不出去怎么排查？

**测试目标**：验证强相关片段进入 Prompt、补充相关可保留、弱相关噪声被 `minScore` 过滤。

**测试问题**：`短信验证码发不出去怎么排查？`

**预期 retrievedChunks**（3 条，按 score 降序）：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 03-troubleshooting.md | ≈ 0.8230 |
| 2 | 02-api-spec.md | ≈ 0.6193 |
| 3 | 01-project-guideline.md | ≈ 0.2701 |

**预期 contextChunks**（2 条）：

| 序号 | documentName |
|------|----------------|
| 1 | 03-troubleshooting.md |
| 2 | 02-api-spec.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 03-troubleshooting.md | `true` |
| 02-api-spec.md | `true` |
| 01-project-guideline.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 03-troubleshooting.md | `null` |
| 02-api-spec.md | `null` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- 「知识库上下文」含 troubleshooting 排查步骤（手机号、Redis 频率、服务商等）
- 可含 api-spec 中 SMS 相关错误码表
- **不得**含 guideline 中「项目背景 / PgVector 规划」等无关段落
- 回答应能列出排查步骤，且与上述两文档相关

**页面验证点**：

- 召回表格 3 行；Top2 绿色「已进入」，guideline 灰色「未进入」+ 标签「分数过低」
- Context 区标题显示「进入 Prompt **2** 个片段」
- Prompt 预览与 Context 正文一致，均为 2 段拼接（`【片段1】`…`【片段2】`）

---

### TC-V3-CF-02 SMS_429 是什么意思？

**测试目标**：验证错误码问题优先命中 api-spec，且弱相关 guideline 被过滤。

**测试问题**：`SMS_429 是什么意思？`

**预期 retrievedChunks**：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 02-api-spec.md | ≈ 0.5567 |
| 2 | 03-troubleshooting.md | ≈ 0.5286 |
| 3 | 01-project-guideline.md | ≈ 0.2873 |

**预期 contextChunks**：

| 序号 | documentName |
|------|----------------|
| 1 | 02-api-spec.md |
| 2 | 03-troubleshooting.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 02-api-spec.md | `true` |
| 03-troubleshooting.md | `true` |
| 01-project-guideline.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 02-api-spec.md | `null` |
| 03-troubleshooting.md | `null` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- Context 含错误码表：`SMS_429` →「发送过于频繁」
- 回答应明确解释 SMS_429 含义，可引用 troubleshooting 中 Redis/频率排查语境
- 不含 guideline 技术栈无关内容

**页面验证点**：

- Top1 为 `02-api-spec.md`
- 两条已进入 Prompt，guideline 显示「分数过低」

---

### TC-V3-CF-03 send-code 接口路径是什么？

**测试目标**：验证接口路径问题召回 api-spec，并正确组装 Context。

**测试问题**：`send-code 接口路径是什么？`

**预期 retrievedChunks**：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 02-api-spec.md | ≈ 0.7277 |
| 2 | 03-troubleshooting.md | ≈ 0.6500 |
| 3 | 01-project-guideline.md | ≈ 0.3734 |

**预期 contextChunks**：

| 序号 | documentName |
|------|----------------|
| 1 | 02-api-spec.md |
| 2 | 03-troubleshooting.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 02-api-spec.md | `true` |
| 03-troubleshooting.md | `true` |
| 01-project-guideline.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 02-api-spec.md | `null` |
| 03-troubleshooting.md | `null` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- Context 含 `POST /api/sms/send-code`
- 回答路径为 `/api/sms/send-code`（或等价表述）
- guideline 未进入 Prompt

**页面验证点**：

- 召回 Top1 为 api-spec；已进入 Prompt 为 2 条

---

### TC-V3-CF-04 公司年终奖发几个月？

**测试目标**：验证知识库无相关内容时，低分召回经「保底 Top1」仍进入唯一 Context，并观察拒答文案。

**测试问题**：`公司年终奖发几个月？`

**预期 retrievedChunks**（3 条均为弱相关，score 均 < minScore）：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 03-troubleshooting.md | ≈ 0.2485 |
| 2 | 02-api-spec.md | ≈ 0.2242 |
| 3 | 01-project-guideline.md | ≈ 0.1796 |

**预期 contextChunks**（保底 Top1，仅 1 条）：

| 序号 | documentName |
|------|----------------|
| 1 | 03-troubleshooting.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 03-troubleshooting.md | `true` |
| 02-api-spec.md | `false` |
| 01-project-guideline.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 03-troubleshooting.md | `null` |
| 02-api-spec.md | `SCORE_TOO_LOW` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- Context 仅含 troubleshooting 文档（与年终奖无关）
- 回答**倾向**为：「知识库中没有找到相关依据。」（Prompt 第 4 条要求；若 mock Chat 偶发发挥，以是否胡编年终奖为准）
- **不得**编造「发 N 个月」等知识库不存在的事实

**页面验证点**：

- 召回 3 条均未达 minScore，但 Top1 仍绿色「已进入」（保底）
- 其余灰色 +「分数过低」
- Context 仅 1 个片段；Answer 区观察是否拒答

---

### TC-V3-CF-05 短信验证码接口有哪些限制？

**测试目标**：验证 `maxChunks=2`——即使多条召回分数合格，进入 Prompt 的 Chunk 不超过 2 个。

**测试问题**：`短信验证码接口有哪些限制？`

**预期 retrievedChunks**：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 03-troubleshooting.md | ≈ 0.7763 |
| 2 | 02-api-spec.md | ≈ 0.7087 |
| 3 | 01-project-guideline.md | ≈ 0.3129 |

**预期 contextChunks**（≤ 2 条）：

| 序号 | documentName |
|------|----------------|
| 1 | 03-troubleshooting.md |
| 2 | 02-api-spec.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 03-troubleshooting.md | `true` |
| 02-api-spec.md | `true` |
| 01-project-guideline.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 03-troubleshooting.md | `null` |
| 02-api-spec.md | `null` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- `contextChunks.length === 2`（严格不超过 `max-chunks`）
- 回答可综合频率限制、错误码、手机号格式等（来自上述两文档）

**页面验证点**：

- 标题「进入 Prompt **2** 个片段」
- 表格仅 2 行绿色「已进入」

**补充：EXCEED_MAX_CONTEXT_CHUNKS 规则推演**（样例库仅 3 Chunk 时难以在集成环境触发，可用单元/手工推演）：

若存在 3 条召回且均满足 `score ≥ 0.45` 且 `top1Score - score ≤ 0.35`，例如 score = `0.80 / 0.60 / 0.50`，则：

| documentName | usedInPrompt | filterReason |
|--------------|--------------|--------------|
| Top1 | `true` | `null` |
| Top2 | `true` | `null` |
| Top3 | `false` | `EXCEED_MAX_CONTEXT_CHUNKS` |

---

### TC-V3-CF-06 为什么后续会使用 PgVector？

**测试目标**：验证与 Top1 差距过大的弱相关文档不进入 Prompt（`maxScoreGap` 规则；样例库下常表现为 `SCORE_TOO_LOW`）。

**测试问题**：`为什么后续会使用 PgVector？`

**预期 retrievedChunks**：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 01-project-guideline.md | ≈ 0.6610 |
| 2 | 02-api-spec.md | ≈ 0.3184 |
| 3 | 03-troubleshooting.md | ≈ 0.2786 |

**预期 contextChunks**：

| 序号 | documentName |
|------|----------------|
| 1 | 01-project-guideline.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 01-project-guideline.md | `true` |
| 02-api-spec.md | `false` |
| 03-troubleshooting.md | `false` |

**预期 filterReason**（实测）：

| documentName | filterReason | 说明 |
|--------------|--------------|------|
| 01-project-guideline.md | `null` | Top1，含 PgVector 选型说明 |
| 02-api-spec.md | `SCORE_TOO_LOW` | score < 0.45 |
| 03-troubleshooting.md | `SCORE_TOO_LOW` | score < 0.45 |

> **maxScoreGap 专项推演**（当次 Top1 与其它片段**均 ≥ minScore** 但分差 > 0.35 时）：  
> 例如 Top1 score = `0.82`，某片段 score = `0.46`（分差 `0.36` > `0.35`）→ `filterReason = SCORE_GAP_TOO_LARGE`，`usedInPrompt = false`。  
> 样例库当前 3 文档分数组合下，次要文档多因低于 `minScore` 先被标为 `SCORE_TOO_LOW`，与 gap 规则共同抑制弱相关。

**Prompt 验证点**：

- Context 仅含 guideline 中「为什么后续会使用 PgVector」段落
- 回答应围绕「同一数据库内存储向量、简化运维」等，不引入短信 API 内容

**页面验证点**：

- 仅 guideline 绿色「已进入」
- 另两条灰色，标签为「分数过低」（若未来分数升高且仅差 gap 超标，应显示「与 Top1 差距过大」）

---

### TC-V3-CF-07 Redis 缓存异常怎么排查？

**测试目标**：验证 `minScore` 过滤——仅分数达标的 troubleshooting 片段进入 Prompt。

**测试问题**：`Redis 缓存异常怎么排查？`

**预期 retrievedChunks**：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 03-troubleshooting.md | ≈ 0.5566 |
| 2 | 01-project-guideline.md | ≈ 0.3750 |
| 3 | 02-api-spec.md | ≈ 0.3530 |

**预期 contextChunks**：

| 序号 | documentName |
|------|----------------|
| 1 | 03-troubleshooting.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 03-troubleshooting.md | `true` |
| 01-project-guideline.md | `false` |
| 02-api-spec.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 03-troubleshooting.md | `null` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |
| 02-api-spec.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- Context 含 troubleshooting「检查 Redis 缓存」步骤（连接、key 一致性）
- 不含 api-spec 错误码表全文、不含 guideline 技术栈

**页面验证点**：

- 仅 1 条绿色「已进入」
- 另两条「分数过低」

---

### TC-V3-CF-08 公司股票期权归属规则是什么？

**测试目标**：验证当**所有**召回 score 均 < `minScore` 时，仍保底 Top1 进入 Prompt，避免 Context 为空。

**测试问题**：`公司股票期权归属规则是什么？`

**预期 retrievedChunks**：

| 序号 | documentName | score（实测） |
|------|----------------|---------------|
| 1 | 02-api-spec.md | ≈ 0.2522 |
| 2 | 03-troubleshooting.md | ≈ 0.2365 |
| 3 | 01-project-guideline.md | ≈ 0.2187 |

**预期 contextChunks**（保底 Top1）：

| 序号 | documentName |
|------|----------------|
| 1 | 02-api-spec.md |

**预期 usedInPrompt**：

| documentName | usedInPrompt |
|--------------|--------------|
| 02-api-spec.md | `true` |
| 03-troubleshooting.md | `false` |
| 01-project-guideline.md | `false` |

**预期 filterReason**：

| documentName | filterReason |
|--------------|--------------|
| 02-api-spec.md | `null` |
| 03-troubleshooting.md | `SCORE_TOO_LOW` |
| 01-project-guideline.md | `SCORE_TOO_LOW` |

**Prompt 验证点**：

- Context 仅含 api-spec（与期权无关）
- 回答**倾向**拒答：「知识库中没有找到相关依据。」
- Top1 虽进入 Prompt，但内容不足以支撑期权规则，不应编造归属规则

**页面验证点**：

- 三条召回分数均偏低；仅 Top1（api-spec）绿色「已进入」
- 与 TC-V3-CF-04 类似，观察保底 Top1 + 拒答

**自动化参考**：`ContextChunkFilterTest#keepsTop1WhenAllFiltered`

---

### TC-V3-CF-09 Prompt 仅包含 contextChunks（横切）

**测试目标**：任意 Debug 查询下，被过滤 Chunk 的正文不得出现在 `prompt` / `context` 字段中。

**测试问题**：建议使用 **TC-V3-CF-01** 同一问题：`短信验证码发不出去怎么排查？`

**预期 retrievedChunks**：同 TC-V3-CF-01（含未进入的 guideline）

**预期 contextChunks**：同 TC-V3-CF-01（不含 guideline）

**预期 usedInPrompt / filterReason**：同 TC-V3-CF-01

**Prompt 验证点**：

| 检查项 | 预期 |
|--------|------|
| `response.context` | 仅包含 `03-troubleshooting.md`、`02-api-spec.md` 的 `content` |
| `response.prompt` 中「知识库上下文」段落 | 与 `context` 一致，**不包含**「AI RAG Lab Platform 是企业级」等 guideline 首段特征文案 |
| `response.prompt` | 含固定模板开头「你是一个企业知识库问答助手」及用户问题原文 |
| 被过滤 Chunk | `usedInPrompt=false` 的文档名/正文均不出现在 prompt 字符串中 |

**页面验证点**：

- 「注入 Prompt 的 Context」折叠区正文 ⊂ 已进入 Prompt 的 Chunk 卡片内容
- 「Prompt 预览」与 API 返回 `prompt` 一致
- 未进入行（灰色）的 Chunk 正文仅出现在召回表格，不出现在 Context/Prompt 区

**建议 API 断言**（伪代码）：

```text
∀ c ∈ retrievedChunks where c.usedInPrompt = false:
  c.content 不是 response.prompt 的子串（或规范化后不相等）
```

---

### TC-V3-CF-10 Debug 页面全字段展示（横切）

**测试目标**：验证 Debug 页与 `/api/debug/query` 返回结构完整、字段含义正确。

**测试问题**：建议使用 **TC-V3-CF-01** 同一问题。

**预期 retrievedChunks / contextChunks / usedInPrompt / filterReason**：同 TC-V3-CF-01

**Prompt 验证点**：同 TC-V3-CF-09

**页面验证点**：

| 区域 | 预期 |
|------|------|
| 召回 Chunk 表格 | 展示全部 `retrievedChunks`；列含文档名、score、**是否进入 Prompt**、**过滤原因** |
| 标签样式 | 已进入 = 绿色 `success`；未进入 = 灰色 + 中文原因（分数过低 / 与 Top1 差距过大 / 超出 Context 上限） |
| Context 区 | 仅 `contextChunks` 拼接；标题含片段数量 |
| Prompt 预览 | 完整 Prompt 文本 |
| Answer | 模型回答非空（mock/real 均可） |
| Latency | 展示 `retrievalTimeMs`、`generationTimeMs`、`totalTimeMs` |
| 历史详情 `/debug/:queryLogId` | 与当次查询字段一致（旧数据若无 `filter_reason` 可能仅部分展示） |

**API 字段清单**：

```json
{
  "retrievedChunks": [{ "documentName", "score", "usedInPrompt", "filterReason", "content", ... }],
  "contextChunks": [{ ... }],
  "context": "...",
  "prompt": "...",
  "answer": "...",
  "latency": { "retrievalTimeMs", "generationTimeMs", "totalTimeMs" }
}
```

---

## 6. 执行方式

### 6.1 页面手工测试

1. 打开 `http://localhost:5173/debug`
2. 选择知识库 `kbId=1`
3. 依次输入上表「测试问题」，点击查询
4. 对照本节「预期」检查表格、Context、Prompt、Answer、Latency

### 6.2 HTTP 文件

`http/debug-test.http` — 修改 `question` 字段执行 `POST /api/debug/query`

### 6.3 单元测试（过滤逻辑）

```bash
cd backend && mvn test -Dtest=ContextChunkFilterTest
```

覆盖：TC-V3-CF-01 分数组合、TC-V3-CF-08 保底 Top1。

---

## 7. 通过标准

| 级别 | 标准 |
|------|------|
| P0 | TC-V3-CF-01～08 的 `usedInPrompt`、`filterReason`、`contextChunks` 条数与文档一致 |
| P0 | TC-V3-CF-09：`prompt`/`context` 不含 `usedInPrompt=false` 的 Chunk 正文 |
| P0 | TC-V3-CF-10：Debug 页各区域字段齐全且与 API 一致 |
| P1 | TC-V3-CF-04、08：Answer 拒答或不说胡话（观察项） |
| P1 | TC-V3-CF-05、06 补充推演：理解 `EXCEED_MAX_CONTEXT_CHUNKS` / `SCORE_GAP_TOO_LARGE` 触发条件 |

---

## 8. 版本边界（再次强调）

本测试文档**仅**用于 V3 Context 过滤与 Debug 可观察性，**不**作为 V4 关键词检索、Hybrid、Reranker、Evaluation 的测试依据。  
`/api/chat` 问答页仍使用 `ChatRelevanceFilter`，不在本用例范围内。
