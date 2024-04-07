package edu.npu.arktouros.receiver.otel.serviceImpl;

import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Log接收器
 */
@Slf4j
public class OtelLogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {

    private final OtelLogAnalyzer analyzer;

    public OtelLogServiceImpl(OtelLogAnalyzer analyzer) {
        this.analyzer = analyzer;
        analyzer.start();
    }

    @Override
    public void export(ExportLogsServiceRequest request,
                       StreamObserver<ExportLogsServiceResponse> responseObserver) {
        ExportLogsServiceResponse.Builder responseBuilder =
                ExportLogsServiceResponse.newBuilder();
        try {
            // 只能定位到analyzer接收时的问题，之后就跟踪不到了
            request.getResourceLogsList().forEach(analyzer::handle);
        } catch (Exception e) {
            responseBuilder.setPartialSuccess(
              ExportLogsPartialSuccess.newBuilder()
                      .setRejectedLogRecords(request.getResourceLogsList().size())
                      .setErrorMessage(Arrays.toString(e.getStackTrace()))
            );
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
