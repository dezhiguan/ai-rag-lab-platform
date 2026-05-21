package com.guan.rag.module.chat.controller;

import com.guan.rag.common.ApiResponse;
import com.guan.rag.module.chat.request.ChatRequest;
import com.guan.rag.module.chat.response.ChatMessageResponse;
import com.guan.rag.module.chat.response.ChatResponse;
import com.guan.rag.module.chat.response.ChatSessionResponse;
import com.guan.rag.module.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "问答", description = "Naive RAG Chat 问答")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送问题并获取回答")
    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(request));
    }

    @Operation(summary = "查询会话列表")
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponse>> listSessions(@RequestParam(required = false) Long kbId) {
        return ApiResponse.success(chatService.listSessions(kbId));
    }

    @Operation(summary = "查询会话消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.success(chatService.listMessages(sessionId));
    }
}
