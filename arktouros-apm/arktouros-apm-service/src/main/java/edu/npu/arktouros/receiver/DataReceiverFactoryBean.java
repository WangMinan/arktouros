package edu.npu.arktouros.receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.receiver.file.json.JsonFileReceiver;
import edu.npu.arktouros.receiver.grpc.arktouros.ArktourosGrpcReceiver;
import edu.npu.arktouros.receiver.grpc.otel.OtelGrpcReceiver;
import edu.npu.arktouros.receiver.tcp.arktouros.ArktourosTcpReceiver;
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

    @Value("${receiver.tcp.port}")
    private int tcpPort;

    @Value("${instance.number.analyzer.otel.log}")
    private int logAnalyzerNumber;

    @Value("${instance.number.analyzer.otel.trace}")
    private int traceAnalyzerNumber;

    @Value("${instance.number.analyzer.otel.metric}")
    private int metricsAnalyzerNumber;

    @Value("${receiver.file.json.logDir}")
    private String logDir;

    @Value("${receiver.file.json.indexFilePath}")
    private String indexFilePath;

    @Value("${receiver.file.json.type}")
    private String fileType;

    private final LogQueueService logQueueService;

    private final TraceQueueService traceQueueService;

    private final MetricsQueueService metricsQueueService;

    private final SinkService sinkService;

    private final ObjectMapper objectMapper;

    @Lazy
    public DataReceiverFactoryBean(LogQueueService logQueueService,
                                   TraceQueueService traceQueueService,
                                   MetricsQueueService metricsQueueService,
                                   SinkService sinkService,
                                   ObjectMapper objectMapper) {
        this.logQueueService = logQueueService;
        this.traceQueueService = traceQueueService;
        this.metricsQueueService = metricsQueueService;
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DataReceiver getObject() {
        switch (activeDataReceiver) {
            case "otelGrpc" -> {
                log.info("OtelGrpc receiver is active");
                return new OtelGrpcReceiver(logAnalyzerNumber, traceAnalyzerNumber, metricsAnalyzerNumber,
                        logQueueService, traceQueueService,
                        metricsQueueService, sinkService, grpcPort);
            }
            case "arktourosGrpc" -> {
                log.info("ArktourosGrpc receiver is active");
                return new ArktourosGrpcReceiver(sinkService, grpcPort);
            }
            case "arktourosTcp" -> {
                log.info("ArktourosTcp receiver is active");
                return new ArktourosTcpReceiver(sinkService, tcpPort, objectMapper);
            }
            case "jsonFile" -> {
                log.info("OtelFile receiver is active");
                return new JsonFileReceiver(logDir, indexFilePath, fileType, sinkService, objectMapper);
            }
            case null, default -> throw new IllegalArgumentException("can not find data receiver type from profile");
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
