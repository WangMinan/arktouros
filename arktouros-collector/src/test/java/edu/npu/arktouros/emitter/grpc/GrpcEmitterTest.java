package edu.npu.arktouros.emitter.grpc;

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
 * @description : {@link GrpcEmitter}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class GrpcEmitterTest {

    private TraceServiceGrpc.TraceServiceBlockingStub traceServiceBlockingStub;
    private MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceBlockingStub;
    private LogsServiceGrpc.LogsServiceBlockingStub logsServiceBlockingStub;
    private GrpcEmitter grpcEmitter;

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @BeforeEach
    void beforeEach() {
        traceServiceBlockingStub = Mockito.mock(TraceServiceGrpc.TraceServiceBlockingStub.class);
        metricsServiceBlockingStub = Mockito.mock(MetricsServiceGrpc.MetricsServiceBlockingStub.class);
        logsServiceBlockingStub = Mockito.mock(LogsServiceGrpc.LogsServiceBlockingStub.class);
        grpcEmitter = new GrpcEmitter(null);
        grpcEmitter.traceServiceBlockingStub = traceServiceBlockingStub;
        grpcEmitter.metricsServiceBlockingStub = metricsServiceBlockingStub;
        grpcEmitter.logsServiceBlockingStub = logsServiceBlockingStub;
    }

    @Test
    void testKeepAlive() throws
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("testKeepAlive");
        LogQueueCache cache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        GrpcEmitter emitter = new GrpcEmitter(cache);
        Method keepAliveCheck =
                GrpcEmitter.class
                        .getDeclaredMethod("startKeepAliveCheck", CountDownLatch.class);
        keepAliveCheck.setAccessible(true);
        keepAliveCheck.invoke(emitter, new CountDownLatch(1));
    }

    @Test
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
        Method handleTrace = GrpcEmitter.class.getDeclaredMethod("handleTrace", String.class);
        handleTrace.setAccessible(true);
        handleTrace.invoke(grpcEmitter, inputJson);
        Mockito.verify(traceServiceBlockingStub, times(1))
                .export(any(ExportTraceServiceRequest.class));
    }

    @Test
    void testHandleTraceWithException() throws NoSuchMethodException {
        log.info("testHandleTraceWithException");
        String inputJson = "{}"; // replace with actual JSON
        Mockito.when(traceServiceBlockingStub.export(any(ExportTraceServiceRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method handleTrace = GrpcEmitter.class.getDeclaredMethod("handleTrace", String.class);
        handleTrace.setAccessible(true);
        // 会被catch住 不会抛异常
        Assertions.assertDoesNotThrow(() -> handleTrace.invoke(grpcEmitter, inputJson));
    }

    @Test
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
        Method handleMetric = GrpcEmitter.class.getDeclaredMethod("handleMetrics", String.class);
        handleMetric.setAccessible(true);
        handleMetric.invoke(grpcEmitter, inputJson);
        Mockito.verify(metricsServiceBlockingStub, times(1))
                .export(any(ExportMetricsServiceRequest.class));
    }

    @Test
    void testHandleMetricsWithException() throws NoSuchMethodException {
        log.info("testHandleMetricsWithException");
        String inputJson = "{}";
        Mockito.when(metricsServiceBlockingStub.export(any(ExportMetricsServiceRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method handleMetric = GrpcEmitter.class.getDeclaredMethod("handleMetrics", String.class);
        handleMetric.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> handleMetric.invoke(grpcEmitter, inputJson));
    }

    @Test
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
        Method handleLog = GrpcEmitter.class.getDeclaredMethod("handleLogs", String.class);
        handleLog.setAccessible(true);
        handleLog.invoke(grpcEmitter, inputJson);
        Mockito.verify(logsServiceBlockingStub, times(1))
                .export(any(ExportLogsServiceRequest.class));
    }

    @Test
    void testHandleLogsWithException() throws NoSuchMethodException {
        log.info("testHandleLogsWithException");
        String inputJson = "{}";
        Mockito.when(logsServiceBlockingStub.export(any(ExportLogsServiceRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method handleLog = GrpcEmitter.class.getDeclaredMethod("handleLogs", String.class);
        handleLog.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> handleLog.invoke(grpcEmitter, inputJson));
    }
}
