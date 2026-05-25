package com.guan.rag.module.token;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenUsageResult {

    private int questionTokens;
    private int contextTokens;
    private int systemPromptTokens;
    private int answerTokens;
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
    /** 预估费用（元），未配置价格时为 0 */
    private BigDecimal estimatedCost;
    /** 是否已配置模型单价 */
    private boolean priceConfigured;
    /** 费用说明，如「未配置价格」 */
    private String costNote;
    private String chatProvider;
    private String chatModel;
}
