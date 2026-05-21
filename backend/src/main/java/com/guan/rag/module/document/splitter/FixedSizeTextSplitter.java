package com.guan.rag.module.document.splitter;

import com.guan.rag.common.util.HashUtils;
import com.guan.rag.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeTextSplitter implements DocumentSplitter {

    private final int chunkSize;
    private final int overlap;

    public FixedSizeTextSplitter(RagProperties ragProperties) {
        this.chunkSize = ragProperties.getDocument().getChunkSize();
        this.overlap = ragProperties.getDocument().getChunkOverlap();
    }

    @Override
    public List<SplitChunk> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.trim();
        List<SplitChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        int step = Math.max(1, chunkSize - overlap);

        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            String content = normalized.substring(start, end);
            int tokenCount = content.length();
            chunks.add(new SplitChunk(index, content, tokenCount, HashUtils.sha256(content)));
            index++;
            if (end >= normalized.length()) {
                break;
            }
            start += step;
        }
        return chunks;
    }
}
