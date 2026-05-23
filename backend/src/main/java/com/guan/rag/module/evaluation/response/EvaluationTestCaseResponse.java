package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationTestCaseResponse {

    private String caseId;
    private String question;
    private String expectedDocument;
}
