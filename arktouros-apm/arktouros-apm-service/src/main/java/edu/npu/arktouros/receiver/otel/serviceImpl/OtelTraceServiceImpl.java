package edu.npu.arktouros.receiver.otel.serviceImpl;

import edu.npu.arktouros.analyzer.otel.OtelTraceAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Trace接收器
 */
@Slf4j
public class OtelTraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

    private final OtelTraceAnalyzer analyzer;

    public OtelTraceServiceImpl(OtelTraceAnalyzer analyzer) {
        this.analyzer = analyzer;
        analyzer.start();
    }

    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {
        request.getResourceSpansList().forEach(analyzer::handle);
        responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
