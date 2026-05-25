package com.guan.rag.module.token.response;

import com.guan.rag.module.token.TokenUsageResult;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenUsageResponse {

    private int questionTokens;
    private int contextTokens;
    private int systemPromptTokens;
    private int answerTokens;
    private int inputTokens;
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

    private int totalTokens;
    private BigDecimal totalCost;

    /** 兼容 V11-01，等同 totalCost */
    private BigDecimal estimatedCost;
    private boolean priceConfigured;
    private String costNote;

    public static TokenUsageResponse from(TokenUsageResult result) {
        if (result == null) {
            return null;
        }
        return TokenUsageResponse.builder()
                .questionTokens(result.getQuestionTokens())
                .contextTokens(result.getContextTokens())
                .systemPromptTokens(result.getSystemPromptTokens())
                .answerTokens(result.getAnswerTokens())
                .inputTokens(result.getInputTokens())
                .outputTokens(result.getOutputTokens())
                .embeddingProvider(result.getEmbeddingProvider())
                .embeddingModel(result.getEmbeddingModel())
                .embeddingTokens(result.getEmbeddingTokens())
                .embeddingCost(result.getEmbeddingCost())
                .chatProvider(result.getChatProvider())
                .chatModel(result.getChatModel())
                .chatInputTokens(result.getChatInputTokens())
                .chatOutputTokens(result.getChatOutputTokens())
                .chatCost(result.getChatCost())
                .totalTokens(result.getTotalTokens())
                .totalCost(result.getTotalCost())
                .estimatedCost(result.getEstimatedCost())
                .priceConfigured(result.isPriceConfigured())
                .costNote(result.getCostNote())
                .build();
    }
}
