package com.guan.rag.module.debug.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DebugRetrievedChunkResponse {

    private Long documentId;
    private String documentName;
    private Long chunkId;
    private Integer chunkIndex;
    private Double score;
    private Integer rankPosition;
    private String content;
    private Boolean usedInPrompt;
    /** 未进入 Prompt 时的原因：SCORE_TOO_LOW / SCORE_GAP_TOO_LARGE / EXCEED_MAX_CONTEXT_CHUNKS */
    private String filterReason;

    /** HYBRID 模式：是否出现在向量召回路 */
    private Boolean matchedByVector;
    /** HYBRID 模式：是否出现在 BM25 召回路 */
    private Boolean matchedByBm25;
    /** HYBRID 模式：向量原始相似度 */
    private Double vectorScore;
    /** HYBRID 模式：BM25 分数（已按 Top1 归一化到 [0,1]） */
    private Double bm25Score;
    /** HYBRID 模式：RRF 融合分 */
    private Double hybridScore;

    /** 启用 Reranker 时：检索原始排名 */
    private Integer originalRank;
    /** 启用 Reranker 时：重排后排名 */
    private Integer rerankRank;
    /** 启用 Reranker 时：重排分数 */
    private Double rerankScore;
}
