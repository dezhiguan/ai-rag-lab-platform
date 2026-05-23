package com.guan.rag.module.search.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Bm25SearchResponse {

    private String query;
    private List<String> extractedTerms;
    private List<SearchResultItemResponse> results;
}
