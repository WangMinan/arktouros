package edu.npu.arktouros.emitter.grpc.arktouros;

import edu.npu.arktouros.cache.LogQueueCache;
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
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)
class ArktourosGrpcEmitterTest {
    private SpanServiceGrpc.SpanServiceBlockingStub spanServiceBlockingStub;
    private MetricServiceGrpc.MetricServiceBlockingStub metricServiceBlockingStub;
    private LogServiceGrpc.LogServiceBlockingStub logServiceBlockingStub;
    private ArktourosGrpcEmitter grpcEmitter;
    private LogQueueCache cache;

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @BeforeEach
    void beforeEach() {
        spanServiceBlockingStub = Mockito.mock(SpanServiceGrpc.SpanServiceBlockingStub.class);
        metricServiceBlockingStub = Mockito.mock(MetricServiceGrpc.MetricServiceBlockingStub.class);
        logServiceBlockingStub = Mockito.mock(LogServiceGrpc.LogServiceBlockingStub.class);
        cache = new LogQueueCache();
        grpcEmitter = (ArktourosGrpcEmitter)
                new ArktourosGrpcEmitter.Factory().createEmitter(cache);
        grpcEmitter.spanServiceBlockingStub = spanServiceBlockingStub;
        grpcEmitter.metricServiceBlockingStub = metricServiceBlockingStub;
        grpcEmitter.logServiceBlockingStub = logServiceBlockingStub;
    }

    @Test
    @Timeout(30)
    void testRun() throws InterruptedException {
        initCache();
        // 几个span被调用的时候啥都不要做
        SpanResponse response1 = SpanResponse.newBuilder().build();
        Mockito.when(spanServiceBlockingStub.export(any(SpanRequest.class)))
                .thenReturn(response1);
        MetricResponse response2 = MetricResponse.newBuilder().build();
        Mockito.when(metricServiceBlockingStub.export(any(MetricRequest.class)))
                .thenReturn(response2);
        LogResponse response3 = LogResponse.newBuilder().build();
        Mockito.when(logServiceBlockingStub.export(any(LogRequest.class)))
                .thenReturn(response3);
        grpcEmitter.start();
        while (true) {
            if (cache.isEmpty()) {
                Thread.sleep(1000);
                grpcEmitter.interrupt();
                break;
            }
        }
        Assertions.assertTrue(cache.isEmpty());
    }

