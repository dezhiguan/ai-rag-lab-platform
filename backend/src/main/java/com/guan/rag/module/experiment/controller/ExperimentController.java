package com.guan.rag.module.experiment.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.experiment.request.ExperimentRagQueryRequest;
import com.guan.rag.module.experiment.response.ExperimentRagQueryResponse;
import com.guan.rag.module.experiment.service.ExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Experiment", description = "V8 RAG 参数实验台")
@RestController
@RequestMapping("/api/experiment")
@RequiredArgsConstructor
public class ExperimentController {

    private final ExperimentService experimentService;

    @Operation(summary = "运行 RAG 参数实验（Context 参数仅对本次请求生效）")
    @PostMapping("/rag-query")
    public ApiResponse<ExperimentRagQueryResponse> ragQuery(@Valid @RequestBody ExperimentRagQueryRequest request) {
        return ApiResponse.success(experimentService.runRagQuery(request));
    }
}
