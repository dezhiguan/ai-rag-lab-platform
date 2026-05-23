package com.guan.rag.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.config.RagProperties;
import com.guan.rag.controller.response.SystemStatusResponse;
import com.guan.rag.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "系统", description = "系统基础接口")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final RagProperties ragProperties;
    private final SystemStatusService systemStatusService;

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        RagProperties.App app = ragProperties.getApp();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("appName", app.getName());
        data.put("version", app.getVersion());
        data.put("timestamp", Instant.now().toString());
        return ApiResponse.success(data);
    }

    @Operation(summary = "系统运行状态看板（V8）")
    @GetMapping("/status")
    public ApiResponse<SystemStatusResponse> status() {
        return ApiResponse.success(systemStatusService.getStatus());
    }
}
