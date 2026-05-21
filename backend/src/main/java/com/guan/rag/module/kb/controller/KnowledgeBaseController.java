package com.guan.rag.module.kb.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.kb.request.KnowledgeBaseCreateRequest;
import com.guan.rag.module.kb.response.KnowledgeBaseResponse;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "知识库", description = "知识库管理")
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @Operation(summary = "创建知识库")
    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(request));
    }

    @Operation(summary = "查询知识库列表")
    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> list() {
        return ApiResponse.success(knowledgeBaseService.list());
    }

    @Operation(summary = "查询知识库详情")
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.getById(id));
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.success();
    }
}
