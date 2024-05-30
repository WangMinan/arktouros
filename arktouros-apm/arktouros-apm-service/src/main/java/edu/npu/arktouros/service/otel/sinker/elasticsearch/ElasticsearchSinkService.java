package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.RolloverRequest;
import co.elastic.clients.elasticsearch.indices.RolloverResponse;
import co.elastic.clients.elasticsearch.indices.rollover.RolloverConditions;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.ObjectBuilder;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.exception.ArktourosException;
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
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * @author : [wangminan]
 * @description : Elasticsearch数据持久化
 */
@Slf4j
public class ElasticsearchSinkService extends SinkService {

    private final ExecutorService createIndexThreadPool =
            Executors.newFixedThreadPool(ElasticsearchIndex.getIndexList().size(),
                    new BasicThreadFactory.Builder()
                            .namingPattern("ElasticSearch-init-%d").build());

    @Override
    public void init() {
        log.info("Check and init mappings in elasticsearch.");
        CountDownLatch createIndexLatch = new CountDownLatch(
                ElasticsearchIndex.getIndexList().size());
        // 先判断各索引是否存在 不存在则创建 并行操作
        List<Thread> initThreads = new ArrayList<>();
        ElasticsearchIndex.getIndexList().forEach(indexName -> {
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
            Thread.currentThread().interrupt();
            throw new ArktourosException(e, "Create index interrupted.");
        }
        this.setReady(true);
        // 关闭init线程池
        createIndexThreadPool.shutdown();
        log.info("ElasticSearch sinker init success.");
    }

    public void checkAndCreate(String indexName, CountDownLatch createIndexLatch) throws IOException {
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        BooleanResponse exists =
                esClient.indices().exists(builder -> builder.index(indexName));
        ElasticsearchClientPool.returnClient(esClient);
        // spring-retry
        if (!exists.value()) {
            // 创建索引
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
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        CreateIndexResponse createIndexResponse = esClient.indices().create(createIndexRequest);
        ElasticsearchClientPool.returnClient(esClient);
        if (!createIndexResponse.acknowledged()) {
            throw new IOException("Create index failed: " + indexName);
        }
        log.info("Create index success: {}", indexName);
    }

    private static CreateIndexRequest.Builder getCreateIndexRequestBuilder(String indexName) {
        CreateIndexRequest.Builder createIndexRequestBuilder =
                new CreateIndexRequest.Builder();
        // <my-index-{now/d}-000001>
        createIndexRequestBuilder.index("<" + indexName+ "-{now/d}-000001>")
                // 不区分 读写都从indexName这个虚拟索引做
                .aliases(indexName, new Alias.Builder().isWriteIndex(true).build());
        createIndexRequestBuilder.mappings(getMappings(indexName));
        return createIndexRequestBuilder;
    }

    private static TypeMapping getMappings(String indexName) {
        TypeMapping.Builder typeMappingBuilder = new TypeMapping.Builder();
        if (ElasticsearchIndex.SERVICE_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Service.documentMap);
        } else if (ElasticsearchIndex.LOG_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Log.documentMap);
        } else if (ElasticsearchIndex.SPAN_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Span.documentMap);
        } else if (ElasticsearchIndex.GAUGE_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Gauge.documentMap);
        } else if (ElasticsearchIndex.COUNTER_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Counter.documentMap);
        } else if (ElasticsearchIndex.SUMMARY_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Summary.documentMap);
        } else if (ElasticsearchIndex.HISTOGRAM_INDEX.getIndexName().equals(indexName)) {
            typeMappingBuilder.properties(Histogram.documentMap);
        } else {
            throw new IllegalArgumentException("Unexpected index name: " + indexName);
        }
        return typeMappingBuilder.build();
    }

    @Override
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sink(Source source) throws IOException {
        log.debug("Sink source:{}", source.toString());
        if (source instanceof Metric sourceMetric) {
            Service service =
                    Service.builder().name(sourceMetric.getServiceName()).build();
            ElasticsearchUtil.sink(service.getId(),
                    ElasticsearchIndex.SERVICE_INDEX.getIndexName(), service);
            switch (sourceMetric.getMetricType()) {
                case COUNTER:
                    try {
                        ElasticsearchUtil.sink(
                                ElasticsearchIndex.COUNTER_INDEX.getIndexName(), sourceMetric);
                    } catch (IOException e) {
                        log.error("Sink counter error.", e);
                        throw e;
                    }
                    break;
                case GAUGE:
                    try {
                        ElasticsearchUtil.sink(
                                ElasticsearchIndex.GAUGE_INDEX.getIndexName(), sourceMetric);
                        log.info("Sink gauge to elasticsearch success.");
                    } catch (IOException e) {
                        log.error("Sink gauge error.", e);
                        throw e;
                    }
                    break;
                case SUMMARY:
                    try {
                        ElasticsearchUtil.sink(
                                ElasticsearchIndex.SUMMARY_INDEX.getIndexName(), sourceMetric);
                        log.info("Sink summary to elasticsearch success.");
                    } catch (IOException e) {
                        log.error("Sink summary error.", e);
                        throw e;
                    }
                    break;
                case HISTOGRAM:
                    try {
                        ElasticsearchUtil.sink(
                                ElasticsearchIndex.HISTOGRAM_INDEX.getIndexName(), sourceMetric);
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
                        ElasticsearchUtil.sink(service.getId(),
                                ElasticsearchIndex.SERVICE_INDEX.getIndexName(), service);
                        ElasticsearchUtil.sink(
                                ElasticsearchIndex.LOG_INDEX.getIndexName(), sourceLog);
                        log.info("Sink log to elasticsearch success.");
                    } catch (IOException e) {
                        log.error("Sink log error.", e);
                        throw e;
                    }
                    break;
                case Span sourceSpan:
                    try {
                        Service service = Service.builder().name(sourceSpan.getServiceName()).build();
                        ElasticsearchUtil.sink(service.getId(),
                                ElasticsearchIndex.SERVICE_INDEX.getIndexName(), service);
                        ElasticsearchUtil.sink(sourceSpan.getId(),
                                ElasticsearchIndex.SPAN_INDEX.getIndexName(), sourceSpan);
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

    // 一个小时执行一次
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void tryRollover() {
        for (String indexName : ElasticsearchIndex.getIndexList()) {
            try {
                handleRollOver(indexName);
            } catch (IOException e) {
                // 不抛异常 大不了我们就不分嘛
                log.error("Rollover index:{} error.", indexName, e);
            }
        }
    }

    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 1000))
    public void handleRollOver(String indexName) throws IOException {
        RolloverRequest rolloverRequest = new RolloverRequest.Builder()
                .alias(indexName)
                .conditions(new RolloverConditions.Builder()
                        // 单页大小达到5gb时翻页
                        .maxSize(PropertiesProvider.getProperty(
                                "elasticsearch.rollover.maxSize", "5gb"))
                        .maxAge(new Time.Builder()
                                .time(PropertiesProvider.getProperty(
                                "elasticsearch.rollover.maxAge", "1d"))
                                .build())
                        .maxDocs(Long.parseLong(PropertiesProvider.getProperty(
                                "elasticsearch.rollover.maxDocs", "100000")))
                        .build())
                // 要重新制定mappings 不然会出问题
                .mappings(getMappings(indexName))
                .build();
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        RolloverResponse rolloverResponse = esClient.indices()
                .rollover(rolloverRequest);
        ElasticsearchClientPool.returnClient(esClient);
        if (!rolloverResponse.acknowledged()) {
            log.info("Check rollover for index:{}, nothing happened.", indexName);
        } else {
            log.info("Check rollover for index:{}, rollover complete, old index:{}, new index:{}.",
                    indexName, rolloverResponse.oldIndex(), rolloverResponse.newIndex());
        }
    }
}
