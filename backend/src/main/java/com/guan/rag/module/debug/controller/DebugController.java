package com.guan.rag.module.debug.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.debug.request.DebugQueryRequest;
import com.guan.rag.module.debug.response.DebugQueryLogSummaryResponse;
import com.guan.rag.module.debug.response.DebugQueryResponse;
import com.guan.rag.module.debug.service.DebugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "RAG Debug", description = "V3 RAG Debug 可观察：召回、Context、Prompt、耗时、历史")
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService debugService;

    @Operation(summary = "执行 Debug 查询")
    @PostMapping("/query")
    public ApiResponse<DebugQueryResponse> query(@Valid @RequestBody DebugQueryRequest request) {
        return ApiResponse.success(debugService.query(request));
    }

    @Operation(summary = "Debug 查询历史列表")
    @GetMapping("/query-logs")
    public ApiResponse<List<DebugQueryLogSummaryResponse>> listQueryLogs(
            @RequestParam(required = false) Long kbId) {
        return ApiResponse.success(debugService.listQueryLogs(kbId));
    }

    @Operation(summary = "Debug 查询详情")
    @GetMapping("/query-logs/{queryLogId}")
    public ApiResponse<DebugQueryResponse> getQueryLog(@PathVariable Long queryLogId) {
        return ApiResponse.success(debugService.getQueryLogDetail(queryLogId));
    }
}
