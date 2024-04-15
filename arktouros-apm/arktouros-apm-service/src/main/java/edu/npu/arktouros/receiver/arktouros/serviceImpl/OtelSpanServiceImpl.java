package edu.npu.arktouros.receiver.arktouros.serviceImpl;

import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.proto.collector.v1.SpanRequest;
import edu.npu.arktouros.proto.collector.v1.SpanResponse;
import edu.npu.arktouros.proto.collector.v1.SpanServiceGrpc;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.stub.StreamObserver;

/**
 * @author : [wangminan]
 * @description : arktouros格式的otel span数据处理器
 */
public class OtelSpanServiceImpl extends SpanServiceGrpc.SpanServiceImplBase {

    private final SinkService sinkService;

    public OtelSpanServiceImpl(SinkService sinkService) {
        this.sinkService = sinkService;
    }

    @Override
    public void export(SpanRequest request, StreamObserver<SpanResponse> responseObserver) {
        SpanResponse.Builder builder = SpanResponse.newBuilder();
        try {
            for (edu.npu.arktouros.proto.span.v1.Span span : request.getSpansList()) {
                Span spanForSink =
                        new Span(span);
                sinkService.sink(spanForSink);
            }
        } catch (Exception e) {
            builder.setRejectedSpanRecords(request.getSpansList().size());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
