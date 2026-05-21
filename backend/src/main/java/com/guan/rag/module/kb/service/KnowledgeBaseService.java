package com.guan.rag.module.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.document.entity.Document;
import com.guan.rag.module.document.entity.DocumentChunk;
import com.guan.rag.module.document.mapper.DocumentChunkMapper;
import com.guan.rag.module.document.mapper.DocumentMapper;
import com.guan.rag.module.kb.entity.KnowledgeBase;
import com.guan.rag.module.kb.mapper.KnowledgeBaseMapper;
import com.guan.rag.module.kb.request.KnowledgeBaseCreateRequest;
import com.guan.rag.module.kb.response.KnowledgeBaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String DEFAULT_SAMPLE_KB_NAME = "默认样例知识库";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;

    @Transactional
    public KnowledgeBaseResponse create(KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setStatus(STATUS_ACTIVE);
        knowledgeBaseMapper.insert(kb);
        return toResponse(kb);
    }

    public List<KnowledgeBaseResponse> list() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .orderByDesc(KnowledgeBase::getCreatedAt)
        ).stream().map(this::toResponse).toList();
    }

    public KnowledgeBaseResponse getById(Long id) {
        KnowledgeBase kb = requireKb(id);
        return toResponse(kb);
    }

    @Transactional
    public void delete(Long id) {
        requireKb(id);
        knowledgeBaseMapper.deleteById(id);
        documentMapper.delete(new LambdaQueryWrapper<Document>().eq(Document::getKbId, id));
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKbId, id));
    }

    public KnowledgeBase requireKb(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        return kb;
    }

    public long count() {
        return knowledgeBaseMapper.selectCount(null);
    }

    public KnowledgeBase findByName(String name) {
        return knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getName, name)
        );
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .status(kb.getStatus())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}
