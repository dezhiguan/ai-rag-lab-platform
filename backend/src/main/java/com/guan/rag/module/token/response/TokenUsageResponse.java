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
    private int totalTokens;
    private BigDecimal estimatedCost;
    private boolean priceConfigured;
    private String costNote;
    private String chatProvider;
    private String chatModel;

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
                .totalTokens(result.getTotalTokens())
                .estimatedCost(result.getEstimatedCost())
                .priceConfigured(result.isPriceConfigured())
                .costNote(result.getCostNote())
                .chatProvider(result.getChatProvider())
                .chatModel(result.getChatModel())
                .build();
    }
}
