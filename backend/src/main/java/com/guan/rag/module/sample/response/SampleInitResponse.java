package com.guan.rag.module.sample.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleInitResponse {

    private boolean initialized;
    private Long knowledgeBaseId;
    private int documentCount;
    private String message;
}
