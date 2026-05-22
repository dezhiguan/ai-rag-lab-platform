package com.guan.rag.module.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import com.guan.rag.module.document.entity.Document;
import com.guan.rag.module.document.entity.DocumentChunk;
import com.guan.rag.module.document.mapper.DocumentChunkMapper;
import com.guan.rag.module.document.mapper.DocumentMapper;
import com.guan.rag.module.search.response.EsIndexRebuildResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsIndexService {

    private final ElasticsearchClient elasticsearchClient;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentMapper documentMapper;
    private final RagProperties ragProperties;

    public EsIndexRebuildResponse rebuildIndex() {
        String indexName = indexName();
        try {
            recreateIndex(indexName);
            List<DocumentChunk> chunks = documentChunkMapper.selectList(
                    new LambdaQueryWrapper<DocumentChunk>().orderByAsc(DocumentChunk::getId)
            );
            if (chunks.isEmpty()) {
                return EsIndexRebuildResponse.builder()
                        .indexName(indexName)
                        .syncedCount(0)
                        .build();
            }

            Map<Long, String> documentNames = loadDocumentNames(chunks);
            int synced = bulkIndexChunks(indexName, chunks, documentNames);
            log.info("ES index rebuild completed: index={}, synced={}", indexName, synced);
            return EsIndexRebuildResponse.builder()
                    .indexName(indexName)
                    .syncedCount(synced)
                    .build();
        } catch (IOException e) {
            throw new BusinessException("Elasticsearch 索引重建失败: " + e.getMessage());
        }
    }

    public String indexName() {
        return ragProperties.getElasticsearch().getIndex();
    }

    private void recreateIndex(String indexName) throws IOException {
        boolean exists = elasticsearchClient.indices()
                .exists(ExistsRequest.of(e -> e.index(indexName)))
                .value();
        if (exists) {
            elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
        }

        CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(TypeMapping.of(m -> m
                        .properties("kbId", Property.of(p -> p.long_(l -> l)))
                        .properties("documentId", Property.of(p -> p.long_(l -> l)))
                        .properties("documentName", Property.of(p -> p.keyword(k -> k)))
                        .properties("chunkId", Property.of(p -> p.long_(l -> l)))
                        .properties("chunkIndex", Property.of(p -> p.integer(i -> i)))
                        .properties("content", Property.of(p -> p.text(TextProperty.of(t -> t
                                .analyzer("standard")
                        ))))
                ))
        );
        elasticsearchClient.indices().create(createRequest);
    }

    private Map<Long, String> loadDocumentNames(List<DocumentChunk> chunks) {
        List<Long> documentIds = chunks.stream()
                .map(DocumentChunk::getDocumentId)
                .distinct()
                .toList();
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        return documentMapper.selectBatchIds(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Document::getFileName, (a, b) -> a));
    }

    private int bulkIndexChunks(
            String indexName,
            List<DocumentChunk> chunks,
            Map<Long, String> documentNames
    ) throws IOException {
        List<BulkOperation> operations = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("kbId", chunk.getKbId());
            doc.put("documentId", chunk.getDocumentId());
            doc.put("documentName", documentNames.getOrDefault(chunk.getDocumentId(), ""));
            doc.put("chunkId", chunk.getId());
            doc.put("chunkIndex", chunk.getChunkIndex());
            doc.put("content", chunk.getContent());

            operations.add(BulkOperation.of(op -> op.index(IndexOperation.of(idx -> idx
                    .index(indexName)
                    .id(String.valueOf(chunk.getId()))
                    .document(doc)
            ))));
        }

        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        var response = elasticsearchClient.bulk(bulkRequest);
        if (response.errors()) {
            throw new BusinessException("Elasticsearch 批量写入存在错误，请检查 ES 日志");
        }
        return chunks.size();
    }
}
