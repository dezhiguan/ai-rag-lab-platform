package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationCaseResultResponse {

    private String caseId;
    private String question;
    private String expectedDocument;
    private String actualTop1Document;
    private Boolean passed;
    private String searchMode;
    private Long latencyMs;
    private String message;
}
