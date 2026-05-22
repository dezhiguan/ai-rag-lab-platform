package com.guan.rag.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelProvidersResponse {

    private String embeddingProvider;
    private String embeddingModel;
    private int embeddingDimension;
    private String embeddingDelegate;

    private String chatProvider;
    private String chatModel;
    private String chatDelegate;
}
