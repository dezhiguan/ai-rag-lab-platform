package com.guan.rag.module.document.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentChunkResponse {

    private Long id;
    private Long kbId;
    private Long documentId;
    private Integer chunkIndex;
    private String titlePath;
    private String content;
    private Integer tokenCount;
    private String contentHash;
    private LocalDateTime createdAt;
}
