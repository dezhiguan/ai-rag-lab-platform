package com.guan.rag.module.document.splitter;

import java.util.List;

public interface DocumentSplitter {

    List<SplitChunk> split(String text);

    record SplitChunk(int chunkIndex, String content, int tokenCount, String contentHash) {
    }
}
