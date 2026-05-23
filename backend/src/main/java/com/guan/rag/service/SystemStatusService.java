package com.guan.rag.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.guan.rag.config.RagProperties;
import com.guan.rag.controller.response.SystemStatusResponse;
import com.guan.rag.module.search.service.EsIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemStatusService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String FEATURE_SUPPORTED = "SUPPORTED";

    private final DataSource dataSource;
    private final RagProperties ragProperties;
    private final ElasticsearchClient elasticsearchClient;
    private final EsIndexService esIndexService;

    public SystemStatusResponse getStatus() {
        return SystemStatusResponse.builder()
                .backend(buildBackend())
                .postgresql(checkPostgresql())
                .elasticsearch(checkElasticsearch())
                .modelProvider(buildModelProvider())
                .features(builtinFeatures())
                .build();
    }

    private SystemStatusResponse.BackendStatusResponse buildBackend() {
        RagProperties.App app = ragProperties.getApp();
        return SystemStatusResponse.BackendStatusResponse.builder()
                .status(STATUS_UP)
                .appName(app.getName())
                .version(app.getVersion())
                .serverTime(Instant.now().toString())
                .message("后端服务运行中")
                .build();
    }

    private SystemStatusResponse.DependencyStatusResponse checkPostgresql() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return SystemStatusResponse.DependencyStatusResponse.builder()
                        .name("PostgreSQL")
                        .status(STATUS_UP)
                        .message("连接正常")
                        .build();
            }
            return downDependency("PostgreSQL", "连接测试未返回结果");
        } catch (Exception ex) {
            log.warn("PostgreSQL health check failed: {}", ex.getMessage());
            return downDependency("PostgreSQL", ex.getMessage());
        }
    }

    private SystemStatusResponse.ElasticsearchStatusResponse checkElasticsearch() {
        String indexName = esIndexService.indexName();
        try {
            boolean reachable = elasticsearchClient.ping().value();
            if (reachable) {
                return SystemStatusResponse.ElasticsearchStatusResponse.builder()
                        .status(STATUS_UP)
                        .indexName(indexName)
                        .message("连接正常")
                        .build();
            }
            return downElasticsearch(indexName, "Ping 未成功");
        } catch (Exception ex) {
            log.warn("Elasticsearch health check failed: {}", ex.getMessage());
            return downElasticsearch(indexName, ex.getMessage());
        }
    }

    private SystemStatusResponse.ModelProviderStatusResponse buildModelProvider() {
        RagProperties.Embedding embedding = ragProperties.getEmbedding();
        RagProperties.Chat chat = ragProperties.getChat();
        return SystemStatusResponse.ModelProviderStatusResponse.builder()
                .embeddingProvider(embedding.getProvider())
                .embeddingModel(embedding.getModel())
                .chatProvider(chat.getProvider())
                .chatModel(chat.getModel())
                .build();
    }

    private static List<SystemStatusResponse.FeatureStatusResponse> builtinFeatures() {
        return List.of(
                feature("document-import", "文档导入"),
                feature("vector-retrieval", "向量检索"),
                feature("bm25", "BM25"),
                feature("hybrid", "Hybrid"),
                feature("reranker", "Reranker"),
                feature("evaluation", "Evaluation")
        );
    }

    private static SystemStatusResponse.FeatureStatusResponse feature(String key, String label) {
        return SystemStatusResponse.FeatureStatusResponse.builder()
                .key(key)
                .label(label)
                .status(FEATURE_SUPPORTED)
                .build();
    }

    private static SystemStatusResponse.DependencyStatusResponse downDependency(String name, String message) {
        return SystemStatusResponse.DependencyStatusResponse.builder()
                .name(name)
                .status(STATUS_DOWN)
                .message(message)
                .build();
    }

    private static SystemStatusResponse.ElasticsearchStatusResponse downElasticsearch(
            String indexName,
            String message
    ) {
        return SystemStatusResponse.ElasticsearchStatusResponse.builder()
                .status(STATUS_DOWN)
                .indexName(indexName)
                .message(message)
                .build();
    }
}
