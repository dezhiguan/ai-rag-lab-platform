package com.guan.rag.module.embedding.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.embedding.response.EmbeddingRebuildResponse;
import com.guan.rag.module.embedding.response.EmbeddingStatusResponse;
import com.guan.rag.module.embedding.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "向量化", description = "Chunk Embedding 向量化")
@RestController
@RequestMapping("/api/kb/{kbId}/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @Operation(summary = "重建知识库向量")
    @PostMapping("/rebuild")
    public ApiResponse<EmbeddingRebuildResponse> rebuild(@PathVariable Long kbId) {
        return ApiResponse.success(embeddingService.rebuild(kbId));
    }

    @Operation(summary = "查询知识库向量化状态")
    @GetMapping("/status")
    public ApiResponse<EmbeddingStatusResponse> status(@PathVariable Long kbId) {
        return ApiResponse.success(embeddingService.status(kbId));
    }
}
