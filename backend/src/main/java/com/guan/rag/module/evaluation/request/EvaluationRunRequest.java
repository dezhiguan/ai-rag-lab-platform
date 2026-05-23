package com.guan.rag.module.evaluation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationRunRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    /** VECTOR（默认）、BM25 或 HYBRID */
    private String searchMode = "VECTOR";

    @Min(value = 1, message = "topK 至少为 1")
    private Integer topK = 5;
}
