package edu.npu.arktouros.receiver.grpc.arktouros.serviceImpl;

import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.proto.collector.v1.LogRequest;
import edu.npu.arktouros.proto.collector.v1.LogResponse;
import edu.npu.arktouros.proto.collector.v1.LogServiceGrpc;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : arktouros格式的otel service数据处理器
 */
@Slf4j
public class ArktourosLogServiceImpl extends LogServiceGrpc.LogServiceImplBase {
    private final SinkService sinkService;

    public ArktourosLogServiceImpl(SinkService sinkService) {
        this.sinkService = sinkService;
    }

    @Override
    public void export(LogRequest request, StreamObserver<LogResponse> responseObserver) {
        LogResponse.Builder builder = LogResponse.newBuilder();
        try {
            for (edu.npu.arktouros.proto.log.v1.Log log : request.getLogsList()) {
                Log logForSink =
                        new Log(log);
                sinkService.sink(logForSink);
            }
        } catch (Exception e) {
            builder.setRejectedLogRecords(request.getLogsList().size());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
