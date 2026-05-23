package com.guan.rag.module.search.hybrid;

import lombok.Builder;
import lombok.Data;

/**
 * Hybrid 融合后的召回结果，按 {@link #hybridScore} 降序排列。
 */
@Data
@Builder
public class HybridSearchResult {

    private Long chunkId;
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String content;

    /** RRF 融合分，越大越靠前 */
    private Double hybridScore;

    private Double vectorScore;
    private Double bm25Score;
    /** 1-based，未出现在该路则为 null */
    private Integer vectorRank;
    private Integer bm25Rank;
}
