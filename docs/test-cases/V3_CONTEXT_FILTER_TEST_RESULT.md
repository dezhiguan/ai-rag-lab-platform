# V3 Context 过滤测试执行结果

> 由 `scripts/run-v3-context-filter-tests.py` 自动生成，数据来自真实 `POST /api/debug/query` 调用。

## 执行摘要

| 项 | 值 |
|----|-----|
| 执行时间（UTC） | 2026-05-22 14:04:55 UTC |
| API 地址 | `http://localhost:8080` |
| kbId | 1 |
| topK | 5 |
| 用例总数 | 10 |
| 通过 | 10 |
| 失败 | 0 |
| 总体结论 | **通过** |

## 通过标准（脚本自动判定）

1. 接口调用成功（HTTP + `code=200`）
2. `retrievedChunks` 不为空
3. `contextChunks` 不为空
4. Prompt 不包含 `usedInPrompt=false` 的 Chunk 正文
5. `usedInPrompt` / `filterReason` 字段符合约定
6. `latency` 字段完整

---

## V3_TC_001 — ✅ 通过

**测试问题**：短信验证码发不出去怎么排查？

### 请求参数

```json
{
  "kbId": 1,
  "question": "短信验证码发不出去怎么排查？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`33`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：9490 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 03-troubleshooting.md | 0.8230 | True | — |
| 02-api-spec.md | 0.6193 | True | — |
| 01-project-guideline.md | 0.2701 | False | SCORE_TOO_LOW |


### contextChunks

1. `03-troubleshooting.md` (chunkId=23, score=0.823002671776782)
2. `02-api-spec.md` (chunkId=22, score=0.6193453484970192)


### Prompt 检查

- Prompt 长度：936 字符
- contextChunks 数量：2
- 被过滤 Chunk 数量：1
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：03-troubleshooting.md，Chunk #0
# 短信验证码发送失败排查流程

当用户反馈「收不到验证码」时，按以下步骤排查：

## 1. 检查手机号格式

确认手机号符合国内 11 位规则，排除空格与区号错误。

## 2. 检查频率限制

查询 Redis 中该手机号的发送次数与冷却时间，确认未触发 SMS_429。

## 3. 检查 Redis 缓存

确认 Redis 连接正常，验证码写入与…
```
</details>

### Answer 摘要

> 根据知识库，短信验证码发送失败的排查步骤如下：  1. **检查手机号格式**：确认手机号符合国内11位规则，排除空格与区号错误。 2. **检查频率限制**：查看该手机号在Redis中的发送次数与冷却时间，确认是否触发了SMS_429错误（发送过于频繁）。 3. **检查Redis缓存**：确认Redis连接正常，验证码写入与读取的key是否一致。 4. **检查短信服务商返回码**：查看服务商…

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 3009 | 6395 | 9413 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_002 — ✅ 通过

**测试问题**：SMS_429 是什么意思？

### 请求参数

