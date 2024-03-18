package edu.npu.arktouros.receiver.otelGrpc.service;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Metrics接收器
 */
public class OtelMetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase{
    @Override
    public void export(ExportMetricsServiceRequest request,
                       StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        super.export(request, responseObserver);
    }
}
