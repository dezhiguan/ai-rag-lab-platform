package com.guan.rag.controller;

import com.guan.rag.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "系统", description = "系统基础接口")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("appName", "ai-rag-lab-platform");
        data.put("version", "V1");
        data.put("timestamp", Instant.now().toString());
        return ApiResponse.success(data);
    }
}
