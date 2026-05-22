package com.guan.rag.module.chat.support;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;

import java.util.List;

public final class ContextTextBuilder {

    private ContextTextBuilder() {
    }

    public static String build(List<RetrievedChunkResponse> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        List<String> parts = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunkResponse item = chunks.get(i);
            parts.add("【片段" + (i + 1) + "】文档：" + item.getDocumentName()
                    + "，Chunk #" + item.getChunkIndex()
                    + "\n" + item.getContent());
        }
        return String.join("\n\n", parts);
    }
}
