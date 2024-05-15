package edu.npu.arktouros.receiver.otel.serviceImpl;

import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
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
 * @description : {@link OtelMetricsServiceImpl}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class OtelMetricsServiceImplTest {

    private static MockedStatic<OtelMetricsAnalyzer> otelMetricsAnalyzer;

    @BeforeEach
    void setUp() {
        otelMetricsAnalyzer = Mockito.mockStatic(OtelMetricsAnalyzer.class);
    }

    @AfterEach
    void tearDown() {
        otelMetricsAnalyzer.close();
    }

    @Test
    void testExport() {
        ExportMetricsServiceRequest request = Mockito.mock(ExportMetricsServiceRequest.class);
        StreamObserver<ExportMetricsServiceResponse> responseObserver =
                Mockito.mock(StreamObserver.class);
        Mockito.when(request.getResourceMetricsList()).thenReturn(new ArrayList<>());
        OtelMetricsServiceImpl otelMetricsService = new OtelMetricsServiceImpl();
        Assertions.assertDoesNotThrow(() -> otelMetricsService.export(request, responseObserver));
    }

    @Test
    void testExportError() {
        ExportMetricsServiceRequest request = Mockito.mock(ExportMetricsServiceRequest.class);
        StreamObserver<ExportMetricsServiceResponse> responseObserver =
                Mockito.mock(StreamObserver.class);
        otelMetricsAnalyzer.when(() -> OtelMetricsAnalyzer.handle(any())).thenThrow(new RuntimeException());
        OtelMetricsServiceImpl otelMetricsService = new OtelMetricsServiceImpl();
        Assertions.assertDoesNotThrow(() -> otelMetricsService.export(request, responseObserver));
    }
}
