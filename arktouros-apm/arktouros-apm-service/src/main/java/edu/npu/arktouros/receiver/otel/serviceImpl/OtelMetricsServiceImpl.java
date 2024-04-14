package edu.npu.arktouros.receiver.otel.serviceImpl;

import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsPartialSuccess;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @author : [wangminan]
 * @description : OpenTelemetry-Metrics接收器
 */
@Slf4j
public class OtelMetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase {


    public OtelMetricsServiceImpl() {
    }


    @Override
    public void export(ExportMetricsServiceRequest request,
                       StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        ExportMetricsServiceResponse.Builder responseBuilder =
                ExportMetricsServiceResponse.newBuilder();
        try {
            request.getResourceMetricsList().forEach(OtelMetricsAnalyzer::handle);
        } catch (Exception e) {
            responseBuilder.setPartialSuccess(ExportMetricsPartialSuccess.newBuilder()
                    .setRejectedDataPoints(request.getResourceMetricsList().size())
                    .setErrorMessage(Arrays.toString(e.getStackTrace()))
            );
        }
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
