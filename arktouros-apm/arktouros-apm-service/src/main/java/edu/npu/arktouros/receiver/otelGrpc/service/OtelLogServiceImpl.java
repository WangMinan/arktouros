package edu.npu.arktouros.receiver.otelGrpc.service;

import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Log接收器
 */
@Slf4j
public class OtelLogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {

    private final OtelLogAnalyzer analyzer;

    public OtelLogServiceImpl() {
        analyzer = OtelLogAnalyzer.getInstance();
        analyzer.start();
    }

    @Override
    public void export(ExportLogsServiceRequest request,
                       StreamObserver<ExportLogsServiceResponse> responseObserver) {
        request.getResourceLogsList().forEach(resourceLogs -> {
            analyzer.handle(resourceLogs);
            responseObserver.onNext(ExportLogsServiceResponse.getDefaultInstance());
            responseObserver.onCompleted();
        });
    }
}
