package edu.npu.arktouros.service.otel.sinker;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import jakarta.annotation.Resource;

/**
 * @author : [wangminan]
 * @description : ElasticSearch数据持久化
 */
public class ElasticSearchSinkService extends SinkService {

    @Resource
    private ElasticsearchClient esClient;

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
