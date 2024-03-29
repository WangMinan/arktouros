package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : ElasticSearch数据持久化
 */
@Slf4j
public class ElasticSearchSinkService extends SinkService {

    @Resource
    private ElasticsearchClient esClient;

    private static final String LOG_INDEX = "arktouros-log";
    private static final String SPAN_INDEX = "arktouros-span";
    private static final String GAUGE_INDEX = "arktouros-gauge";
    private static final String COUNTER_INDEX = "arktouros-counter";
    private static final String SUMMARY_INDEX = "arktouros-summary";
    private static final String HISTOGRAM_INDEX = "arktouros-histogram";

    private static final List<String> indexList = List.of(LOG_INDEX, SPAN_INDEX, GAUGE_INDEX,
            COUNTER_INDEX, SUMMARY_INDEX, HISTOGRAM_INDEX);

    @PostConstruct
    public void initMappings() {
        // 先判断各索引是否存在
        indexList.forEach(this::checkAndCreate);
    }

    private void checkAndCreate(String indexName) {
        try {
            esClient.indices().exists(builder -> builder.index(indexName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
