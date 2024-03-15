package edu.npu.arktouros.receiver.otelGrpc.service;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Log接收器
 */
public class OtelLogHandler extends LogsServiceGrpc.LogsServiceImplBase {
    @Override
    public void export(ExportLogsServiceRequest request,
                       StreamObserver<ExportLogsServiceResponse> responseObserver) {
        super.export(request, responseObserver);
    }
}
