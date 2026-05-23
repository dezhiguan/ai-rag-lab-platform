package com.guan.rag.module.search.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResultItemResponse {

    private Long documentId;
    private String documentName;
    private Long chunkId;
    private Integer chunkIndex;
    private Double score;
    private String content;
    private List<String> matchedTerms;
}
