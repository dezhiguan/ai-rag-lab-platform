package com.guan.rag.module.chat.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {

    private Long sessionId;
    private String answer;
    private List<ChatSourceResponse> sources;
}
