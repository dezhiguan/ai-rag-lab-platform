package com.guan.rag.module.debug.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DebugQueryRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    @NotBlank(message = "问题不能为空")
    private String question;

    @Min(value = 1, message = "topK 至少为 1")
    private Integer topK = 5;

    /** VECTOR（默认）、BM25 或 HYBRID */
    private String searchMode = "VECTOR";

    /** 是否对召回结果做轻量 Reranker 重排（默认 false） */
    private Boolean enableRerank = false;
}
