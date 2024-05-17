package edu.npu.arktouros.receiver.otel;

import edu.npu.arktouros.service.otel.queue.LogQueueService;
import edu.npu.arktouros.service.otel.queue.MetricsQueueService;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author : [wangminan]
 * @description : {@link OtelGrpcReceiver}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class OtelGrpcReceiverTest {

    @Test
    void testMain() {
        LogQueueService logQueueService = Mockito.mock(LogQueueService.class);
        TraceQueueService traceQueueService = Mockito.mock(TraceQueueService.class);
        MetricsQueueService metricsQueueService = Mockito.mock(MetricsQueueService.class);
        SinkService sinkService = Mockito.mock(SinkService.class);
        OtelGrpcReceiver receiver = new OtelGrpcReceiver(
                1,1,1,
                logQueueService, traceQueueService, metricsQueueService,
                sinkService, 50058
        );
        receiver.start();
        receiver.stop();
    }
}
