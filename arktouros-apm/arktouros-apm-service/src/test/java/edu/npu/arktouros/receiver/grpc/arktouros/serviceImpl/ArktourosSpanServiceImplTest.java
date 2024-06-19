package edu.npu.arktouros.receiver.grpc.arktouros.serviceImpl;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.proto.collector.v1.SpanRequest;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : {@link ArktourosSpanServiceImpl}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
// 要加这个配置 不然对any的类型推断有很严格的限制
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class ArktourosSpanServiceImplTest {

    @Test
    void testExport() throws IOException {
        SinkService sinkService = Mockito.mock(SinkService.class);
        Mockito.doNothing().when(sinkService).sink(any(Source.class));
        ArktourosSpanServiceImpl arktourosSpanService = new ArktourosSpanServiceImpl(sinkService);
        Assertions.assertDoesNotThrow(() ->arktourosSpanService.export(SpanRequest.newBuilder().build(),
                Mockito.mock(StreamObserver.class)));
    }

    @Test
    void testExportError() throws IOException {
        SinkService sinkService = Mockito.mock(SinkService.class);
        Mockito.doThrow(new IOException()).when(sinkService).sink(any(Source.class));
        ArktourosSpanServiceImpl arktourosSpanService = new ArktourosSpanServiceImpl(sinkService);
        Assertions.assertDoesNotThrow(() ->arktourosSpanService.export(SpanRequest.newBuilder().build(),
                Mockito.mock(StreamObserver.class)));
    }
}
