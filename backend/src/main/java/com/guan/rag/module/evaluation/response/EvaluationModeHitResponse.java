package com.guan.rag.module.evaluation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationModeHitResponse {

    private String searchMode;
    private String actualTop1Document;
    private Boolean passed;
    private Long latencyMs;
}
