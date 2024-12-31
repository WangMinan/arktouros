package edu.npu.arktouros.analyzer.otel;

import com.google.protobuf.ByteString;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.queue.TraceQueueItem;
import edu.npu.arktouros.service.queue.TraceQueueService;
import edu.npu.arktouros.service.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Status;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : {@link OtelTraceAnalyzer}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class OtelTraceAnalyzerTest {

    @Mock
    private SinkService sinkService;

    @Mock
    private TraceQueueService traceQueueService;

    private OtelTraceAnalyzer otelTraceAnalyzer;

    @BeforeEach
    void beforeAll() {
        otelTraceAnalyzer = new OtelTraceAnalyzer(sinkService);
        OtelTraceAnalyzer.setQueueService(traceQueueService);
    }

    @Test
    void testHandle() {
        ResourceSpans resourceSpans = ResourceSpans.newBuilder().build();
        Mockito.doNothing().when(traceQueueService)
                .put(any(TraceQueueItem.class));
        OtelTraceAnalyzer.handle(resourceSpans);
        Mockito.verify(traceQueueService, Mockito.times(1))
                .put(any(TraceQueueItem.class));
    }

    @Test
    void testHandleError() {
        ResourceSpans resourceSpans = ResourceSpans.newBuilder().build();
        // ProtoBufJsonUtils.toJson抛IOException
        try (MockedStatic<ProtoBufJsonUtils> protoBufJsonUtilsMockedStatic =
                     Mockito.mockStatic(ProtoBufJsonUtils.class)) {
            protoBufJsonUtilsMockedStatic.when(() ->
                            ProtoBufJsonUtils.toJSON(ArgumentMatchers.any()))
                    .thenThrow(IOException.class);
            Assertions.assertThrows(RuntimeException.class, () ->
                    OtelTraceAnalyzer.handle(resourceSpans));
        }
    }

    @Test
    void testInterrupt() {
        otelTraceAnalyzer.init();
        otelTraceAnalyzer.start();
        otelTraceAnalyzer.interrupt();
        Assertions.assertTrue(otelTraceAnalyzer.isInterrupted());
    }

    @Test
    void testTransformHandleValidItem() {
        try (InputStream resourceAsStream = OtelLogAnalyzerTest.class
                .getClassLoader()
                .getResourceAsStream(
                        "example/resource_span_example.json")) {
            if (resourceAsStream == null) {
                throw new IOException("example/resource_span_example.json");
            }
            String json = new String(resourceAsStream.readAllBytes());
            TraceQueueItem traceQueueItem = TraceQueueItem.builder().data(json).build();
            Mockito.when(traceQueueService.get()).thenReturn(traceQueueItem);
            Mockito.doNothing().when(sinkService).sink(ArgumentMatchers.any(Log.class));
            Assertions.assertDoesNotThrow(() -> otelTraceAnalyzer.transform());
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
            TraceQueueItem traceQueueItem = TraceQueueItem.builder().data(json).build();
            Mockito.when(traceQueueService.get()).thenReturn(traceQueueItem);
            Assertions.assertDoesNotThrow(() -> otelTraceAnalyzer.transform());
        }
    }

    @Test
    void testTransformShouldHandleIOException() {
        try (InputStream resourceAsStream = OtelLogAnalyzerTest.class
                .getClassLoader()
                .getResourceAsStream("example/resource_span_example.json")) {
            if (resourceAsStream == null) {
                throw new IOException("Failed to load resource_span_example.json");
            }
            String json = new String(resourceAsStream.readAllBytes());
            TraceQueueItem traceQueueItem = TraceQueueItem.builder().data(json).build();
            Mockito.when(traceQueueService.get()).thenReturn(traceQueueItem);
            Mockito.doThrow(IOException.class).when(sinkService).sink(ArgumentMatchers.any());
            Assertions.assertThrows(RuntimeException.class,
                    () -> otelTraceAnalyzer.transform());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConvertLink() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // 反射获取方法
        Method convertLink = OtelTraceAnalyzer.class
                .getDeclaredMethod("convertLink", Map.class, List.class);
        convertLink.setAccessible(true);
        // Creating the list of Span.Link
        List<io.opentelemetry.proto.trace.v1.Span.Link> links = new ArrayList<>();
        io.opentelemetry.proto.trace.v1.Span.Link link = io.opentelemetry.proto.trace.v1.Span.Link.newBuilder()
                .setTraceId(ByteString.copyFromUtf8("traceId1"))
                .setSpanId(ByteString.copyFromUtf8("spanId1"))
                .setTraceState("traceState1")
                .build();
        links.add(link);
        // Creating the tags map
        Map<String, String> tags = new HashMap<>();
        // Verifying the contents of tags
        convertLink.invoke(otelTraceAnalyzer, tags, links);
        Assertions.assertEquals("8390876135273620529|32493186086167601|traceState1||0",
                tags.get("otlp.link.0"));
    }

    @Test
    void testIdToHexStringNullInput() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method idToHexString = OtelTraceAnalyzer.class
                .getDeclaredMethod("idToHexString", ByteString.class);
        idToHexString.setAccessible(true);
        Assertions.assertEquals("",
                idToHexString.invoke(otelTraceAnalyzer, (Object) null));
    }

    @Test
    void testPopulateStatus() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Creating the Status instance
        Status status = Status.newBuilder()
                .setCode(Status.StatusCode.STATUS_CODE_ERROR)
                .setMessage("Test error message")
                .build();

        // Creating the tags map
        Map<String, String> tags = new HashMap<>();

        // Getting the method to test
        Method populateStatusMethod = OtelTraceAnalyzer.class
                .getDeclaredMethod("populateStatus", Status.class, Map.class);
        populateStatusMethod.setAccessible(true);

        // Invoking the method to test
        populateStatusMethod.invoke(otelTraceAnalyzer, status, tags);

        // Verifying the contents of tags
        Assertions.assertEquals("true", tags.get("error"));
        Assertions.assertEquals("STATUS_CODE_ERROR",
                tags.get("otel.status_code"));
        Assertions.assertEquals("Test error message",
                tags.get("otel.status_description"));
    }
}
