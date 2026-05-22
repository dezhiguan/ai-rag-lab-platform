package com.guan.rag.module.retrieval.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.retrieval.request.RetrievalTestRequest;
import com.guan.rag.module.retrieval.response.RetrievalTestResponse;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.retrieval.service.VectorRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "向量检索", description = "V2 基础向量检索（含内部测试接口）")
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
public class RetrievalController {

    private final VectorRetrievalService vectorRetrievalService;

    @Operation(summary = "向量检索测试（V2 基线验证）")
    @PostMapping("/test")
    public ApiResponse<RetrievalTestResponse> test(@Valid @RequestBody RetrievalTestRequest request) {
        int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
        List<RetrievedChunkResponse> results = vectorRetrievalService.retrieve(
                request.getKbId(), request.getQuery(), topK);
        return ApiResponse.success(RetrievalTestResponse.builder()
                .query(request.getQuery())
                .results(results)
                .build());
    }
}
