package edu.npu.arktouros.receiver.otel.serviceImpl;

import edu.npu.arktouros.analyzer.otel.OtelTraceAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : {@link OtelTraceServiceImpl}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class OtelTraceServiceImplTest {

    private static MockedStatic<OtelTraceAnalyzer> otelTraceAnalyzer;

    @BeforeEach
    void setUp() {
        otelTraceAnalyzer = Mockito.mockStatic(OtelTraceAnalyzer.class);
    }

    @AfterEach
    void tearDown() {
        otelTraceAnalyzer.close();
    }

    @Test
    void testExport() {
        ExportTraceServiceRequest request = Mockito.mock(ExportTraceServiceRequest.class);
        StreamObserver<ExportTraceServiceResponse> responseObserver =
                Mockito.mock(StreamObserver.class);
        Mockito.when(request.getResourceSpansList()).thenReturn(new ArrayList<>());
        OtelTraceServiceImpl otelTraceService = new OtelTraceServiceImpl();
        Assertions.assertDoesNotThrow(() -> otelTraceService.export(request, responseObserver));
    }

    @Test
    void testExportError() {
        ExportTraceServiceRequest request = Mockito.mock(ExportTraceServiceRequest.class);
        StreamObserver<ExportTraceServiceResponse> responseObserver =
                Mockito.mock(StreamObserver.class);
        otelTraceAnalyzer.when(() -> OtelTraceAnalyzer.handle(any()))
                .thenThrow(new RuntimeException());
        OtelTraceServiceImpl otelTraceService = new OtelTraceServiceImpl();
        Assertions.assertDoesNotThrow(() -> otelTraceService.export(request, responseObserver));
    }
}
