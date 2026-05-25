package com.guan.rag.module.token.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TokenCostStatsRow {

    private Long queryCount;
    private Long totalTokens;
    private BigDecimal totalCost;
    private BigDecimal avgTokens;
    private BigDecimal avgCost;
}
