package com.guan.rag.module.chat.support;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat 专用：根据 query 与 chunk 的字符 n-gram 重叠判断是否有相关依据。
 * 仅用于问答上下文过滤，不影响 retrieval/test 的 TopK 返回。
 */
public final class ChatRelevanceFilter {

    private static final int BIGRAM = 2;
    private static final int TRIGRAM = 3;

    private ChatRelevanceFilter() {
    }

    public static List<RetrievedChunkResponse> filter(String question, List<RetrievedChunkResponse> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<RetrievedChunkResponse> relevant = new ArrayList<>();
        for (RetrievedChunkResponse chunk : chunks) {
            if (hasNgramOverlap(question, chunk.getContent())
                    || hasNgramOverlap(question, chunk.getDocumentName())) {
                relevant.add(chunk);
            }
        }
        if (relevant.isEmpty() && !chunks.isEmpty()) {
            RetrievedChunkResponse top = chunks.get(0);
            if (top.getScore() != null && top.getScore() >= 0.12) {
                relevant.add(top);
            }
        }
        return relevant;
    }

    public static boolean hasNgramOverlap(String question, String content) {
        String q = normalize(question);
        String c = normalize(content);
        if (q.isEmpty() || c.isEmpty()) {
            return false;
        }
        int required = q.length() > 10 ? 2 : 1;
        int hits = 0;
        for (int n = BIGRAM; n <= TRIGRAM; n++) {
            if (q.length() < n) {
                continue;
            }
            for (int i = 0; i <= q.length() - n; i++) {
                String gram = q.substring(i, i + n);
                if (gram.isBlank()) {
                    continue;
                }
                if (c.contains(gram)) {
                    hits++;
                    if (hits >= required) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        boolean lastSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (!lastSpace && !sb.isEmpty()) {
                    sb.append(' ');
                    lastSpace = true;
                }
            } else {
                if (ch < 128) {
                    sb.append(Character.toLowerCase(ch));
                } else {
                    sb.append(ch);
                }
                lastSpace = false;
            }
        }
        return sb.toString().trim();
    }
}
