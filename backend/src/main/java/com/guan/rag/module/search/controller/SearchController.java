package com.guan.rag.module.search.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.search.request.Bm25SearchRequest;
import com.guan.rag.module.search.response.Bm25SearchResponse;
import com.guan.rag.module.search.response.EsIndexRebuildResponse;
import com.guan.rag.module.search.service.Bm25SearchService;
import com.guan.rag.module.search.service.EsIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "关键词检索", description = "V4 Elasticsearch BM25")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final EsIndexService esIndexService;
    private final Bm25SearchService bm25SearchService;

    @Operation(summary = "全量重建 ES 索引")
    @PostMapping("/index/rebuild")
    public ApiResponse<EsIndexRebuildResponse> rebuildIndex() {
        return ApiResponse.success(esIndexService.rebuildIndex());
    }

    @Operation(summary = "BM25 关键词检索")
    @PostMapping("/bm25")
    public ApiResponse<Bm25SearchResponse> bm25(@Valid @RequestBody Bm25SearchRequest request) {
        return ApiResponse.success(bm25SearchService.search(request));
    }
}
