package com.guan.rag.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.config.RagProperties;
import com.guan.rag.controller.response.ModelProvidersResponse;
import com.guan.rag.module.chat.provider.ChatModelProviderRouter;
import com.guan.rag.module.embedding.provider.EmbeddingProviderRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "模型 Provider", description = "V2.5 当前 Embedding / Chat Provider 配置")
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelController {

    private final RagProperties ragProperties;
    private final EmbeddingProviderRouter embeddingProviderRouter;
    private final ChatModelProviderRouter chatModelProviderRouter;

    @Operation(summary = "查看当前模型 Provider 配置")
    @GetMapping("/providers")
    public ApiResponse<ModelProvidersResponse> providers() {
        return ApiResponse.success(ModelProvidersResponse.builder()
                .embeddingProvider(embeddingProviderRouter.configuredProvider())
                .embeddingModel(embeddingProviderRouter.model())
                .embeddingDimension(embeddingProviderRouter.dimension())
                .embeddingDelegate(embeddingProviderRouter.delegateType())
                .chatProvider(chatModelProviderRouter.configuredProvider())
                .chatModel(chatModelProviderRouter.model())
                .chatDelegate(chatModelProviderRouter.delegateType())
                .build());
    }
}
