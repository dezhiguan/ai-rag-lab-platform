package com.guan.rag.module.evaluation.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.evaluation.request.EvaluationRunRequest;
import com.guan.rag.module.evaluation.response.EvaluationRunResponse;
import com.guan.rag.module.evaluation.response.EvaluationTestCaseResponse;
import com.guan.rag.module.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Evaluation", description = "V7 评测中心：批量检索 Top1 命中验收")
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "内置评测用例列表")
    @GetMapping("/cases")
    public ApiResponse<List<EvaluationTestCaseResponse>> listCases() {
        return ApiResponse.success(evaluationService.listBuiltinCases());
    }

    @Operation(summary = "执行批量评测")
    @PostMapping("/run")
    public ApiResponse<EvaluationRunResponse> run(@Valid @RequestBody EvaluationRunRequest request) {
        return ApiResponse.success(evaluationService.run(request));
    }
}
