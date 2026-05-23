package com.guan.rag.module.metrics.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.metrics.response.RagMetricsResponse;
import com.guan.rag.module.metrics.service.RagMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RAG Metrics", description = "V8 RAG 运行指标看板")
@RestController
@RequestMapping("/api/rag/metrics")
@RequiredArgsConstructor
public class RagMetricsController {

    private final RagMetricsService ragMetricsService;

    @Operation(summary = "RAG 运行指标（基于 Debug 查询日志）")
    @GetMapping
    public ApiResponse<RagMetricsResponse> metrics() {
        return ApiResponse.success(ragMetricsService.getMetrics());
    }
}
