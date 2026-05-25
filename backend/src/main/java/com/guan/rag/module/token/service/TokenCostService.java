package com.guan.rag.module.token.service;

import com.guan.rag.module.token.mapper.TokenCostMapper;
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
                        "Debug 查询已展示 Token 组成与预估费用",
                        "参数实验台已展示单次实验 Token 与对比表费用列",
                        "查询日志已持久化 Token 字段，看板展示累计统计"
                ))
                .allTime(toStats(tokenCostMapper.aggregateAll()))
                .recent7Days(toStats(tokenCostMapper.aggregateRecent()))
                .modelPrices(List.of(
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
                    .totalCost(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                    .avgTokens(0)
                    .avgCost(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
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

    private BigDecimal scaleCost(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
