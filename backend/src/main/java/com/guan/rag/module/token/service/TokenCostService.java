package com.guan.rag.module.token.service;

import com.guan.rag.module.token.mapper.TokenCostMapper;
import com.guan.rag.module.token.model.EmbeddingCostStatsRow;
import com.guan.rag.module.token.model.TokenCostStatsRow;
import com.guan.rag.module.token.response.TokenCostOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenCostService {

    private final TokenCostMapper tokenCostMapper;

    public TokenCostOverviewResponse getOverview() {
        return TokenCostOverviewResponse.builder()
                .tokenTrackingEnabled(true)
                .capabilities(List.of(
                        "Debug / 参数实验台：Embedding + Chat + Total 分项 Token 与费用",
                        "qwen/text-embedding-v4：按问题 Token × 单价估算 Embedding 成本",
                        "查询日志持久化 embedding_tokens、embedding_cost、total_cost"
                ))
                .allTime(toStats(tokenCostMapper.aggregateAll()))
                .recent7Days(toStats(tokenCostMapper.aggregateRecent()))
                .embeddingAllTime(toEmbeddingStats(tokenCostMapper.aggregateEmbeddingAll()))
                .embeddingRecent7Days(toEmbeddingStats(tokenCostMapper.aggregateEmbeddingRecent()))
                .modelPrices(List.of(
                        TokenCostOverviewResponse.ModelPriceInfoResponse.builder()
                                .model("qwen / text-embedding-v4")
                                .inputPriceNote("约 ¥0.0007 / 千 tokens（问题文本估算）")
                                .outputPriceNote("—")
                                .build(),
                        TokenCostOverviewResponse.ModelPriceInfoResponse.builder()
                                .model("deepseek-chat")
                                .inputPriceNote("约 ¥1 / 百万 input tokens")
                                .outputPriceNote("约 ¥2 / 百万 output tokens")
                                .build(),
                        TokenCostOverviewResponse.ModelPriceInfoResponse.builder()
                                .model("deepseek-v4-pro")
                                .inputPriceNote("约 ¥3 / 百万 input tokens")
                                .outputPriceNote("约 ¥6 / 百万 output tokens")
                                .build(),
                        TokenCostOverviewResponse.ModelPriceInfoResponse.builder()
                                .model("mock / local")
                                .inputPriceNote("¥0（实验用）")
                                .outputPriceNote("¥0（实验用）")
                                .build()
                ))
                .build();
    }

    private TokenCostOverviewResponse.TokenCostStatsResponse toStats(TokenCostStatsRow row) {
        if (row == null || row.getQueryCount() == null || row.getQueryCount() == 0) {
            return TokenCostOverviewResponse.TokenCostStatsResponse.builder()
                    .queryCount(0)
                    .totalTokens(0)
                    .totalCost(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP))
                    .avgTokens(0)
                    .avgCost(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP))
                    .build();
        }
        return TokenCostOverviewResponse.TokenCostStatsResponse.builder()
                .queryCount(row.getQueryCount())
                .totalTokens(row.getTotalTokens() != null ? row.getTotalTokens() : 0)
                .totalCost(scaleCost(row.getTotalCost()))
                .avgTokens(row.getAvgTokens() != null ? row.getAvgTokens().longValue() : 0)
                .avgCost(scaleCost(row.getAvgCost()))
                .build();
    }

    private TokenCostOverviewResponse.EmbeddingCostStatsResponse toEmbeddingStats(EmbeddingCostStatsRow row) {
        if (row == null || row.getQueryCount() == null || row.getQueryCount() == 0) {
            return TokenCostOverviewResponse.EmbeddingCostStatsResponse.builder()
                    .queryCount(0)
                    .embeddingTokens(0)
                    .embeddingCost(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP))
                    .avgEmbeddingTokens(0)
                    .avgEmbeddingCost(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP))
                    .embeddingProvider("qwen")
                    .embeddingModel("text-embedding-v4")
                    .build();
        }
        return TokenCostOverviewResponse.EmbeddingCostStatsResponse.builder()
                .queryCount(row.getQueryCount())
                .embeddingTokens(row.getEmbeddingTokens() != null ? row.getEmbeddingTokens() : 0)
                .embeddingCost(scaleCost(row.getEmbeddingCost()))
                .avgEmbeddingTokens(row.getAvgEmbeddingTokens() != null ? row.getAvgEmbeddingTokens().longValue() : 0)
                .avgEmbeddingCost(scaleCost(row.getAvgEmbeddingCost()))
                .embeddingProvider("qwen")
                .embeddingModel("text-embedding-v4")
                .build();
    }

    private BigDecimal scaleCost(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return value.setScale(8, RoundingMode.HALF_UP);
    }
}
