package com.guan.rag.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.guan.rag.config.RagProperties;
import com.guan.rag.controller.response.SystemStatusResponse;
import com.guan.rag.module.search.service.EsIndexService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        int timeoutMs = ragProperties.getSystemStatus().getHealthCheckTimeoutMs();
        String indexName = esIndexService.indexName();
        long deadlineMs = timeoutMs + 1000L;

        CompletableFuture<SystemStatusResponse.DependencyStatusResponse> postgresqlFuture =
                CompletableFuture.supplyAsync(this::checkPostgresql);
        CompletableFuture<SystemStatusResponse.ElasticsearchStatusResponse> elasticsearchFuture =
                CompletableFuture.supplyAsync(this::checkElasticsearch);

        SystemStatusResponse.DependencyStatusResponse postgresql =
                awaitDependency(postgresqlFuture, deadlineMs, "PostgreSQL", timeoutMs);
        SystemStatusResponse.ElasticsearchStatusResponse elasticsearch =
                awaitElasticsearch(elasticsearchFuture, deadlineMs, indexName, timeoutMs);

        return SystemStatusResponse.builder()
                .backend(buildBackend())
                .postgresql(postgresql)
                .elasticsearch(elasticsearch)
                .modelProvider(buildModelProvider())
                .features(builtinFeatures())
                .build();
    }

    private SystemStatusResponse.DependencyStatusResponse awaitDependency(
            CompletableFuture<SystemStatusResponse.DependencyStatusResponse> future,
            long deadlineMs,
            String name,
            int timeoutMs
    ) {
        try {
            return future.get(deadlineMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            log.warn("{} health check timed out after {}ms", name, deadlineMs);
            return downDependency(name, timeoutMessage(timeoutMs));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return downDependency(name, "探活被中断");
        } catch (ExecutionException ex) {
            log.warn("{} health check failed: {}", name, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            return downDependency(name, resolveMessage(ex.getCause()));
        }
    }

    private SystemStatusResponse.ElasticsearchStatusResponse awaitElasticsearch(
            CompletableFuture<SystemStatusResponse.ElasticsearchStatusResponse> future,
            long deadlineMs,
            String indexName,
            int timeoutMs
    ) {
        try {
            return future.get(deadlineMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            log.warn("Elasticsearch health check timed out after {}ms", deadlineMs);
            return downElasticsearch(indexName, timeoutMessage(timeoutMs));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return downElasticsearch(indexName, "探活被中断");
        } catch (ExecutionException ex) {
            log.warn("Elasticsearch health check failed: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            return downElasticsearch(indexName, resolveMessage(ex.getCause()));
        }
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
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            return checkPostgresqlDirect(hikariDataSource);
        }
        return checkPostgresqlViaPool();
    }

    private SystemStatusResponse.DependencyStatusResponse checkPostgresqlDirect(HikariDataSource hikariDataSource) {
        int timeoutSeconds = Math.max(1, ragProperties.getSystemStatus().getHealthCheckTimeoutMs() / 1000);
        String jdbcUrl = withPostgresqlDriverTimeouts(hikariDataSource.getJdbcUrl(), timeoutSeconds);
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl,
                hikariDataSource.getUsername(),
                hikariDataSource.getPassword());
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
        } catch (SQLException ex) {
            log.warn("PostgreSQL health check failed: {}", ex.getMessage());
            return downDependency("PostgreSQL", resolvePostgresqlMessage(ex, timeoutSeconds));
        }
    }

    private SystemStatusResponse.DependencyStatusResponse checkPostgresqlViaPool() {
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

    private static String withPostgresqlDriverTimeouts(String jdbcUrl, int timeoutSeconds) {
        if (jdbcUrl.contains("connectTimeout=")) {
            return jdbcUrl;
        }
        String params = "connectTimeout=" + timeoutSeconds + "&socketTimeout=" + timeoutSeconds;
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + params;
    }

    private static String resolvePostgresqlMessage(SQLException ex, int timeoutSeconds) {
        String message = ex.getMessage();
        if (message != null && message.contains("timed out")) {
            return "连接超时（" + timeoutSeconds + "s）";
        }
        return message != null ? message : "连接失败";
    }

    private static String timeoutMessage(int timeoutMs) {
        return "连接超时（" + timeoutMs + "ms）";
    }

    private static String resolveMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return "连接失败";
        }
        return throwable.getMessage();
    }
}
