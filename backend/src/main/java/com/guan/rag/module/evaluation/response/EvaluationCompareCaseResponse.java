package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationCompareCaseResponse {

    private String caseId;
    private String question;
    private String expectedDocument;
    private EvaluationModeHitResponse vector;
    private EvaluationModeHitResponse bm25;
    private EvaluationModeHitResponse hybrid;
}
