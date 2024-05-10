package edu.npu.arktouros.emitter.otel;

import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.config.PropertiesProvider;
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
import io.opentelemetry.proto.trace.v1.TracesData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author : [wangminan]
 * @description : {@link OtelGrpcEmitter}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class OtelGrpcEmitterTest {

    private TraceServiceGrpc.TraceServiceBlockingStub traceServiceBlockingStub;
    private MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceBlockingStub;
    private LogsServiceGrpc.LogsServiceBlockingStub logsServiceBlockingStub;
    private OtelGrpcEmitter otelGrpcEmitter;

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @BeforeEach
    void beforeEach() {
        traceServiceBlockingStub = Mockito.mock(TraceServiceGrpc.TraceServiceBlockingStub.class);
        metricsServiceBlockingStub = Mockito.mock(MetricsServiceGrpc.MetricsServiceBlockingStub.class);
        logsServiceBlockingStub = Mockito.mock(LogsServiceGrpc.LogsServiceBlockingStub.class);
        otelGrpcEmitter = new OtelGrpcEmitter(null);
        otelGrpcEmitter.traceServiceBlockingStub = traceServiceBlockingStub;
        otelGrpcEmitter.metricsServiceBlockingStub = metricsServiceBlockingStub;
        otelGrpcEmitter.logsServiceBlockingStub = logsServiceBlockingStub;
    }

    @Test
    @Timeout(30)
    void testKeepAlive() throws
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("testKeepAlive");
        LogQueueCache cache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        OtelGrpcEmitter emitter = new OtelGrpcEmitter(cache);
        Method keepAliveCheck =
                OtelGrpcEmitter.class
                        .getDeclaredMethod("startKeepAliveCheck", CountDownLatch.class);
        keepAliveCheck.setAccessible(true);
        keepAliveCheck.invoke(emitter, new CountDownLatch(1));
    }

    @Test
    @Timeout(30)
    void testHandleTrace() throws
            IOException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        log.info("testHandleTrace");
        String inputJson = "{}"; // replace with actual JSON
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        ExportTraceServiceResponse response =
                ExportTraceServiceResponse.newBuilder()
                        .build();
        Mockito.when(traceServiceBlockingStub.export(any(ExportTraceServiceRequest.class)))
                .thenReturn(response);
        Method handleTrace = OtelGrpcEmitter.class.getDeclaredMethod("handleTrace", String.class);
        handleTrace.setAccessible(true);
        handleTrace.invoke(otelGrpcEmitter, inputJson);
        Mockito.verify(traceServiceBlockingStub, times(1))
                .export(any(ExportTraceServiceRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleTraceWithException() throws NoSuchMethodException {
        log.info("testHandleTraceWithException");
        String inputJson = "{}"; // replace with actual JSON
        Mockito.when(traceServiceBlockingStub.export(any(ExportTraceServiceRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method handleTrace = OtelGrpcEmitter.class.getDeclaredMethod("handleTrace", String.class);
        handleTrace.setAccessible(true);
        // 会被catch住 不会抛异常
        Assertions.assertDoesNotThrow(() -> handleTrace.invoke(otelGrpcEmitter, inputJson));
    }

    @Test
    @Timeout(30)
    void testHandleMetrics() throws
            IOException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        log.info("testHandleMetrics");
        String inputJson = "{}";
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        ExportMetricsServiceResponse response =
                ExportMetricsServiceResponse.newBuilder()
                        .build();
        Mockito.when(metricsServiceBlockingStub.export(any(ExportMetricsServiceRequest.class)))
                .thenReturn(response);
        Method handleMetric = OtelGrpcEmitter.class.getDeclaredMethod("handleMetrics", String.class);
        handleMetric.setAccessible(true);
        handleMetric.invoke(otelGrpcEmitter, inputJson);
        Mockito.verify(metricsServiceBlockingStub, times(1))
                .export(any(ExportMetricsServiceRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleMetricsWithException() throws NoSuchMethodException {
        log.info("testHandleMetricsWithException");
        String inputJson = "{}";
        Mockito.when(metricsServiceBlockingStub.export(any(ExportMetricsServiceRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method handleMetric = OtelGrpcEmitter.class.getDeclaredMethod("handleMetrics", String.class);
        handleMetric.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> handleMetric.invoke(otelGrpcEmitter, inputJson));
    }

    @Test
    @Timeout(30)
    void testHandleLogs() throws
            IOException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        log.info("testHandleLogs");
        String inputJson = "{}";
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        ExportLogsServiceResponse response =
                ExportLogsServiceResponse.newBuilder()
                        .build();
        Mockito.when(logsServiceBlockingStub.export(any(ExportLogsServiceRequest.class)))
                .thenReturn(response);
        Method handleLog = OtelGrpcEmitter.class.getDeclaredMethod("handleLogs", String.class);
        handleLog.setAccessible(true);
        handleLog.invoke(otelGrpcEmitter, inputJson);
        Mockito.verify(logsServiceBlockingStub, times(1))
                .export(any(ExportLogsServiceRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleLogsWithException() throws NoSuchMethodException {
        log.info("testHandleLogsWithException");
        String inputJson = "{}";
        Mockito.when(logsServiceBlockingStub.export(any(ExportLogsServiceRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method handleLog = OtelGrpcEmitter.class.getDeclaredMethod("handleLogs", String.class);
        handleLog.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> handleLog.invoke(otelGrpcEmitter, inputJson));
    }
}
