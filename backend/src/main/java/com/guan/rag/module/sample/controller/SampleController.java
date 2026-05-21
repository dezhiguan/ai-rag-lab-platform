package com.guan.rag.module.sample.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.sample.response.SampleInitResponse;
import com.guan.rag.module.sample.response.SampleStatusResponse;
import com.guan.rag.module.sample.service.SampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "样例数据", description = "样例数据初始化")
@RestController
@RequestMapping("/api/sample")
@RequiredArgsConstructor
public class SampleController {

    private final SampleDataService sampleDataService;

    @Operation(summary = "初始化样例数据")
    @PostMapping("/init")
    public ApiResponse<SampleInitResponse> init() throws IOException {
        return ApiResponse.success(sampleDataService.init());
    }

    @Operation(summary = "查询样例数据状态")
    @GetMapping("/status")
    public ApiResponse<SampleStatusResponse> status() {
        return ApiResponse.success(sampleDataService.status());
    }
}
