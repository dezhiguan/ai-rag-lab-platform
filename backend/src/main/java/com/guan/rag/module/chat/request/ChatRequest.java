package com.guan.rag.module.chat.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotNull(message = "kbId 不能为空")
    private Long kbId;

    private Long sessionId;

    @NotBlank(message = "question 不能为空")
    private String question;

    private Integer topK = 5;
}
