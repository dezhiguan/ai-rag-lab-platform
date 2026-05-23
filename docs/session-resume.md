
---

# 四、docs/SESSION_RESUME.md

```markdown
# SESSION_RESUME.md

## 用途

这个文件用于会话中断后恢复项目上下文。

每次新开 Cursor 会话，不要直接写代码，先让 Cursor 阅读项目上下文。

## 恢复上下文时需要阅读

请先阅读：

1. `AGENTS.md`
2. `docs/AI_CONTEXT.md`
3. `docs/CURRENT_VERSION.md`

阅读后先总结：

1. 当前项目是什么
2. 当前开发到哪个版本
3. 当前版本已经完成什么
4. 当前版本允许做什么
5. 当前版本禁止做什么
6. 下一步应该做什么

总结确认之前，不要修改代码。

## 复制给 Cursor 的恢复提示词

```text
请先恢复当前项目上下文，不要立刻写代码。

请阅读以下文件：

1. AGENTS.md
2. docs/AI_CONTEXT.md
3. docs/CURRENT_VERSION.md

阅读后请先总结：

1. 当前项目是什么
2. 当前开发到哪个版本
3. 当前版本已经完成什么
4. 当前版本允许做什么
5. 当前版本禁止做什么
6. 下一步应该做什么

在你总结并确认之前，不要修改任何代码。