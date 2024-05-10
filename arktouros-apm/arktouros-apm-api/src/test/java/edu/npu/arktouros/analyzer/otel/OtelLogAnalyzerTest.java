package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.queue.LogQueueItem;
import edu.npu.arktouros.service.otel.queue.LogQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

/**
 * @author : [wangminan]
 * @description : {@link OtelLogAnalyzer}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class OtelLogAnalyzerTest {
    @Mock
    private SinkService sinkService;

    @Mock
    private LogQueueService logQueueService;

    private final OtelLogAnalyzer otelLogAnalyzer = new OtelLogAnalyzer(sinkService);

    @BeforeEach
    public void setup() {
        OtelLogAnalyzer.setQueueService(logQueueService);
    }

    @Test
    public void testHandle() {
        ResourceLogs resourceLogs = ResourceLogs.newBuilder().build();
        Mockito.doNothing().when(logQueueService).put(any(LogQueueItem.class));
        OtelLogAnalyzer.handle(resourceLogs);
        Mockito.verify(logQueueService, times(1))
                .put(any(LogQueueItem.class));
    }

    @Test
    public void testTransform() throws IOException {
        LogQueueItem logQueueItem = LogQueueItem.builder().data("{}").build();
        Mockito.when(logQueueService.get()).thenReturn(logQueueItem);
        Mockito.doNothing().when(sinkService).sink(any(Log.class));
        Assertions.assertDoesNotThrow(otelLogAnalyzer::transform);
    }
}
