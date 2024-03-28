package edu.npu.arktouros.service.otel.sinker.h2;

import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

/**
 * @author : [wangminan]
 * @description : H2数据持久化
 */
public class H2SinkService extends SinkService {

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
