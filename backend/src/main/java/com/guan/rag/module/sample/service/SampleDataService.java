package com.guan.rag.module.sample.service;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.document.service.DocumentService;
import com.guan.rag.module.kb.entity.KnowledgeBase;
import com.guan.rag.module.kb.request.KnowledgeBaseCreateRequest;
import com.guan.rag.module.kb.response.KnowledgeBaseResponse;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.sample.response.SampleInitResponse;
import com.guan.rag.module.sample.response.SampleStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class SampleDataService {

    private static final String SAMPLE_DATA_PATTERN = "classpath:sample-data/*.md";

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;

    public boolean isInitialized() {
        KnowledgeBase kb = knowledgeBaseService.findByName(KnowledgeBaseService.DEFAULT_SAMPLE_KB_NAME);
        return kb != null && documentService.countByKbId(kb.getId()) >= 3;
    }

    public SampleStatusResponse status() {
        KnowledgeBase kb = knowledgeBaseService.findByName(KnowledgeBaseService.DEFAULT_SAMPLE_KB_NAME);
        boolean initialized = kb != null && documentService.countByKbId(kb.getId()) >= 3;
        return SampleStatusResponse.builder()
                .initialized(initialized)
                .knowledgeBaseId(kb != null ? kb.getId() : null)
                .documentCount(kb != null ? documentService.countByKbId(kb.getId()) : 0)
                .build();
    }

    @Transactional
    public SampleInitResponse init() throws IOException {
        if (isInitialized()) {
            KnowledgeBase kb = knowledgeBaseService.findByName(KnowledgeBaseService.DEFAULT_SAMPLE_KB_NAME);
            return SampleInitResponse.builder()
                    .initialized(true)
                    .knowledgeBaseId(kb.getId())
                    .documentCount(documentService.countByKbId(kb.getId()))
                    .message("样例数据已初始化")
                    .build();
        }

        KnowledgeBase kbEntity = knowledgeBaseService.findByName(KnowledgeBaseService.DEFAULT_SAMPLE_KB_NAME);
        Long kbId;
        if (kbEntity == null) {
            KnowledgeBaseCreateRequest createRequest = new KnowledgeBaseCreateRequest();
            createRequest.setName(KnowledgeBaseService.DEFAULT_SAMPLE_KB_NAME);
            createRequest.setDescription("V1 默认样例知识库，包含项目规范、API 说明与排查文档");
            KnowledgeBaseResponse kb = knowledgeBaseService.create(createRequest);
            kbId = kb.getId();
        } else {
            kbId = kbEntity.getId();
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(SAMPLE_DATA_PATTERN);
        Arrays.sort(resources, Comparator.comparing(r -> {
            try {
                return r.getFilename();
            } catch (Exception e) {
                return "";
            }
        }));

        if (resources.length == 0) {
            throw new BusinessException("未找到样例数据文件");
        }

        int imported = 0;
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null) {
                continue;
            }
            if (documentService.existsByKbIdAndFileName(kbId, fileName)) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                byte[] content = in.readAllBytes();
                documentService.importFromClasspath(kbId, fileName, content);
                imported++;
            }
        }

        return SampleInitResponse.builder()
                .initialized(true)
                .knowledgeBaseId(kbId)
                .documentCount(imported)
                .message("样例数据初始化成功")
                .build();
    }
}
