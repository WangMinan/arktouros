package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : ElasticSearch数据持久化
 */
@Slf4j
public class ElasticSearchSinkService extends SinkService {
    private final ElasticsearchClient esClient;

    public ElasticSearchSinkService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    private static final String LOG_INDEX = "arktouros-log";
    private static final String SPAN_INDEX = "arktouros-span";
    private static final String GAUGE_INDEX = "arktouros-gauge";
    private static final String COUNTER_INDEX = "arktouros-counter";
    private static final String SUMMARY_INDEX = "arktouros-summary";
    private static final String HISTOGRAM_INDEX = "arktouros-histogram";

    private static final List<String> indexList = List.of(LOG_INDEX, SPAN_INDEX, GAUGE_INDEX,
            COUNTER_INDEX, SUMMARY_INDEX, HISTOGRAM_INDEX);

    @Override
    public void init() {
        log.info("Check and init mappings in elasticsearch.");
        // 先判断各索引是否存在
        indexList.forEach(this::checkAndCreate);
        this.setReady(true);
    }

    private void checkAndCreate(String indexName) {
        try {
            BooleanResponse exists =
                    esClient.indices().exists(builder -> builder.index(indexName));
            if (!exists.value()) {
                // spring-retry
                createIndex(indexName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用elasticsearch-java 创建index
     * <a href="https://juejin.cn/post/7097068865854636069">参考https://juejin.cn/post/7097068865854636069</a>
     *
     * @param indexName 索引名称
     * @throws IOException IO异常
     */
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void createIndex(String indexName) throws IOException {
        log.info("Index not exist, start creating index: {}", indexName);
        URL resourceUrl = ElasticSearchSinkService.class.getResource("/mapping/arktouros-es-mapping/" + indexName + ".json");
        if (resourceUrl == null) {
            throw new FileNotFoundException("Mapping file not found: " + indexName);
        }
        CreateIndexRequest.Builder createIndexRequestBuilder =
                getCreateIndexRequestBuilder(indexName);
        CreateIndexRequest createIndexRequest = createIndexRequestBuilder.build();
        CreateIndexResponse createIndexResponse = esClient.indices().create(createIndexRequest);
        if (!createIndexResponse.acknowledged()) {
            throw new IOException("Create index failed: " + indexName);
        }
        log.info("Create index success: {}", indexName);
    }

    private static CreateIndexRequest.Builder getCreateIndexRequestBuilder(String indexName) {
        CreateIndexRequest.Builder createIndexRequestBuilder = new CreateIndexRequest.Builder();
        createIndexRequestBuilder.index(indexName);
        if (LOG_INDEX.equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Log.documentMap));
        } else if (SPAN_INDEX.equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Span.documentMap));
        } else if (GAUGE_INDEX.equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Gauge.documentMap));
        } else if (COUNTER_INDEX.equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Counter.documentMap));
        } else if (SUMMARY_INDEX.equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Summary.documentMap));
        } else if (HISTOGRAM_INDEX.equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Histogram.documentMap));
        }
        return createIndexRequestBuilder;
    }

    @Override
    public void saveResourceLogs(ResourceLogs resourceLogs) {

    }

    @Override
    public void saveResourceSpans(ResourceSpans resourceSpans) {

    }

    @Override
    public void saveResourceMetrics(ResourceMetrics resourceMetrics) {

    }
}
