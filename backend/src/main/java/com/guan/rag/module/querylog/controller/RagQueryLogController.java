package com.guan.rag.module.querylog.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.querylog.response.RagQueryLogPageResponse;
import com.guan.rag.module.querylog.service.RagQueryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RAG Query Logs", description = "V8 RAG 查询日志中心")
@RestController
@RequestMapping("/api/rag/query-logs")
@RequiredArgsConstructor
public class RagQueryLogController {

    private final RagQueryLogService ragQueryLogService;

    @Operation(summary = "分页查询 RAG 查询日志（基于 rag_query_log）")
    @GetMapping
    public ApiResponse<RagQueryLogPageResponse> pageQueryLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchMode,
            @RequestParam(required = false) Boolean enableRerank,
            @RequestParam(required = false) Boolean slowOnly
    ) {
        return ApiResponse.success(ragQueryLogService.pageQueryLogs(
                page,
                pageSize,
                keyword,
                searchMode,
                enableRerank,
                slowOnly
        ));
    }
}
