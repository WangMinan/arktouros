package edu.npu.arktouros.receiver.otelGrpc.service;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Trace接收器
 */
public class OtelTraceHandler extends TraceServiceGrpc.TraceServiceImplBase {

    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {
        super.export(request, responseObserver);
    }
}
