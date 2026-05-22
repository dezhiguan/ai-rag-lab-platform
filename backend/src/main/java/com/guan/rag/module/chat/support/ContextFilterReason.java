package com.guan.rag.module.chat.support;

/**
 * Context 组装过滤原因（V3 轻量级过滤，非 Reranker）。
 */
public final class ContextFilterReason {

    public static final String SCORE_TOO_LOW = "SCORE_TOO_LOW";
    public static final String SCORE_GAP_TOO_LARGE = "SCORE_GAP_TOO_LARGE";
    public static final String EXCEED_MAX_CONTEXT_CHUNKS = "EXCEED_MAX_CONTEXT_CHUNKS";

    private ContextFilterReason() {
    }
}
