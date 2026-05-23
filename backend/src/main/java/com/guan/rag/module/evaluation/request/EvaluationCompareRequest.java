package com.guan.rag.module.evaluation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationCompareRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    @Min(value = 1, message = "topK 至少为 1")
    private Integer topK = 5;

    /** 是否对召回结果做轻量 Reranker 后再取 Top1（默认 false） */
    private Boolean enableRerank = false;
}
