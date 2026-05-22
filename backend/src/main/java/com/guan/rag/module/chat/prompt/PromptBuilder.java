package com.guan.rag.module.chat.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private static final String TEMPLATE = """
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
            {context}

            用户问题：
            {question}
            """;

    public String build(String context, String question) {
        String safeContext = context == null || context.isBlank() ? "（无相关上下文）" : context;
        String safeQuestion = question == null ? "" : question;
        return TEMPLATE
                .replace("{context}", safeContext)
                .replace("{question}", safeQuestion);
    }
}
