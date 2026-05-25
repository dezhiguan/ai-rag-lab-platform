package com.guan.rag.module.token.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmbeddingCostStatsRow {

    private Long queryCount;
    private Long embeddingTokens;
    private BigDecimal embeddingCost;
    private BigDecimal avgEmbeddingTokens;
    private BigDecimal avgEmbeddingCost;
}
