package edu.npu.arktouros.emitter.grpc.otel;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.emitter.AbstractEmitter;
import edu.npu.arktouros.emitter.EmitterFactory;
import edu.npu.arktouros.emitter.grpc.AbstractGrpcEmitter;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.trace.v1.TracesData;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : [grpc发射器]
 */
@Slf4j
public class OtelGrpcEmitter extends AbstractGrpcEmitter {


    protected LogsServiceGrpc.LogsServiceBlockingStub logsServiceBlockingStub;
    protected MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceBlockingStub;
    protected TraceServiceGrpc.TraceServiceBlockingStub traceServiceBlockingStub;

    protected OtelGrpcEmitter(AbstractCache inputCache) {
        super(inputCache);

        logsServiceBlockingStub = LogsServiceGrpc.newBlockingStub(channel);
        metricsServiceBlockingStub = MetricsServiceGrpc.newBlockingStub(channel);
        traceServiceBlockingStub = TraceServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void run() {
        // 我们通过拿到的json串的前缀来判断这玩意是metrics logs还是trace
        // 这个位置是有够见鬼的 但好像没有别的办法 我们的ProtobufJsonUtil帮不上忙
        while (true) {
            String inputJson = inputCache.get().trim();
            String tmpStr = inputJson;
            // 删除tmpStr开头的大括号和空格
            while (
                    tmpStr.startsWith("{") ||
                            tmpStr.startsWith(" ") ||
                            tmpStr.startsWith("\n") ||
                            tmpStr.startsWith("\r")
            ) {
                tmpStr = tmpStr.substring(1);
            }
            try {
                // 这位置可能还有换行符
                if (tmpStr.startsWith("\"resourceSpans\"")) {
                    handleTrace(inputJson);
                } else if (tmpStr.startsWith("\"resourceMetrics\"")) {
                    handleMetrics(inputJson);
                } else if (tmpStr.startsWith("\"resourceLogs\"")) {
                    handleLogs(inputJson);
                } else {
                    log.warn("Invalid input for json when emitting: {}", inputJson);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleLogs(String inputJson) throws IOException {
        LogsData.Builder builder = LogsData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        LogsData logsData = builder.build();
        log.info("Sending otel logs data to apm");
        ExportLogsServiceRequest request =
                ExportLogsServiceRequest
                        .newBuilder()
                        .addAllResourceLogs(logsData.getResourceLogsList())
                        .build();
        try {
            ExportLogsServiceResponse export = logsServiceBlockingStub.export(request);
            if (export.getPartialSuccess().getRejectedLogRecords() != 0) {
                log.error("Failed to send otel logs data to apm, rejected log records: {}, error message: {}.",
                        export.getPartialSuccess().getRejectedLogRecords(),
                        export.getPartialSuccess().getErrorMessage()
                );
            }
        } catch (StatusRuntimeException e) {
            log.error("Failed to send otel logs data to apm, error message: {}.",
                    e.getMessage()
            );
        }
    }

    private void handleMetrics(String inputJson) throws IOException {
        MetricsData.Builder builder = MetricsData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        MetricsData metricsData = builder.build();
        log.info("Sending otel metrics data to apm");
        ExportMetricsServiceRequest request =
                ExportMetricsServiceRequest
                        .newBuilder()
                        .addAllResourceMetrics(metricsData.getResourceMetricsList())
                        .build();
        try {
            ExportMetricsServiceResponse export = metricsServiceBlockingStub.export(request);
            if (export.getPartialSuccess().getRejectedDataPoints() != 0) {
                log.error("Failed to send otel metrics data to apm, rejected data points: {}, error message: {}.",
                        export.getPartialSuccess().getRejectedDataPoints(),
                        export.getPartialSuccess().getErrorMessage()
                );
            }
        } catch (StatusRuntimeException e) {
            log.error("Failed to send otel metrics data to apm, error message: {}.",
                    e.getMessage()
            );
        }
    }

    private void handleTrace(String inputJson) throws IOException {
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        TracesData tracesData = builder.build();
        log.info("Sending otel trace data to apm");
        ExportTraceServiceRequest request =
                ExportTraceServiceRequest
                        .newBuilder()
                        .addAllResourceSpans(tracesData.getResourceSpansList())
                        .build();
        try {
            ExportTraceServiceResponse export = traceServiceBlockingStub.export(request);
            if (export.getPartialSuccess().getRejectedSpans() != 0) {
                log.error("Failed to send otel trace data to apm, rejected spans: {}, error message: {}.",
                        export.getPartialSuccess().getRejectedSpans(),
                        export.getPartialSuccess().getErrorMessage()
                );
            }
        } catch (StatusRuntimeException e) {
            log.error("Failed to send otel trace data to apm, error message: {}.",
                    e.getMessage()
            );
        }
    }

    public static class Factory implements EmitterFactory {

        @Override
        public AbstractEmitter createEmitter(AbstractCache inputCache) {
            return new OtelGrpcEmitter(inputCache);
        }
    }
}
