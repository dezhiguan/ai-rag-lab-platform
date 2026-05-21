package com.guan.rag.module.chat.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long sessionId;
    private Long kbId;
    private String role;
    private String content;
    private List<ChatSourceResponse> sources;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long latencyMs;
    private LocalDateTime createdAt;
}
