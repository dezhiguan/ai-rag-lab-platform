package com.guan.rag.module.retrieval.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RetrievalTestRequest {

    @NotNull(message = "kbId 不能为空")
    private Long kbId;

    @NotBlank(message = "query 不能为空")
    private String query;

    private Integer topK = 5;
}
