package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import edu.npu.arktouros.model.common.ElasticSearchIndex;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private final ExecutorService createIndexThreadPool =
            new ThreadPoolExecutor(
                    ElasticSearchIndex.getIndexList().size(),
                    ElasticSearchIndex.getIndexList().size(),
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(ElasticSearchIndex.getIndexList().size()),
                    new BasicThreadFactory.Builder()
                            .namingPattern("ElasticSearch-init-%d").build(),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    @Override
    public void init() {
        log.info("Check and init mappings in elasticsearch.");
        CountDownLatch createIndexLatch = new CountDownLatch(ElasticSearchIndex.getIndexList().size());
        // 先判断各索引是否存在 不存在则创建 并行操作
        List<Thread> initThreads = new ArrayList<>();
        ElasticSearchIndex.getIndexList().forEach(indexName -> {
            Thread thread = new Thread(() -> {
                try {
                    checkAndCreate(indexName, createIndexLatch);
                } catch (Exception e) {
                    log.error("Check and create index:{} error.", indexName, e);
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
        // 关闭init线程池
        createIndexThreadPool.shutdown();
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
        if (ElasticSearchIndex.SERVICE_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Service.documentMap));
        } else if (ElasticSearchIndex.LOG_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Log.documentMap));
        } else if (ElasticSearchIndex.SPAN_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Span.documentMap));
        } else if (ElasticSearchIndex.GAUGE_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Gauge.documentMap));
        } else if (ElasticSearchIndex.COUNTER_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Counter.documentMap));
        } else if (ElasticSearchIndex.SUMMARY_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Summary.documentMap));
        } else if (ElasticSearchIndex.HISTOGRAM_INDEX.getIndexName().equals(indexName)) {
            createIndexRequestBuilder.mappings(typeMappingBuilder ->
                    typeMappingBuilder.properties(Histogram.documentMap));
        }
        return createIndexRequestBuilder;
    }

    @Override
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sink(Source source) throws IOException {
        if (source instanceof Metric sourceMetric) {
            Service service =
                    Service.builder().name(sourceMetric.getServiceName()).build();
            esClient.index(builder -> builder
                    .id(service.getId())
                    .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                    .document(service)
            );
            switch (sourceMetric.getMetricType()) {
                case COUNTER:
                    try {
                        esClient.index(builder -> builder
                                .index(ElasticSearchIndex.COUNTER_INDEX.getIndexName())
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
                                .index(ElasticSearchIndex.GAUGE_INDEX.getIndexName())
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
                                .index(ElasticSearchIndex.SUMMARY_INDEX.getIndexName())
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
                                .index(ElasticSearchIndex.HISTOGRAM_INDEX.getIndexName())
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
        } else {
            switch (source) {
                // 模式匹配
                case Log sourceLog:
                    try {
                        Service service = Service.builder().name(sourceLog.getServiceName()).build();
                        esClient.index(builder -> builder
                                .id(service.getId())
                                .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                                .document(service)
                        );
                        esClient.index(builder -> builder
                                .index(ElasticSearchIndex.LOG_INDEX.getIndexName())
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
                        Service service = Service.builder().name(sourceSpan.getServiceName()).build();
                        esClient.index(builder -> builder
                                .id(service.getId())
                                .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                                .document(service)
                        );
                        esClient.index(builder -> builder
                                .index(ElasticSearchIndex.SPAN_INDEX.getIndexName())
                                .id(sourceSpan.getId())
                                .document(sourceSpan)
                        );
                        log.info("Sink span to elasticsearch success.");
                    } catch (IOException e) {
                        log.error("Sink span error.", e);
                        throw e;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected source type value: " + source);
            }
        }
    }
}
