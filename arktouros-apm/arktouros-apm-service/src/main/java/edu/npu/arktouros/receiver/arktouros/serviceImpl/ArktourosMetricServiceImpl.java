package edu.npu.arktouros.receiver.arktouros.serviceImpl;

import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.proto.collector.v1.MetricRequest;
import edu.npu.arktouros.proto.collector.v1.MetricResponse;
import edu.npu.arktouros.proto.collector.v1.MetricServiceGrpc;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : arktouros格式的otel metric数据处理器
 */
@Slf4j
public class ArktourosMetricServiceImpl extends MetricServiceGrpc.MetricServiceImplBase {

    private final SinkService sinkService;

    public ArktourosMetricServiceImpl(SinkService sinkService) {
        this.sinkService = sinkService;
    }

    @Override
    public void export(MetricRequest request, StreamObserver<MetricResponse> responseObserver) {
        log.debug("Received metric records: {}", request.getMetricsList().size());
        MetricResponse.Builder builder = MetricResponse.newBuilder();
        try {
            for (edu.npu.arktouros.proto.collector.v1.Metric metric : request.getMetricsList()) {
                if (metric.hasGauge()) {
                    edu.npu.arktouros.proto.metric.v1.Gauge gauge = metric.getGauge();
                    Gauge gaugeForSink = new Gauge(gauge);
                    sinkService.sink(gaugeForSink);
                } else if (metric.hasCounter()) {
                    edu.npu.arktouros.proto.metric.v1.Counter counter = metric.getCounter();
                    Counter counterForSink = new Counter(counter);
                    sinkService.sink(counterForSink);
                } else if (metric.hasHistogram()) {
                    edu.npu.arktouros.proto.metric.v1.Histogram histogram = metric.getHistogram();
                    Histogram histogramForSink = new Histogram(histogram);
                    sinkService.sink(histogramForSink);
                } else if (metric.hasSummary()) {
                    edu.npu.arktouros.proto.metric.v1.Summary summary = metric.getSummary();
                    Summary summaryForSink = new Summary(summary);
                    sinkService.sink(summaryForSink);
                }
            }
        } catch (IOException e) {
            builder.setRejectedMetricRecords(request.getMetricsList().size());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