```json
{
  "kbId": 1,
  "question": "SMS_429 是什么意思？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`34`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：4297 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 02-api-spec.md | 0.5567 | True | — |
| 03-troubleshooting.md | 0.5286 | True | — |
| 01-project-guideline.md | 0.2873 | False | SCORE_TOO_LOW |


### contextChunks

1. `02-api-spec.md` (chunkId=22, score=0.556659291089395)
2. `03-troubleshooting.md` (chunkId=23, score=0.5286480613660597)


### Prompt 检查

- Prompt 长度：936 字符
- contextChunks 数量：2
- 被过滤 Chunk 数量：1
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：02-api-spec.md，Chunk #0
# 短信验证码 API 说明

## POST /api/sms/send-code

发送短信验证码。

请求体示例：

```json
{
  "phone": "13800138000"
}
```

## POST /api/sms/verify-code

校验短信验证码。

请求体示例：

```json
{
  "phone": "13800138000",…
```
</details>

### Answer 摘要

> SMS_429 表示发送过于频繁。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 2445 | 1791 | 4248 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_003 — ✅ 通过

**测试问题**：send-code 接口路径是什么？

### 请求参数

```json
{
  "kbId": 1,
  "question": "send-code 接口路径是什么？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`35`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：2819 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 02-api-spec.md | 0.7277 | True | — |
| 03-troubleshooting.md | 0.6500 | True | — |
| 01-project-guideline.md | 0.3734 | False | SCORE_TOO_LOW |


### contextChunks

1. `02-api-spec.md` (chunkId=22, score=0.7277289251312072)
2. `03-troubleshooting.md` (chunkId=23, score=0.6499979123476419)


### Prompt 检查

- Prompt 长度：940 字符
- contextChunks 数量：2
- 被过滤 Chunk 数量：1
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：02-api-spec.md，Chunk #0
# 短信验证码 API 说明

## POST /api/sms/send-code

发送短信验证码。

请求体示例：

```json
{
  "phone": "13800138000"
}
```

## POST /api/sms/verify-code

校验短信验证码。

请求体示例：

```json
{
  "phone": "13800138000",…
```
</details>

### Answer 摘要

> 根据文档，短信验证码发送接口路径为：`POST /api/sms/send-code`。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 601 | 2177 | 2781 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_004 — ✅ 通过

**测试问题**：公司年终奖发几个月？

### 请求参数

```json
{
  "kbId": 1,
  "question": "公司年终奖发几个月？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`36`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：2737 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 03-troubleshooting.md | 0.2485 | True | — |
| 02-api-spec.md | 0.2242 | False | SCORE_TOO_LOW |
| 01-project-guideline.md | 0.1796 | False | SCORE_TOO_LOW |


### contextChunks

1. `03-troubleshooting.md` (chunkId=23, score=0.2485085876573887)


### Prompt 检查

- Prompt 长度：579 字符
- contextChunks 数量：1
- 被过滤 Chunk 数量：2
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：03-troubleshooting.md，Chunk #0
# 短信验证码发送失败排查流程

当用户反馈「收不到验证码」时，按以下步骤排查：

## 1. 检查手机号格式

确认手机号符合国内 11 位规则，排除空格与区号错误。

## 2. 检查频率限制

查询 Redis 中该手机号的发送次数与冷却时间，确认未触发 SMS_429。

## 3. 检查 Redis 缓存

确认 Redis 连接正常，验证码写入与…
```
</details>

### Answer 摘要

> 知识库中没有找到相关依据。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 615 | 2061 | 2684 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_005 — ✅ 通过

**测试问题**：短信验证码接口有哪些限制？

### 请求参数

```json
{
  "kbId": 1,
  "question": "短信验证码接口有哪些限制？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`37`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：6122 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 03-troubleshooting.md | 0.7763 | True | — |
| 02-api-spec.md | 0.7087 | True | — |
| 01-project-guideline.md | 0.3129 | False | SCORE_TOO_LOW |


### contextChunks

1. `03-troubleshooting.md` (chunkId=23, score=0.7762826155773964)
2. `02-api-spec.md` (chunkId=22, score=0.7086813736191986)


### Prompt 检查

- Prompt 长度：935 字符
- contextChunks 数量：2
- 被过滤 Chunk 数量：1
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：03-troubleshooting.md，Chunk #0
# 短信验证码发送失败排查流程

当用户反馈「收不到验证码」时，按以下步骤排查：

## 1. 检查手机号格式

确认手机号符合国内 11 位规则，排除空格与区号错误。

## 2. 检查频率限制

查询 Redis 中该手机号的发送次数与冷却时间，确认未触发 SMS_429。

## 3. 检查 Redis 缓存

确认 Redis 连接正常，验证码写入与…
```
</details>

### Answer 摘要

> 根据知识库，短信验证码接口的限制包括：  - **手机号格式限制**：需符合国内 11 位规则，否则返回 `SMS_400` 错误。 - **发送频率限制**：通过 Redis 记录每手机号的发送次数与冷却时间，发送过于频繁会触发 `SMS_429` 错误。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 1084 | 4995 | 6089 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_006 — ✅ 通过

**测试问题**：为什么后续会使用 PgVector？

### 请求参数

```json
{
  "kbId": 1,
  "question": "为什么后续会使用 PgVector？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`38`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：6428 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 01-project-guideline.md | 0.6610 | True | — |
| 02-api-spec.md | 0.3184 | False | SCORE_TOO_LOW |
| 03-troubleshooting.md | 0.2786 | False | SCORE_TOO_LOW |


### contextChunks

1. `01-project-guideline.md` (chunkId=21, score=0.661027809320224)


### Prompt 检查

- Prompt 长度：864 字符
- contextChunks 数量：1
- 被过滤 Chunk 数量：2
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：01-project-guideline.md，Chunk #0
# 项目背景

AI RAG Lab Platform 是企业级 RAG 知识库实验平台，用于逐步验证文档导入、分块、检索与问答等能力。

## 技术栈

- 后端：Spring Boot 3、JDK 17、Maven、PostgreSQL、MyBatis-Plus
- 前端：Vue 3、TypeScript、Vite、Element Plus
- 基础…
```
</details>

### Answer 摘要

> 根据提供的知识库上下文，后续会使用 PgVector 是因为：  > “在后续版本中，向量 Embedding 将存储在 PostgreSQL 的 PgVector 扩展中，以便在同一数据库内完成元数据与向量检索，简化运维。”  简而言之，使用 PgVector 可以将向量数据与原有的关系型元数据统一存放在 PostgreSQL 中，实现元数据检索与向量检索的融合，从而降低架构复杂度和运维成本。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 1949 | 4350 | 6302 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_007 — ✅ 通过

**测试问题**：Redis 缓存异常怎么排查？

### 请求参数

```json
{
  "kbId": 1,
  "question": "Redis 缓存异常怎么排查？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`39`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：3479 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 03-troubleshooting.md | 0.5566 | True | — |
| 01-project-guideline.md | 0.3750 | False | SCORE_TOO_LOW |
| 02-api-spec.md | 0.3530 | False | SCORE_TOO_LOW |


### contextChunks

1. `03-troubleshooting.md` (chunkId=23, score=0.5565598736524968)


### Prompt 检查

- Prompt 长度：584 字符
- contextChunks 数量：1
- 被过滤 Chunk 数量：2
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：03-troubleshooting.md，Chunk #0
# 短信验证码发送失败排查流程

当用户反馈「收不到验证码」时，按以下步骤排查：

## 1. 检查手机号格式

确认手机号符合国内 11 位规则，排除空格与区号错误。

## 2. 检查频率限制

查询 Redis 中该手机号的发送次数与冷却时间，确认未触发 SMS_429。

## 3. 检查 Redis 缓存

确认 Redis 连接正常，验证码写入与…
```
</details>

### Answer 摘要

> 根据知识库中的排查流程，检查 Redis 缓存异常时，需要确认 Redis 连接正常，并且验证码写入与读取的 key 是否一致。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 792 | 2642 | 3441 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_008 — ✅ 通过

**测试问题**：公司股票期权归属规则是什么？

### 请求参数

```json
{
  "kbId": 1,
  "question": "公司股票期权归属规则是什么？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`40`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：3400 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 02-api-spec.md | 0.2522 | True | — |
| 03-troubleshooting.md | 0.2365 | False | SCORE_TOO_LOW |
| 01-project-guideline.md | 0.2187 | False | SCORE_TOO_LOW |


### contextChunks

1. `02-api-spec.md` (chunkId=22, score=0.2522427818234465)


### Prompt 检查

- Prompt 长度：555 字符
- contextChunks 数量：1
- 被过滤 Chunk 数量：2
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：02-api-spec.md，Chunk #0
# 短信验证码 API 说明

## POST /api/sms/send-code

发送短信验证码。

请求体示例：

```json
{
  "phone": "13800138000"
}
```

## POST /api/sms/verify-code

校验短信验证码。

请求体示例：

```json
{
  "phone": "13800138000",…
```
</details>

### Answer 摘要

> 知识库中没有找到相关依据。

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 1235 | 2143 | 3382 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_009 — ✅ 通过

**测试问题**：短信验证码发不出去怎么排查？

### 请求参数

```json
{
  "kbId": 1,
  "question": "短信验证码发不出去怎么排查？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`41`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：5377 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 03-troubleshooting.md | 0.8230 | True | — |
| 02-api-spec.md | 0.6193 | True | — |
| 01-project-guideline.md | 0.2701 | False | SCORE_TOO_LOW |


### contextChunks

1. `03-troubleshooting.md` (chunkId=23, score=0.823002671776782)
2. `02-api-spec.md` (chunkId=22, score=0.6193453484970192)


### Prompt 检查

- Prompt 长度：936 字符
- contextChunks 数量：2
- 被过滤 Chunk 数量：1
- **泄漏检测**：被过滤 Chunk 正文未出现在 Prompt 中 ✓

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：03-troubleshooting.md，Chunk #0
# 短信验证码发送失败排查流程

当用户反馈「收不到验证码」时，按以下步骤排查：

## 1. 检查手机号格式

确认手机号符合国内 11 位规则，排除空格与区号错误。

## 2. 检查频率限制

查询 Redis 中该手机号的发送次数与冷却时间，确认未触发 SMS_429。

## 3. 检查 Redis 缓存

确认 Redis 连接正常，验证码写入与…
```
</details>

### Answer 摘要

> 可按照以下步骤排查：  1. **检查手机号格式** – 确认号码符合国内11位规则，无空格和区号错误。 2. **检查频率限制** – 查询 Redis 中该手机号发送次数与冷却时间，确认未触发 `SMS_429` 错误。 3. **检查 Redis 缓存** – 确认 Redis 连接正常，验证码写入和读取的 key 是否一致。 4. **检查短信服务商返回码** – 查看服务商 API 响应…

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 778 | 4555 | 5337 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---

## V3_TC_010 — ✅ 通过

**测试问题**：短信验证码发不出去怎么排查？

### 请求参数

```json
{
  "kbId": 1,
  "question": "短信验证码发不出去怎么排查？",
  "topK": 5
}
```

### 接口状态

- HTTP 状态：200
- queryLogId：`42`
- Embedding：`qwen` / `text-embedding-v4`
- Chat：`deepseek` / `deepseek-v4-pro`
- 请求耗时（脚本侧）：5484 ms

### retrievedChunks

| documentName | score | usedInPrompt | filterReason |
|--------------|-------|--------------|--------------|
| 03-troubleshooting.md | 0.8230 | True | — |
| 02-api-spec.md | 0.6193 | True | — |
| 01-project-guideline.md | 0.2701 | False | SCORE_TOO_LOW |


### contextChunks

1. `03-troubleshooting.md` (chunkId=23, score=0.823002671776782)
2. `02-api-spec.md` (chunkId=22, score=0.6193453484970192)


### Prompt 检查

- Prompt 长度：936 字符
- contextChunks 数量：2
- 被过滤 Chunk 数量：1

<details><summary>Prompt 预览（前 400 字符）</summary>

```
你是一个企业知识库问答助手。

请严格根据下面提供的知识库上下文回答用户问题。

要求：
1. 只能使用上下文中的信息回答。
2. 优先使用与问题最相关的片段。
3. 如果多个片段相关，请综合回答，但不要引入无关内容。
4. 如果上下文中没有答案，请回答：“知识库中没有找到相关依据。”
5. 不要编造不存在的信息。
6. 回答要简洁、清晰。

知识库上下文：
【片段1】文档：03-troubleshooting.md，Chunk #0
# 短信验证码发送失败排查流程

当用户反馈「收不到验证码」时，按以下步骤排查：

## 1. 检查手机号格式

确认手机号符合国内 11 位规则，排除空格与区号错误。

## 2. 检查频率限制

查询 Redis 中该手机号的发送次数与冷却时间，确认未触发 SMS_429。

## 3. 检查 Redis 缓存

确认 Redis 连接正常，验证码写入与…
```
</details>

### Answer 摘要

> 根据知识库，可按以下步骤排查短信验证码发送失败的问题：  1. **检查手机号格式**：确认手机号是否符合国内11位规则，排除空格与区号错误。 2. **检查频率限制**：查询Redis中该手机号的发送次数与冷却时间，确认是否触发SMS_429（发送过于频繁）。 3. **检查Redis缓存**：确认Redis连接正常，验证码写入与读取的key是否一致。 4. **检查短信服务商返回码**：查看服…

### 耗时（latency）

| retrievalTimeMs | generationTimeMs | totalTimeMs |
|-----------------|------------------|-------------|
| 775 | 4671 | 5450 |

### 是否通过

**✅ 通过**

### 失败原因 / 后续建议

- 无

---
