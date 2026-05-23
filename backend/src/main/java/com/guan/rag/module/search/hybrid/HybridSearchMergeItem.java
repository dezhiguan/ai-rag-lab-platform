package com.guan.rag.module.search.hybrid;

import lombok.Builder;
import lombok.Data;

/**
 * 单路召回条目，供 Hybrid 融合前使用（Vector 或 BM25）。
 */
@Data
@Builder
public class HybridSearchMergeItem {

    private Long chunkId;
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private Double score;
    private String content;
}
