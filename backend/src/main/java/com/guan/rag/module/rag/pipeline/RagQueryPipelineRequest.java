package com.guan.rag.module.rag.pipeline;

import com.guan.rag.module.chat.support.ContextFilterOptions;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagQueryPipelineRequest {

    private Long kbId;

    private String question;

    private Integer topK;

    private String searchMode;

    private Boolean enableRerank;

    private ContextFilterOptions contextFilter;

    @Builder.Default
    private boolean persistLog = true;
}
