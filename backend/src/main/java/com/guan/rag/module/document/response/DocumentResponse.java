package com.guan.rag.module.document.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {

    private Long id;
    private Long kbId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String contentHash;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
