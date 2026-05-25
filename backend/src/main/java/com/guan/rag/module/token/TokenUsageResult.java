package com.guan.rag.module.token;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenUsageResult {

    /** 用户问题 token（与 embedding 估算同源） */
    private int questionTokens;
    private int contextTokens;
    private int systemPromptTokens;
    private int answerTokens;
    /** Chat 输入 token */
    private int inputTokens;
    /** Chat 输出 token */
    private int outputTokens;

    private String embeddingProvider;
    private String embeddingModel;
    private int embeddingTokens;
    private BigDecimal embeddingCost;

    private String chatProvider;
    private String chatModel;
    private int chatInputTokens;
    private int chatOutputTokens;
    private BigDecimal chatCost;

    /** embedding + chat */
    private int totalTokens;
    private BigDecimal totalCost;

    /** 兼容 V11-01：等同 totalCost */
    private BigDecimal estimatedCost;
    private boolean priceConfigured;
    private String costNote;
}
