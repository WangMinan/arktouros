package edu.npu.arktouros.receiver.otel.service;

import edu.npu.arktouros.analyzer.otel.OtelTraceAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Trace接收器
 */
public class OtelTraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

    private final OtelTraceAnalyzer analyzer;

    public OtelTraceServiceImpl() {
        analyzer = OtelTraceAnalyzer.getInstance();
        analyzer.start();
    }

    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {
        request.getResourceSpansList().forEach(resourceSpans -> {
            analyzer.handle(resourceSpans);
            responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance());
            responseObserver.onCompleted();
        });
    }
}
