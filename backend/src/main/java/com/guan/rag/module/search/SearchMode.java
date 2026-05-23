package com.guan.rag.module.search;

import com.guan.rag.common.exception.BusinessException;

/**
 * Debug 检索模式（V5 起支持 HYBRID）。
 */
public enum SearchMode {

    VECTOR,
    BM25,
    HYBRID;

    public static SearchMode from(String value) {
        if (value == null || value.isBlank()) {
            return VECTOR;
        }
        try {
            return SearchMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的 searchMode: " + value + "，仅支持 VECTOR、BM25、HYBRID");
        }
    }
}
