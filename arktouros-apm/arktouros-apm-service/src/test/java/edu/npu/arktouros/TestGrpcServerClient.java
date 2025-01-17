package edu.npu.arktouros;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.sinker.SinkService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

/**
 * @description  : [一句话描述该类的功能]
 * @author       : [wangminan]
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class TestGrpcServerClient {

    @Resource
    private SinkService sinkService;

    @Resource
    private ObjectMapper objectMapper;

    @Test
    void testGrpcSpanReview() throws IOException {
        PropertiesProvider.init();
        sinkService.init();
        if (!sinkService.isReady()) {
            log.error("APM sink service is not ready, shutting down.");
            return;
        }
        Span grpc_client = Span.builder()
                .id("3970683974090625025")
                .traceId("3970683974090625024")
                .name("grpc_client_span_1")
                .serviceName("grpc_client")
                .startTime(946684831164L)
                .endTime(946684831253L)
                .build();
        Span grpc_server = Span.builder()
                .id("3970684006516789248")
                .parentSpanId("3970683974090625025")
                .traceId("3970683974090625024")
                .name("grpc_server_span_1")
                .serviceName("grpc_server")
                .startTime(946684838895L)
                .endTime(946684838895L)
                .build();
        sinkService.sink(grpc_client);
        sinkService.sink(grpc_server);
    }
}
