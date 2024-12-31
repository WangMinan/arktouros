package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.queue.LogQueueItem;
import edu.npu.arktouros.service.queue.LogQueueService;
import edu.npu.arktouros.service.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
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

/**
 * @author : [wangminan]
 * @description : {@link OtelLogAnalyzer}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class OtelLogAnalyzerTest {
    @Mock
    private SinkService sinkService;

    @Mock
    private LogQueueService logQueueService;

    private OtelLogAnalyzer otelLogAnalyzer;

    @BeforeEach
    void setup() {
        otelLogAnalyzer = new OtelLogAnalyzer(sinkService);
        OtelLogAnalyzer.setQueueService(logQueueService);
    }

    @Test
    void testHandle() {
        ResourceLogs resourceLogs = ResourceLogs.newBuilder().build();
        Mockito.doNothing().when(logQueueService)
                .put(ArgumentMatchers.any(LogQueueItem.class));
        OtelLogAnalyzer.handle(resourceLogs);
        Mockito.verify(logQueueService, Mockito.times(1))
                .put(ArgumentMatchers.any(LogQueueItem.class));
    }

    @Test
    void testHandleError() {
        ResourceLogs resourceLogs = ResourceLogs.newBuilder().build();
        // ProtoBufJsonUtils.toJson抛IOException
        try (MockedStatic<ProtoBufJsonUtils> protoBufJsonUtilsMockedStatic =
                     Mockito.mockStatic(ProtoBufJsonUtils.class)) {
            protoBufJsonUtilsMockedStatic.when(() -> ProtoBufJsonUtils.toJSON(ArgumentMatchers.any()))
                    .thenThrow(IOException.class);
            Assertions.assertThrows(RuntimeException.class, () -> OtelLogAnalyzer.handle(resourceLogs));
        }
    }

    @Test
    void transformShouldHandleEmptyLog() throws IOException {
        String json = "{}";
        LogQueueItem logQueueItem = LogQueueItem.builder().data(json).build();
        Mockito.when(logQueueService.get()).thenReturn(logQueueItem);
        Mockito.doNothing().when(sinkService).sink(ArgumentMatchers.any(Log.class));
        Assertions.assertDoesNotThrow(() -> otelLogAnalyzer.transform());
    }

    @Test
    void transformShouldHandleValidLogQueueItem() {
        try (InputStream resourceAsStream = OtelLogAnalyzerTest.class
                .getClassLoader()
                .getResourceAsStream("example/resource_log_example.json")) {
            if (resourceAsStream == null) {
                throw new IOException("Failed to load resource_log_example.json");
            }
            String json = new String(resourceAsStream.readAllBytes());
            LogQueueItem logQueueItem = LogQueueItem.builder().data(json).build();
            Mockito.when(logQueueService.get()).thenReturn(logQueueItem);
            Mockito.doNothing().when(sinkService).sink(ArgumentMatchers.any(Log.class));
            Assertions.assertDoesNotThrow(() -> otelLogAnalyzer.transform());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            LogQueueItem logQueueItem = LogQueueItem.builder().data(json).build();
            Mockito.when(logQueueService.get()).thenReturn(logQueueItem);
            Assertions.assertDoesNotThrow(() -> otelLogAnalyzer.transform());
        }
    }

    @Test
    void transformShouldHandleIOException() {
        try (InputStream resourceAsStream = OtelLogAnalyzerTest.class
                .getClassLoader()
                .getResourceAsStream("example/resource_log_example.json")) {
            if (resourceAsStream == null) {
                throw new IOException("Failed to load resource_log_example.json");
            }
            String json = new String(resourceAsStream.readAllBytes());
            LogQueueItem logQueueItem = LogQueueItem.builder().data(json).build();
            Mockito.when(logQueueService.get()).thenReturn(logQueueItem);
            Mockito.doThrow(IOException.class).when(sinkService).sink(ArgumentMatchers.any());
            Assertions.assertThrows(RuntimeException.class,
                    () -> otelLogAnalyzer.transform());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInterrupt() {
        otelLogAnalyzer.init();
        otelLogAnalyzer.start();
        otelLogAnalyzer.interrupt();
        Assertions.assertTrue(otelLogAnalyzer.isInterrupted());
    }
}
