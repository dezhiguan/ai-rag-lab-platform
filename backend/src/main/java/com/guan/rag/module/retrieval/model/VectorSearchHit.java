package com.guan.rag.module.retrieval.model;

import lombok.Data;

@Data
public class VectorSearchHit {

    private Long chunkId;

    private Long documentId;

    private Integer chunkIndex;

    private String documentName;

    private String content;

    private Double score;
}
