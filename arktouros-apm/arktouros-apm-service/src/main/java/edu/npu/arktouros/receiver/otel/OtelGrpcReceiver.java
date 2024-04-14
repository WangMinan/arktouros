package edu.npu.arktouros.receiver.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelTraceAnalyzer;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.otel.serviceImpl.OtelLogServiceImpl;
import edu.npu.arktouros.receiver.otel.serviceImpl.OtelMetricsServiceImpl;
import edu.npu.arktouros.receiver.otel.serviceImpl.OtelTraceServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : 通过GRPC协议接收数据
 */
@Slf4j
public class OtelGrpcReceiver extends DataReceiver {

    // 这个位置未来要解耦 不用框架来管理analyzer实例 我们要有多个analyzer并发做处理
    private final OtelLogAnalyzer logAnalyzer;

    private final OtelMetricsAnalyzer metricsAnalyzer;

    private final OtelTraceAnalyzer traceAnalyzer;

    private final int port;

    private final Server server;

    public OtelGrpcReceiver(int grpcPort, OtelLogAnalyzer logAnalyzer,
                            OtelMetricsAnalyzer metricsAnalyzer, OtelTraceAnalyzer traceAnalyzer) {
        this.port = grpcPort;
        this.logAnalyzer = logAnalyzer;
        this.metricsAnalyzer = metricsAnalyzer;
        this.traceAnalyzer = traceAnalyzer;
        List<DataAnalyzer> analyzers = List.of(this.logAnalyzer,
                this.metricsAnalyzer, this.traceAnalyzer);
        analyzers.forEach(DataAnalyzer::init);
        server = ServerBuilder.forPort(grpcPort)
                .addService(new OtelMetricsServiceImpl(metricsAnalyzer))
                .addService(new OtelLogServiceImpl(logAnalyzer))
                .addService(new OtelTraceServiceImpl(traceAnalyzer))
                .build();
    }

    @Override
    public void start() {
        try {
            server.start();
            log.info("OtelGrpcReceiver start to receive data, listening on port:{}", port);
        } catch (IOException e) {
            log.error("Grpc receiver start error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                log.info("Grpc server is shutting down.");
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                logAnalyzer.interrupt();
                metricsAnalyzer.interrupt();
                traceAnalyzer.interrupt();
                log.info("Grpc receiver stopped");
            } catch (InterruptedException e) {
                log.error("Grpc receiver failed to shutdown.");
                throw new RuntimeException(e);
            }
        }
    }
}
