package com.guan.rag.module.token.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TokenCostOverviewResponse {

    private boolean tokenTrackingEnabled;
    private List<String> capabilities;
    private TokenCostStatsResponse allTime;
    private TokenCostStatsResponse recent7Days;
    private EmbeddingCostStatsResponse embeddingAllTime;
    private EmbeddingCostStatsResponse embeddingRecent7Days;
    private List<ModelPriceInfoResponse> modelPrices;

    @Data
    @Builder
    public static class TokenCostStatsResponse {
        private long queryCount;
        private long totalTokens;
        private BigDecimal totalCost;
        private long avgTokens;
        private BigDecimal avgCost;
    }

    @Data
    @Builder
    public static class EmbeddingCostStatsResponse {
        private long queryCount;
        private long embeddingTokens;
        private BigDecimal embeddingCost;
        private long avgEmbeddingTokens;
        private BigDecimal avgEmbeddingCost;
        private String embeddingProvider;
        private String embeddingModel;
    }

    @Data
    @Builder
    public static class ModelPriceInfoResponse {
        private String model;
        private String inputPriceNote;
        private String outputPriceNote;
    }
}
