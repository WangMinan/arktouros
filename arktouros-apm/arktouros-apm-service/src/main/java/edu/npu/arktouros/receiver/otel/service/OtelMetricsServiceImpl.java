package edu.npu.arktouros.receiver.otel.service;

import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Metrics接收器
 */
public class OtelMetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase{

    private final OtelMetricsAnalyzer analyzer;

    public OtelMetricsServiceImpl() {
        analyzer = OtelMetricsAnalyzer.getInstance();
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
