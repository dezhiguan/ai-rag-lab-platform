package com.guan.rag.module.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.chat.entity.ChatMessage;
import com.guan.rag.module.chat.entity.ChatSession;
import com.guan.rag.module.chat.mapper.ChatMessageMapper;
import com.guan.rag.module.chat.mapper.ChatSessionMapper;
import com.guan.rag.module.chat.prompt.PromptBuilder;
import com.guan.rag.module.chat.provider.ChatModelProvider;
import com.guan.rag.module.chat.request.ChatRequest;
import com.guan.rag.module.chat.response.ChatMessageResponse;
import com.guan.rag.module.chat.response.ChatResponse;
import com.guan.rag.module.chat.response.ChatSessionResponse;
import com.guan.rag.module.chat.response.ChatSourceResponse;
import com.guan.rag.module.chat.support.ChatRelevanceFilter;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.retrieval.service.VectorRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final VectorRetrievalService vectorRetrievalService;
    private final PromptBuilder promptBuilder;
    private final ChatModelProvider chatModelProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        knowledgeBaseService.requireKb(request.getKbId());
        int topK = request.getTopK() == null ? 5 : request.getTopK();

        ChatSession session = resolveSession(request);
        saveMessage(session.getId(), request.getKbId(), ROLE_USER, request.getQuestion(), null, null, null, null);

        List<RetrievedChunkResponse> retrieved = vectorRetrievalService.retrieve(
                request.getKbId(), request.getQuestion(), topK);
        List<RetrievedChunkResponse> relevant = ChatRelevanceFilter.filter(request.getQuestion(), retrieved);
        List<ChatSourceResponse> sources = relevant.stream().map(this::toSource).toList();

        String context = buildContext(relevant);
        String prompt = promptBuilder.build(context, request.getQuestion());

        long start = System.currentTimeMillis();
        ChatModelProvider.ChatResult result = chatModelProvider.chat(prompt);
        long latency = System.currentTimeMillis() - start;
        if (result.latencyMs() > 0) {
            latency = result.latencyMs();
        }

        String sourcesJson = writeSourcesJson(sources);
        saveMessage(
                session.getId(),
                request.getKbId(),
                ROLE_ASSISTANT,
                result.answer(),
                sourcesJson,
                result.promptTokens(),
                result.completionTokens(),
                latency
        );

        session.setUpdatedAt(java.time.LocalDateTime.now());
        chatSessionMapper.updateById(session);

        return ChatResponse.builder()
                .sessionId(session.getId())
                .answer(result.answer())
                .sources(sources)
                .build();
    }

    public List<ChatSessionResponse> listSessions(Long kbId) {
        if (kbId != null) {
            knowledgeBaseService.requireKb(kbId);
        }
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .orderByDesc(ChatSession::getUpdatedAt);
        if (kbId != null) {
            wrapper.eq(ChatSession::getKbId, kbId);
        }
        return chatSessionMapper.selectList(wrapper).stream().map(this::toSessionResponse).toList();
    }

    public List<ChatMessageResponse> listMessages(Long sessionId) {
        ChatSession session = requireSession(sessionId);
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, session.getId())
                        .orderByAsc(ChatMessage::getCreatedAt)
        ).stream().map(this::toMessageResponse).toList();
    }

    public long countSessions() {
        return chatSessionMapper.selectCount(null);
    }

    public long countMessages() {
        return chatMessageMapper.selectCount(null);
    }

    private ChatSession resolveSession(ChatRequest request) {
        if (request.getSessionId() != null) {
            ChatSession session = chatSessionMapper.selectById(request.getSessionId());
            if (session == null) {
                throw new BusinessException("会话不存在");
            }
            if (!session.getKbId().equals(request.getKbId())) {
                throw new BusinessException("会话与知识库不匹配");
            }
            return session;
        }

        ChatSession session = new ChatSession();
        session.setKbId(request.getKbId());
        session.setTitle(truncateTitle(request.getQuestion()));
        chatSessionMapper.insert(session);
        return session;
    }

    private void saveMessage(
            Long sessionId,
            Long kbId,
            String role,
            String content,
            String sourceChunks,
            Integer promptTokens,
            Integer completionTokens,
            Long latencyMs
    ) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setKbId(kbId);
        message.setRole(role);
        message.setContent(content);
        message.setSourceChunks(sourceChunks);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setLatencyMs(latencyMs);
        chatMessageMapper.insert(message);
    }

    private String buildContext(List<RetrievedChunkResponse> retrieved) {
        if (retrieved.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < retrieved.size(); i++) {
            RetrievedChunkResponse item = retrieved.get(i);
            parts.add("【片段" + (i + 1) + "】文档：" + item.getDocumentName()
                    + "，Chunk #" + item.getChunkIndex()
                    + "\n" + item.getContent());
        }
        return String.join("\n\n", parts);
    }

    private ChatSourceResponse toSource(RetrievedChunkResponse item) {
        return ChatSourceResponse.builder()
                .documentId(item.getDocumentId())
                .documentName(item.getDocumentName())
                .chunkId(item.getChunkId())
                .chunkIndex(item.getChunkIndex())
                .score(item.getScore())
                .content(item.getContent())
                .build();
    }

    private String writeSourcesJson(List<ChatSourceResponse> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            throw new BusinessException("引用来源序列化失败");
        }
    }

    private List<ChatSourceResponse> readSourcesJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ChatSourceResponse>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private ChatSession requireSession(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .kbId(session.getKbId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .kbId(message.getKbId())
                .role(message.getRole())
                .content(message.getContent())
                .sources(readSourcesJson(message.getSourceChunks()))
                .promptTokens(message.getPromptTokens())
                .completionTokens(message.getCompletionTokens())
                .latencyMs(message.getLatencyMs())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String truncateTitle(String question) {
        if (question == null) {
            return "新会话";
        }
        String trimmed = question.trim();
        if (trimmed.length() <= 200) {
            return trimmed.isEmpty() ? "新会话" : trimmed;
        }
        return trimmed.substring(0, 200);
    }
}
