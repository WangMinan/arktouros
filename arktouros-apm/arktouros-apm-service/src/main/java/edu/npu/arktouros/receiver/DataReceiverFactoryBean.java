package edu.npu.arktouros.receiver;

import edu.npu.arktouros.receiver.arktouros.ArktourosReceiver;
import edu.npu.arktouros.receiver.otel.OtelGrpcReceiver;
import edu.npu.arktouros.service.otel.queue.LogQueueService;
import edu.npu.arktouros.service.otel.queue.MetricsQueueService;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author : [wangminan]
 * @description : 数据接收器工厂
 */
@Component
@DependsOn("dataSource")
@Slf4j
public class DataReceiverFactoryBean implements FactoryBean<DataReceiver> {

    @Value("${instance.active.dataReceiver}")
    private String activeDataReceiver;

    @Value("${receiver.grpc.port}")
    private int grpcPort;

    @Value("${instance.number.analyzer.otel.log}")
    private int logAnalyzerNumber;

    @Value("${instance.number.analyzer.otel.trace}")
    private int traceAnalyzerNumber;

    @Value("${instance.number.analyzer.otel.metric}")
    private int metricsAnalyzerNumber;

    private final LogQueueService logQueueService;

    private final TraceQueueService traceQueueService;

    private final MetricsQueueService metricsQueueService;

    private final SinkService sinkService;

    @Lazy
    public DataReceiverFactoryBean(LogQueueService logQueueService,
                                   TraceQueueService traceQueueService,
                                   MetricsQueueService metricsQueueService,
                                   SinkService sinkService) {
        this.logQueueService = logQueueService;
        this.traceQueueService = traceQueueService;
        this.metricsQueueService = metricsQueueService;
        this.sinkService = sinkService;
    }

    @Override
    public DataReceiver getObject() {
        if (activeDataReceiver.equals("otelGrpc")) {
            log.info("OtelGrpc receiver is active");
            return new OtelGrpcReceiver(logAnalyzerNumber, traceAnalyzerNumber, metricsAnalyzerNumber,
                    logQueueService, traceQueueService,
                    metricsQueueService, sinkService, grpcPort);
        } else if (activeDataReceiver.equals("arktourosGrpc")) {
            log.info("ArktourosGrpc receiver is active");
            return new ArktourosReceiver(sinkService, grpcPort);
        } else {
            throw new IllegalArgumentException("can not find data receiver type from profile");
        }
    }

    @Override
    public Class<?> getObjectType() {
        return DataReceiver.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
