package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EvaluationRunResponse {

    private Long kbId;
    private String searchMode;
    private Integer totalCount;
    private Integer passedCount;
    private Integer failedCount;
    private Double passRate;
    private Long totalLatencyMs;
    private List<EvaluationCaseResultResponse> results;
}
