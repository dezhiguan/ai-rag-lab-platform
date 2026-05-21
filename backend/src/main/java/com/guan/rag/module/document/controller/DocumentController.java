package com.guan.rag.module.document.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.document.response.DocumentChunkResponse;
import com.guan.rag.module.document.response.DocumentResponse;
import com.guan.rag.module.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "文档", description = "文档上传、解析与分块")
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "上传文档")
    @PostMapping("/api/kb/{kbId}/documents/upload")
    public ApiResponse<DocumentResponse> upload(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(kbId, file));
    }

    @Operation(summary = "查询知识库文档列表")
    @GetMapping("/api/kb/{kbId}/documents")
    public ApiResponse<List<DocumentResponse>> listByKb(@PathVariable Long kbId) {
        return ApiResponse.success(documentService.listByKbId(kbId));
    }

    @Operation(summary = "查询文档详情")
    @GetMapping("/api/documents/{documentId}")
    public ApiResponse<DocumentResponse> detail(@PathVariable Long documentId) {
        return ApiResponse.success(documentService.getById(documentId));
    }

    @Operation(summary = "查询文档 Chunk 列表")
    @GetMapping("/api/documents/{documentId}/chunks")
    public ApiResponse<List<DocumentChunkResponse>> chunks(@PathVariable Long documentId) {
        return ApiResponse.success(documentService.listChunks(documentId));
    }
}
