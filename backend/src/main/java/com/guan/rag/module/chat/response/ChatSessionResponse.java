package com.guan.rag.module.chat.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionResponse {

    private Long id;
    private Long kbId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
