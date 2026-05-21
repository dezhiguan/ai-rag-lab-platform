package com.guan.rag.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.controller.response.DashboardStatsResponse;
import com.guan.rag.module.document.service.DocumentService;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.sample.service.SampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "仪表盘统计")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final SampleDataService sampleDataService;

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> stats() {
        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .knowledgeBaseCount(knowledgeBaseService.count())
                .documentCount(documentService.count())
                .chunkCount(documentService.countChunks())
                .sampleDataInitialized(sampleDataService.isInitialized())
                .build();
        return ApiResponse.success(response);
    }
}
