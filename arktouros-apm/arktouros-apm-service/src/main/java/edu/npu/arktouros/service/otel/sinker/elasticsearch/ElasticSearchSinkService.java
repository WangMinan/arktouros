package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final ExecutorService createIndexThreadPool =
            Executors.newFixedThreadPool(indexList.size());

    @Override
    public void init() {
        log.info("Check and init mappings in elasticsearch.");
        CountDownLatch createIndexLatch = new CountDownLatch(indexList.size());
        // 先判断各索引是否存在 不存在则创建 并行操作
        List<Thread> initThreads = new ArrayList<>();
        indexList.forEach(indexName -> {
            Thread thread = new Thread(() -> {
                try {
                    checkAndCreate(indexName, createIndexLatch);
                } catch (Exception e) {
                    log.error("Check and create index" + indexName +" error.", e);
                    System.exit(1);
                }
            });
            thread.setName("Check and create index: " + indexName);

            initThreads.add(thread);
        });
        initThreads.forEach(createIndexThreadPool::submit);
        try {
            createIndexLatch.await();
        } catch (InterruptedException e) {
            log.error("Create index interrupted.");
            throw new RuntimeException(e);
        }
        this.setReady(true);
        log.info("ElasticSearch sinker init success.");
    }

    public void checkAndCreate(String indexName, CountDownLatch createIndexLatch) throws IOException {
        BooleanResponse exists =
                esClient.indices().exists(builder -> builder.index(indexName));
        if (!exists.value()) {
            // spring-retry
            createIndex(indexName);
        }
        createIndexLatch.countDown();
    }

    /**
     * 使用elasticsearch-java 创建index
     * <a href="https://juejin.cn/post/7097068865854636069">参考https://juejin.cn/post/7097068865854636069</a>
     *
     * @param indexName 索引名称
     * @throws IOException IO异常
     */
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void createIndex(String indexName) throws IOException {
        log.info("Index not exist, start creating index: {}", indexName);
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
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sink(Source source) throws IOException {
        switch (source) {
            // 模式匹配
            case Log sourceLog:
                try {
                    esClient.index(builder -> builder
                            .index(LOG_INDEX)
                            .document(sourceLog)
                    );
                    log.info("Sink log to elasticsearch success.");
                } catch (IOException e) {
                    log.error("Sink log error.", e);
                    throw e;
                }
                break;
            case Span sourceSpan:
                try {
                    esClient.index(builder -> builder
                            .index(SPAN_INDEX)
                            .document(sourceSpan)
                    );
                    log.info("Sink span to elasticsearch success.");
                } catch (IOException e) {
                    log.error("Sink span error.", e);
                    throw e;
                }
                break;
            case Metric sourceMetric:
                switch (sourceMetric.getMetricType()) {
                    case COUNTER:
                        try {
                            esClient.index(builder -> builder
                                    .index(COUNTER_INDEX)
                                    .document(sourceMetric)
                            );
                        } catch (IOException e) {
                            log.error("Sink counter error.", e);
                            throw e;
                        }
                        break;
                    case GAUGE:
                        try {
                            esClient.index(builder -> builder
                                    .index(GAUGE_INDEX)
                                    .document(sourceMetric)
                            );
                            log.info("Sink gauge to elasticsearch success.");
                        } catch (IOException e) {
                            log.error("Sink gauge error.", e);
                            throw e;
                        }
                        break;
                    case SUMMARY:
                        try {
                            esClient.index(builder -> builder
                                    .index(SUMMARY_INDEX)
                                    .document(sourceMetric)
                            );
                            log.info("Sink summary to elasticsearch success.");
                        } catch (IOException e) {
                            log.error("Sink summary error.", e);
                            throw e;
                        }
                        break;
                    case HISTOGRAM:
                        try {
                            esClient.index(builder -> builder
                                    .index(HISTOGRAM_INDEX)
                                    .document(sourceMetric)
                            );
                            log.info("Sink histogram to elasticsearch success.");
                        } catch (IOException e) {
                            log.error("Sink histogram error.", e);
                            throw e;
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected metric type value: " + sourceMetric.getMetricType());
                }
            default:
                throw new IllegalStateException("Unexpected source type value: " + source);
        }
    }
}
