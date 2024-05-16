package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import edu.npu.arktouros.service.otel.queue.MetricsQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : {@link OtelMetricsAnalyzer}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class OtelMetricsAnalyzerTest {
    @Mock
    private SinkService sinkService;

    @Mock
    private MetricsQueueService metricsQueueService;

    private OtelMetricsAnalyzer otelMetricsAnalyzer;

    @BeforeEach
    void setup() {
        otelMetricsAnalyzer = new OtelMetricsAnalyzer(sinkService);
        OtelMetricsAnalyzer.setQueueService(metricsQueueService);
    }

    @Test
    void testHandle() {
        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder().build();
        Mockito.doNothing().when(metricsQueueService)
                .put(any(MetricsQueueItem.class));
        OtelMetricsAnalyzer.handle(resourceMetrics);
        Mockito.verify(metricsQueueService, Mockito.times(1))
                .put(any(MetricsQueueItem.class));
    }

    @Test
    void testHandleError() {
        ResourceMetrics resourceMetrics = ResourceMetrics.newBuilder().build();
        // ProtoBufJsonUtils.toJson抛IOException
        try (MockedStatic<ProtoBufJsonUtils> protoBufJsonUtilsMockedStatic =
                     Mockito.mockStatic(ProtoBufJsonUtils.class)) {
            protoBufJsonUtilsMockedStatic.when(() ->
                            ProtoBufJsonUtils.toJSON(ArgumentMatchers.any()))
                    .thenThrow(IOException.class);
            Assertions.assertThrows(RuntimeException.class, () ->
                    OtelMetricsAnalyzer.handle(resourceMetrics));
        }
    }

    @Test
    void testTransformHandleValidLogQueueItem() {
        for (int i = 0; i < 5; i++) {
            try (InputStream resourceAsStream = OtelLogAnalyzerTest.class
                    .getClassLoader()
                    .getResourceAsStream(
                            "example/resource_metric_example_" + i +".json")) {
                if (resourceAsStream == null) {
                    throw new IOException("example/resource_metric_example_" + i +".json");
                }
                String json = new String(resourceAsStream.readAllBytes());
                MetricsQueueItem metricsQueueItem = MetricsQueueItem.builder().data(json).build();
                Mockito.when(metricsQueueService.get()).thenReturn(metricsQueueItem);
                Mockito.doNothing().when(sinkService).sink(ArgumentMatchers.any(Log.class));
                Assertions.assertDoesNotThrow(() -> otelMetricsAnalyzer.transform());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void testTransformError() {
        // ProtoBufJsonUtils.toJson抛IOException
        try (MockedStatic<ProtoBufJsonUtils> protoBufJsonUtilsMockedStatic =
                     Mockito.mockStatic(ProtoBufJsonUtils.class)) {
            protoBufJsonUtilsMockedStatic.when(() ->
                            ProtoBufJsonUtils.fromJSON(ArgumentMatchers.any(String.class),
                                    ArgumentMatchers.any(ResourceLogs.Builder.class)))
                    .thenThrow(IOException.class);
            String json = "{}";
            MetricsQueueItem metricsQueueItem = MetricsQueueItem.builder().data(json).build();
            Mockito.when(metricsQueueService.get()).thenReturn(metricsQueueItem);
            Assertions.assertDoesNotThrow(() -> otelMetricsAnalyzer.transform());
        }
    }

    @Test
    void testTransformShouldHandleIOException() {
        try (InputStream resourceAsStream = OtelLogAnalyzerTest.class
                .getClassLoader()
                .getResourceAsStream("example/resource_metric_example_0.json")) {
            if (resourceAsStream == null) {
                throw new IOException("Failed to load resource_metric_example_0.json");
            }
            String json = new String(resourceAsStream.readAllBytes());
            MetricsQueueItem metricsQueueItem = MetricsQueueItem.builder().data(json).build();
            Mockito.when(metricsQueueService.get()).thenReturn(metricsQueueItem);
            Mockito.doThrow(IOException.class).when(sinkService).sink(ArgumentMatchers.any());
            Assertions.assertThrows(RuntimeException.class,
                    () -> otelMetricsAnalyzer.transform());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInterrupt() {
        otelMetricsAnalyzer.init();
        otelMetricsAnalyzer.start();
        otelMetricsAnalyzer.interrupt();
        Assertions.assertTrue(otelMetricsAnalyzer.isInterrupted());
    }
}
