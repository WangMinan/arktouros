package edu.npu.arktouros.emit.grpc;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.emit.AbstractEmitter;
import edu.npu.arktouros.emit.EmitterFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : [grpc发射器]
 */
@Slf4j
public class GrpcEmitter extends AbstractEmitter {

    @Getter
    private final ManagedChannel channel;
    private final LogsServiceGrpc.LogsServiceBlockingStub logsServiceBlockingStub;
    private final MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceBlockingStub;
    private final TraceServiceGrpc.TraceServiceBlockingStub traceServiceBlockingStub;

    public GrpcEmitter(AbstractCache inputCache) {
        super(inputCache);
        String HOST = PropertiesProvider.getProperty("emitter.grpc.host");
        int PORT = Integer.parseInt(PropertiesProvider.getProperty("emitter.grpc.port"));
        channel = ManagedChannelBuilder.forAddress(HOST, PORT)
                .usePlaintext()
                .build();
        logsServiceBlockingStub = LogsServiceGrpc.newBlockingStub(channel);
        metricsServiceBlockingStub = MetricsServiceGrpc.newBlockingStub(channel);
        traceServiceBlockingStub = TraceServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void run() {
        // 我们通过拿到的json串的前缀来判断这玩意是metrics logs还是trace
        while (true) {
            String inputJson = inputCache.get();
            try {
                if (inputJson.startsWith("{\"resourceSpans\":")) {
                    handleTrace(inputJson);
                } else if (inputJson.startsWith("{\"resourceMetrics\":")) {
                    handleMetrics(inputJson);
                } else if (inputJson.startsWith("{\"resourceLogs\":")) {
                    handleLogs(inputJson);
                } else {
                    log.warn("Invalid input for json: {}", inputJson);
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
        log.debug("sending logs data to apm: {}", inputJson);
        ExportLogsServiceRequest request =
                ExportLogsServiceRequest
                        .newBuilder()
                        .addAllResourceLogs(logsData.getResourceLogsList())
                        .build();
        ExportLogsServiceResponse response = logsServiceBlockingStub.export(request);
        log.debug("response from apm: {}", response);
    }

    private void handleMetrics(String inputJson) throws IOException {
        MetricsData.Builder builder = MetricsData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        MetricsData metricsData = builder.build();
        log.debug("sending metrics data to apm: {}", inputJson);
        ExportMetricsServiceRequest request =
                ExportMetricsServiceRequest
                        .newBuilder()
                        .addAllResourceMetrics(metricsData.getResourceMetricsList())
                        .build();
        ExportMetricsServiceResponse response = metricsServiceBlockingStub.export(request);
        log.debug("response from apm: {}", response);
    }

    private void handleTrace(String inputJson) throws IOException {
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        TracesData tracesData = builder.build();
        log.debug("sending trace data to apm: {}", inputJson);
        ExportTraceServiceRequest request =
                ExportTraceServiceRequest
                        .newBuilder()
                        .addAllResourceSpans(tracesData.getResourceSpansList())
                        .build();
        ExportTraceServiceResponse response = traceServiceBlockingStub.export(request);
        log.debug("response from apm: {}", response);
    }

    public static class Factory implements EmitterFactory {

        @Override
        public AbstractEmitter createEmitter(AbstractCache inputCache) {
            return new GrpcEmitter(inputCache);
        }
    }
}
