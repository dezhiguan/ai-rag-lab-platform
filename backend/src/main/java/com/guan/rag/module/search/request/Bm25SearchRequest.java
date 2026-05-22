package com.guan.rag.module.search.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Bm25SearchRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    @NotBlank(message = "query 不能为空")
    private String query;

    @Min(value = 1, message = "topK 至少为 1")
    private Integer topK = 5;
}
