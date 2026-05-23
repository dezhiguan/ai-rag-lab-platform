package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EvaluationCompareResponse {

    private Long kbId;
    private Boolean enableRerank;
    private List<EvaluationModeSummaryResponse> modeSummaries;
    private List<EvaluationCompareCaseResponse> cases;
}
