package edu.npu.arktouros.service.otel.sinker;

import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

public abstract class SinkService {
    public abstract void saveResourceLogs(ResourceLogs resourceLogs);

    public abstract void saveResourceSpans(ResourceSpans resourceSpans);

    public abstract void saveResourceMetrics(ResourceMetrics resourceMetrics);
}
