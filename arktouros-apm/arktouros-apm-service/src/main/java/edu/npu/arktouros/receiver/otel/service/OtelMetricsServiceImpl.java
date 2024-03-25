package edu.npu.arktouros.receiver.otel.service;

import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Metrics接收器
 */
@Slf4j
public class OtelMetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase{

    private final OtelMetricsAnalyzer analyzer;

    public OtelMetricsServiceImpl(OtelMetricsAnalyzer analyzer) {
        this.analyzer = analyzer;
        analyzer.start();
    }


    @Override
    public void export(ExportMetricsServiceRequest request,
                       StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        request.getResourceMetricsList().forEach(resourceMetrics -> {
            analyzer.handle(resourceMetrics);
            responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
            responseObserver.onCompleted();
        });
    }
}
