package com.guan.rag.module.experiment.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentRagQueryRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    @NotBlank(message = "问题不能为空")
    private String question;

    @Min(value = 1, message = "topK 至少为 1")
    private Integer topK = 5;

    private String searchMode = "VECTOR";

    private Boolean enableRerank = false;

    @Min(value = 1, message = "maxChunks 至少为 1")
    private Integer maxChunks = 2;

    @DecimalMin(value = "0.0", message = "minScore 不能小于 0")
    @DecimalMax(value = "1.0", message = "minScore 不能大于 1")
    private Double minScore = 0.45;

    @DecimalMin(value = "0.0", message = "maxScoreGap 不能小于 0")
    @DecimalMax(value = "1.0", message = "maxScoreGap 不能大于 1")
    private Double maxScoreGap = 0.35;
}
