package edu.npu.arktouros.receiver.grpc.otel.serviceImpl;

import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
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
 * @description : {@link OtelLogServiceImpl}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class OtelLogServiceImplTest {

    private static MockedStatic<OtelLogAnalyzer> otelLogAnalyzer;

    @BeforeEach
    void setUp() {
        otelLogAnalyzer = Mockito.mockStatic(OtelLogAnalyzer.class);
    }

    @AfterEach
    void tearDown() {
        otelLogAnalyzer.close();
    }

    @Test
    void testExport() {
        ExportLogsServiceRequest request = Mockito.mock(ExportLogsServiceRequest.class);
        StreamObserver<ExportLogsServiceResponse> responseObserver =
                Mockito.mock(StreamObserver.class);
        Mockito.when(request.getResourceLogsList()).thenReturn(new ArrayList<>());
        OtelLogServiceImpl otelLogService = new OtelLogServiceImpl();
        Assertions.assertDoesNotThrow(() -> otelLogService.export(request, responseObserver));
    }

    @Test
    void testExportException() {
        ExportLogsServiceRequest request = Mockito.mock(ExportLogsServiceRequest.class);
        StreamObserver<ExportLogsServiceResponse> responseObserver =
                Mockito.mock(StreamObserver.class);
        otelLogAnalyzer.when(() -> OtelLogAnalyzer.handle(any(ResourceLogs.class)))
                .thenThrow(new RuntimeException());
        OtelLogServiceImpl otelLogService = new OtelLogServiceImpl();
        Assertions.assertDoesNotThrow(() -> otelLogService.export(request, responseObserver));
    }
}
