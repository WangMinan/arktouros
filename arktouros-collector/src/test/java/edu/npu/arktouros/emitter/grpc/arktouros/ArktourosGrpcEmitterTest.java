package edu.npu.arktouros.emitter.grpc.arktouros;

import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.proto.collector.v1.LogRequest;
import edu.npu.arktouros.proto.collector.v1.LogResponse;
import edu.npu.arktouros.proto.collector.v1.LogServiceGrpc;
import edu.npu.arktouros.proto.collector.v1.Metric;
import edu.npu.arktouros.proto.collector.v1.MetricRequest;
import edu.npu.arktouros.proto.collector.v1.MetricResponse;
import edu.npu.arktouros.proto.collector.v1.MetricServiceGrpc;
import edu.npu.arktouros.proto.collector.v1.SpanRequest;
import edu.npu.arktouros.proto.collector.v1.SpanResponse;
import edu.npu.arktouros.proto.collector.v1.SpanServiceGrpc;
import edu.npu.arktouros.proto.log.v1.Log;
import edu.npu.arktouros.proto.span.v1.Span;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

/**
 * @author : [wangminan]
 * @description : {@link ArktourosGrpcEmitter}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class ArktourosGrpcEmitterTest {
    private SpanServiceGrpc.SpanServiceBlockingStub spanServiceBlockingStub;
    private MetricServiceGrpc.MetricServiceBlockingStub metricServiceBlockingStub;
    private LogServiceGrpc.LogServiceBlockingStub logServiceBlockingStub;
    private ArktourosGrpcEmitter grpcEmitter;

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @BeforeEach
    void beforeEach() {
        spanServiceBlockingStub = Mockito.mock(SpanServiceGrpc.SpanServiceBlockingStub.class);
        metricServiceBlockingStub = Mockito.mock(MetricServiceGrpc.MetricServiceBlockingStub.class);
        logServiceBlockingStub = Mockito.mock(LogServiceGrpc.LogServiceBlockingStub.class);
        grpcEmitter = new ArktourosGrpcEmitter(null);
        grpcEmitter.spanServiceBlockingStub = spanServiceBlockingStub;
        grpcEmitter.metricServiceBlockingStub = metricServiceBlockingStub;
        grpcEmitter.logServiceBlockingStub = logServiceBlockingStub;
    }

    @Test
    @Timeout(30)
    void testHandleSpan() throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        log.info("testHandleSpan");
        String inputJson = "{}"; // replace with actual JSON
        Span.Builder builder = Span.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        SpanResponse response = SpanResponse.newBuilder().build();
        Mockito.when(spanServiceBlockingStub.export(any(SpanRequest.class)))
                .thenReturn(response);
        Method emitSpan = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitSpan", Span.class);
        emitSpan.setAccessible(true);
        emitSpan.invoke(grpcEmitter, builder.build());
        Mockito.verify(spanServiceBlockingStub, times(1))
                .export(any(SpanRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleMetric() throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        log.info("testHandleMetric");
        String inputJson = "{}"; // replace with actual JSON
        Metric.Builder builder = Metric.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        MetricResponse response = MetricResponse.newBuilder().build();
        Mockito.when(metricServiceBlockingStub.export(any(MetricRequest.class)))
                .thenReturn(response);
        Method emitSpan = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitMetric", Metric.class);
        emitSpan.setAccessible(true);
        emitSpan.invoke(grpcEmitter, builder.build());
        Mockito.verify(metricServiceBlockingStub, times(1))
                .export(any(MetricRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleLog() throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        log.info("testHandleLog");
        String inputJson = "{}"; // replace with actual JSON
        Log.Builder builder = Log.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        LogResponse response = LogResponse.newBuilder().build();
        Mockito.when(logServiceBlockingStub.export(any(LogRequest.class)))
                .thenReturn(response);
        Method emitSpan = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitLog", Log.class);
        emitSpan.setAccessible(true);
        emitSpan.invoke(grpcEmitter, builder.build());
        Mockito.verify(logServiceBlockingStub, times(1))
                .export(any(LogRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleSpanWithException() throws NoSuchMethodException, IOException {
        log.info("testHandleSpanWithException");
        Span.Builder builder = Span.newBuilder();
        Mockito.when(spanServiceBlockingStub.export(any(SpanRequest.class)))
                .thenThrow(RuntimeException.class);
        Method emitSpan = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitSpan", Span.class);
        emitSpan.setAccessible(true);
        Assertions.assertThrows(Exception.class,
                () -> emitSpan.invoke(grpcEmitter, builder.build()));
    }

    @Test
    @Timeout(30)
    void testHandleMetricWithException() throws NoSuchMethodException, IOException {
        log.info("testHandleMetricWithException");
        Metric.Builder builder = Metric.newBuilder();
        Mockito.when(metricServiceBlockingStub.export(any(MetricRequest.class)))
                .thenThrow(RuntimeException.class);
        Method emitMetric = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitMetric", Metric.class);
        emitMetric.setAccessible(true);
        Assertions.assertThrows(Exception.class,
                () -> emitMetric.invoke(grpcEmitter, builder.build()));
    }

    @Test
    @Timeout(30)
    void testHandleLogWithException() throws NoSuchMethodException, IOException {
        log.info("testHandleLogWithException");
        Log.Builder builder = Log.newBuilder();
        Mockito.when(logServiceBlockingStub.export(any(LogRequest.class))
        ).thenThrow(RuntimeException.class);
        Method emitLog = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitLog", Log.class);
        emitLog.setAccessible(true);
        Assertions.assertThrows(Exception.class,
                () -> emitLog.invoke(grpcEmitter, builder.build()));
    }
}
