package edu.npu.arktouros.emitter.grpc.arktouros;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.emitter.AbstractEmitter;
import edu.npu.arktouros.emitter.EmitterFactory;
import edu.npu.arktouros.emitter.grpc.AbstractGrpcEmitter;
import edu.npu.arktouros.proto.collector.v1.LogRequest;
import edu.npu.arktouros.proto.collector.v1.LogResponse;
import edu.npu.arktouros.proto.collector.v1.LogServiceGrpc;
import edu.npu.arktouros.proto.collector.v1.Metric;
import edu.npu.arktouros.proto.collector.v1.MetricRequest;
import edu.npu.arktouros.proto.collector.v1.MetricResponse;
import edu.npu.arktouros.proto.collector.v1.MetricServiceGrpc;
import edu.npu.arktouros.proto.collector.v1.SpanRequest;
import edu.npu.arktouros.proto.collector.v1.SpanResponse;
import edu.npu.arktouros.proto.collector.v1.SpanServiceGrpc;
import edu.npu.arktouros.proto.log.v1.Log;
import edu.npu.arktouros.proto.span.v1.Span;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : ArktourosGrpcEmitter
 */
@Slf4j
public class ArktourosGrpcEmitter extends AbstractGrpcEmitter {

    protected LogServiceGrpc.LogServiceBlockingStub logServiceBlockingStub;
    protected SpanServiceGrpc.SpanServiceBlockingStub spanServiceBlockingStub;
    protected MetricServiceGrpc.MetricServiceBlockingStub metricServiceBlockingStub;

    public ArktourosGrpcEmitter(AbstractCache inputCache) {
        super(inputCache);
        logServiceBlockingStub = LogServiceGrpc.newBlockingStub(channel);
        spanServiceBlockingStub = SpanServiceGrpc.newBlockingStub(channel);
        metricServiceBlockingStub = MetricServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void run() {
        // 还好我们预留了一个type
        while (true) {
            String inputJson = inputCache.get().trim();
            // 准备三个Builder
            Log.Builder logBuilder = Log.newBuilder();
            Span.Builder spanBuilder = Span.newBuilder();
            Metric.Builder metricBuilder = Metric.newBuilder();
            // 序列化
            try {
                ProtoBufJsonUtils.fromJSON(inputJson, logBuilder);
                emitLog(logBuilder.build());
            } catch (IOException e) {
                try {
                    ProtoBufJsonUtils.fromJSON(inputJson, spanBuilder);
                    emitSpan(spanBuilder.build());
                } catch (IOException ex) {
                    try {
                        ProtoBufJsonUtils.fromJSON(inputJson, metricBuilder);
                        emitMetric(metricBuilder.build());
                    } catch (IOException exc) {
                        log.error("Failed to parse json string: {}", inputJson);
                    }
                }
            }
        }
    }

    private void emitMetric(Metric metric) {
        log.info("Sending arktouros metric data to apm");
        MetricRequest request = MetricRequest.newBuilder().addMetrics(metric).build();
        try {
            MetricResponse response = metricServiceBlockingStub.export(request);
            if (response.getRejectedMetricRecords() > 0) {
                log.error("Rejected {} metric records", response.getRejectedMetricRecords());
            }
        } catch (StatusRuntimeException e) {
            log.error("Failed to send arktouros metric data to apm, error message: {}.",
                    e.getMessage()
            );
        }
    }

    private void emitSpan(Span span) {
        log.info("Sending arktouros span data to apm");
        SpanRequest request = SpanRequest.newBuilder().addSpans(span).build();
        try {
            SpanResponse response = spanServiceBlockingStub.export(request);
            if (response.getRejectedSpanRecords() > 0) {
                log.error("Rejected {} span records", response.getRejectedSpanRecords());
            }
        } catch (StatusRuntimeException e) {
            log.error("Failed to send arktouros span data to apm, error message: {}.",
                    e.getMessage()
            );
        }
    }

    private void emitLog(Log emitLog) {
        log.info("Sending arktouros logs data to apm");
        LogRequest request = LogRequest.newBuilder().addLogs(emitLog).build();
        try {
            LogResponse response = logServiceBlockingStub.export(request);
            if (response.getRejectedLogRecords() > 0) {
                log.error("Rejected {} log records", response.getRejectedLogRecords());
            }
        } catch (StatusRuntimeException e) {
            log.error("Failed to send arktouros logs data to apm, error message: {}.",
                    e.getMessage()
            );
        }
    }

    public static class Factory implements EmitterFactory {

        @Override
        public AbstractEmitter createEmitter(AbstractCache inputCache) {
            return new ArktourosGrpcEmitter(inputCache);
        }
    }
}
