package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationModeSummaryResponse {

    private String searchMode;
    private Integer totalCount;
    private Integer passedCount;
    private Integer failedCount;
    private Double passRate;
    private Long totalLatencyMs;
    private Long avgLatencyMs;
}