    private void initCache() {
        cache.put("""
                {
                  "serviceName": null,
                  "traceId": null,
                  "spanId": null,
                  "content": "",
                  "tags": [
                    {
                      "key": "k8s.resource.name",
                      "value": "events"
                    },
                    {
                      "key": "event.domain",
                      "value": "k8s"
                    },
                    {
                      "key": "event.name",
                      "value": "otel-collector-cluster-opentelemetry-collector-59cc664645-kjcc6.17c57569b0e7fa50"
                    }
                  ],
                  "error": false,
                  "timestamp": "0",
                  "severityText": "",
                  "type": "LOG"
                }
                """);
        cache.put("""
                {
                  "serviceName": null,
                  "traceId": null,
                  "spanId": null,
                  "content": "",
                  "tags": [
                    {
                      "key": "k8s.resource.name",
                      "value": "events"
                    },
                    {
                      "key": "event.domain",
                      "value": "k8s"
                    },
                    {
                      "key": "event.name",
                      "value": "otel-collector-cluster-opentelemetry-collector-59cc664645-kjcc6.17c57569b0e7fa50"
                    }
                  ],
                  alse,
                  "timestamp": "0",
                  "severityText": "",
                  "type": "LOG"
                }
                """);
        cache.put("""
                {
                  "name": "k8s.pod.phase",
                  "description": "Current phase of the pod (1 - Pending, 2 - Running, 3 - Succeeded, 4 - Failed, 5 - Unknown)",
                  "labels": {
                    "k8s_node_name": "minikube",
                    "k8s_pod_name": "kube-scheduler-minikube",
                    "k8s_pod_uid": "ed4639a4-8bb8-4e83-a06a-ed5488562c0b",
                    "k8s_namespace_name": "kube-system"
                  },
                  "timestamp": "1712904237218",
                  "value": 2,
                  "serviceName": "k8s",
                  "sourceType": "METRIC",
                  "metricType": "GAUGE"
                }
                """);
        cache.put("""
                {
                  "name": "lets-go",
                  "id": "8056722501683345247",
                  "serviceName": "telemetrygen",
                  "traceId": "9054263745264933759",
                  "parentSpanId": null,
                  "localEndPoint": {
                    "serviceName": "telemetrygen",
                    "ip": null,
                    "port": 0,
                    "latency": 0,
                    "type": "ENDPOINT"
                  },
                  "remoteEndPoint": {
                    "serviceName": "telemetrygen-server",
                    "ip": "1.2.3.4",
                    "port": 0,
                    "latency": 0,
                    "type": "ENDPOINT"
                  },
                  "startTime": "1712152069036",
                  "endTime": "1712152069036",
                  "root": true,
                  "type": "SPAN",
                  "tags": [
                    {
                      "key": "w3c_tracestate",
                      "value": ""
                    }
                  ]
                }
                """);
        cache.put("""
                {
                  "name": "lets-go",
                  "id": "8056722501683345247",
                  "serviceName": "telemetrygen",
                  "traceId": "9054263745264933759",
                  "parentSpanId": null,
                  "localEndPoint": {
                    "serviceName": "telemetrygen",
                    "ip": null,
                    "port": 0,
                    "latency": 0,
                    "type": "ENDPOINT"
                  },
                  "remoteEndPoint": {
                    "serviceName": "telemetrygen-server",
                    "ip": "1.2.3.4",
                    "port": 0,
                    "latency": 0,
                    "type": "ENDPOINT"
                  },
                  "startTime": "1712152069036",
                  "endTime": "1712152069036",
                  "root": true,
                  "type": "SPAN",
                  "tags": [
                    {
                      "key": "w3c_tracestate",
                      "value"
                  ]
                }
                """);
        cache.put("""
                {
                  "name": "k8s.pod.phase",
                  "description": "Current phase of the pod (1 - Pending, 2 - Running, 3 - Succeeded, 4 - Failed, 5 - Unknown)",
                  "labels": {
                    "k8s_node_name": "minikube",
                    "k8s_pod_name": "kube-scheduler-minikube",
                    "k8s_pod_uid": "ed4639a4-8bb8-4e83-a06a-ed5488562c0b",
                    "k8s_namespace_name": "kube-system"
                  },
                  "timestamp": "1712904237218",
                  "value": 2,
                  "serviceName": "k8s",
                  "sourceType": "METRIC",
                  "metricType": 
                }
                """);

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

        response = SpanResponse.newBuilder().setRejectedSpanRecords(2).build();
        Mockito.when(spanServiceBlockingStub.export(any(SpanRequest.class)))
                .thenReturn(response);
        emitSpan.invoke(grpcEmitter, builder.build());

        Mockito.verify(spanServiceBlockingStub, times(2))
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

        response = MetricResponse.newBuilder().setRejectedMetricRecords(2).build();
        Mockito.when(metricServiceBlockingStub.export(any(MetricRequest.class)))
                .thenReturn(response);
        emitSpan.invoke(grpcEmitter, builder.build());

        Mockito.verify(metricServiceBlockingStub, times(2))
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

        response = LogResponse.newBuilder().setRejectedLogRecords(2).build();
        Mockito.when(logServiceBlockingStub.export(any(LogRequest.class)))
                .thenReturn(response);
        emitSpan.invoke(grpcEmitter, builder.build());

        Mockito.verify(logServiceBlockingStub, times(2))
                .export(any(LogRequest.class));
    }

    @Test
    @Timeout(30)
    void testHandleSpanWithException() throws NoSuchMethodException, IOException {
        log.info("testHandleSpanWithException");
        Span.Builder builder = Span.newBuilder();
        Mockito.when(spanServiceBlockingStub.export(any(SpanRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method emitSpan = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitSpan", Span.class);
        emitSpan.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> emitSpan.invoke(grpcEmitter, builder.build()));
    }

    @Test
    @Timeout(30)
    void testHandleMetricWithException() throws NoSuchMethodException, IOException {
        log.info("testHandleMetricWithException");
        Metric.Builder builder = Metric.newBuilder();
        Mockito.when(metricServiceBlockingStub.export(any(MetricRequest.class)))
                .thenThrow(StatusRuntimeException.class);
        Method emitMetric = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitMetric", Metric.class);
        emitMetric.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> emitMetric.invoke(grpcEmitter, builder.build()));
    }

    @Test
    @Timeout(30)
    void testHandleLogWithException() throws NoSuchMethodException, IOException {
        log.info("testHandleLogWithException");
        Log.Builder builder = Log.newBuilder();
        Mockito.when(logServiceBlockingStub.export(any(LogRequest.class))
        ).thenThrow(StatusRuntimeException.class);
        Method emitLog = ArktourosGrpcEmitter.class
                .getDeclaredMethod("emitLog", Log.class);
        emitLog.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> emitLog.invoke(grpcEmitter, builder.build()));
    }
}
